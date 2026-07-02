import { useEffect, useRef, useState } from "react";
import Board from "./Board.jsx";
import { downloadPgn, getValidMoves } from "../api.js";
import { connectToGame } from "../ws.js";

const PROMOTION_PIECES = ["QUEEN", "ROOK", "BISHOP", "KNIGHT"];

const RESULT_LABELS = {
  "1-0": "White wins",
  "0-1": "Black wins",
  "1/2-1/2": "Draw",
};

function formatClock(ms) {
  const clamped = Math.max(0, ms);
  const totalSeconds = Math.floor(clamped / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

function Clock({ label, remainingMs, active, low }) {
  return (
    <div className={`clock ${active ? "active" : ""} ${low ? "low" : ""}`}>
      <span className="clock-label">{label}</span>
      <span className="clock-time">{formatClock(remainingMs)}</span>
    </div>
  );
}

export default function Game({ auth, game, onExit }) {
  const [chessGame, setChessGame] = useState(null);
  const [gameOver, setGameOver] = useState(false);
  const [result, setResult] = useState(null);
  const [moveHistory, setMoveHistory] = useState([]);
  const [lastMove, setLastMove] = useState(null);
  // clock snapshot from the last LOAD_GAME plus when we received it
  const [clockInfo, setClockInfo] = useState(null);
  const [log, setLog] = useState([]);
  const [selected, setSelected] = useState(null);
  const [legalMoves, setLegalMoves] = useState([]);
  const [promotion, setPromotion] = useState(null);
  const [connStatus, setConnStatus] = useState("connecting");
  const [, setTick] = useState(0);
  const socketRef = useRef(null);
  const logEndRef = useRef(null);

  const isPlayer = game.color === "WHITE" || game.color === "BLACK";

  useEffect(() => {
    const socket = connectToGame({
      authToken: auth.authToken,
      gameID: game.gameID,
      username: auth.username,
      color: game.color,
      onStatus: setConnStatus,
      onMessage: (message) => {
        switch (message.serverMessageType) {
          case "LOAD_GAME":
            setChessGame(message.game);
            setGameOver(Boolean(message.gameOver));
            setResult(message.result ?? null);
            setMoveHistory(message.moveHistory ?? []);
            setLastMove(message.lastMove ?? null);
            if (message.whiteTimeMs != null) {
              setClockInfo({
                whiteTimeMs: message.whiteTimeMs,
                blackTimeMs: message.blackTimeMs,
                turnStartedAt: message.turnStartedAt,
                serverTime: message.serverTime,
                receivedAt: Date.now(),
              });
            } else {
              setClockInfo(null);
            }
            setSelected(null);
            setLegalMoves([]);
            setPromotion(null);
            break;
          case "NOTIFICATION":
            setLog((old) => [...old, { kind: "note", text: message.message }]);
            // resignation ends the game without a board update from the server
            if (message.message?.includes("has resigned")) {
              setGameOver(true);
              setSelected(null);
              setLegalMoves([]);
              setPromotion(null);
            }
            break;
          case "ERROR":
            setLog((old) => [...old, { kind: "error", text: message.errorMessage }]);
            break;
          default:
            break;
        }
      },
    });
    socketRef.current = socket;
    return () => socket.close();
  }, [auth.authToken, auth.username, game.gameID, game.color]);

  // keep the ticking clock display fresh
  useEffect(() => {
    if (!clockInfo || gameOver) return undefined;
    const interval = setInterval(() => setTick((t) => t + 1), 250);
    return () => clearInterval(interval);
  }, [clockInfo, gameOver]);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ block: "nearest" });
  }, [log]);

  const sendMove = (move) => {
    socketRef.current?.makeMove({
      startPosition: move.startPosition,
      endPosition: move.endPosition,
      promotionPiece: move.promotionPiece ?? null,
    });
    setSelected(null);
    setLegalMoves([]);
    setPromotion(null);
  };

  const handleSquareClick = async (row, col) => {
    if (!isPlayer || gameOver || !chessGame || promotion) return;

    const movesToSquare = legalMoves.filter(
      (m) => m.endPosition.row === row && m.endPosition.col === col,
    );
    if (selected && movesToSquare.length > 0) {
      if (movesToSquare.some((m) => m.promotionPiece)) {
        setPromotion({ moves: movesToSquare });
      } else {
        sendMove(movesToSquare[0]);
      }
      return;
    }

    const piece = chessGame.board?.board?.[row - 1]?.[col - 1];
    if (piece && piece.pieceColor === game.color) {
      try {
        const result = await getValidMoves(auth.authToken, game.gameID, row, col);
        setSelected({ row, col });
        setLegalMoves(result.moves || []);
      } catch {
        setSelected(null);
        setLegalMoves([]);
      }
    } else {
      setSelected(null);
      setLegalMoves([]);
    }
  };

  const choosePromotion = (pieceType) => {
    const move = promotion.moves.find((m) => m.promotionPiece === pieceType);
    if (move) sendMove(move);
  };

  const handleResign = () => {
    if (window.confirm("Resign this game?")) {
      socketRef.current?.resign();
    }
  };

  const handleLeave = () => {
    socketRef.current?.leave();
    onExit();
  };

  const handlePgn = async () => {
    try {
      await downloadPgn(auth.authToken, game.gameID);
    } catch (e) {
      setLog((old) => [...old, { kind: "error", text: e.message }]);
    }
  };

  const targets = new Set(
    legalMoves.map((m) => `${m.endPosition.row},${m.endPosition.col}`),
  );

  const turn = chessGame?.teamsTurn;
  const orientation = game.color === "BLACK" ? "BLACK" : "WHITE";

  const remaining = (color) => {
    if (!clockInfo) return null;
    const base = color === "WHITE" ? clockInfo.whiteTimeMs : clockInfo.blackTimeMs;
    if (gameOver || clockInfo.turnStartedAt == null || turn !== color) return base;
    const elapsedAtServer = clockInfo.serverTime - clockInfo.turnStartedAt;
    const elapsedSince = Date.now() - clockInfo.receivedAt;
    return base - elapsedAtServer - elapsedSince;
  };

  let status;
  if (!chessGame) {
    status = "Connecting…";
  } else if (gameOver) {
    status = RESULT_LABELS[result] ? `Game over — ${RESULT_LABELS[result]}` : "Game over";
  } else if (!isPlayer) {
    status = `${turn === "WHITE" ? "White" : "Black"} to move`;
  } else if (turn === game.color) {
    status = "Your move";
  } else {
    status = "Waiting for opponent…";
  }

  const movePairs = [];
  for (let i = 0; i < moveHistory.length; i += 2) {
    movePairs.push({
      number: i / 2 + 1,
      white: moveHistory[i],
      black: moveHistory[i + 1],
    });
  }

  const topColor = orientation === "WHITE" ? "BLACK" : "WHITE";
  const bottomColor = orientation;

  return (
    <div className="game-page">
      <header className="topbar">
        <h1>
          {game.gameName}
          <span className="role">
            {isPlayer ? ` — playing ${game.color.toLowerCase()}` : " — observing"}
          </span>
        </h1>
        <div>
          {connStatus !== "connected" && (
            <span className={`conn-status ${connStatus}`}>{connStatus}</span>
          )}
          <button className="secondary" onClick={handlePgn}>
            Download PGN
          </button>
          {isPlayer && !gameOver && (
            <button className="danger" onClick={handleResign}>
              Resign
            </button>
          )}
          <button onClick={handleLeave}>Back to lobby</button>
        </div>
      </header>

      <div className="game-layout">
        <div className="board-panel">
          <div className="status-row">
            <p className={`status ${gameOver ? "over" : ""}`}>{status}</p>
            {clockInfo && (
              <div className="clocks">
                <Clock
                  label={topColor === "WHITE" ? "White" : "Black"}
                  remainingMs={remaining(topColor)}
                  active={!gameOver && turn === topColor}
                  low={remaining(topColor) < 30000}
                />
                <Clock
                  label={bottomColor === "WHITE" ? "White" : "Black"}
                  remainingMs={remaining(bottomColor)}
                  active={!gameOver && turn === bottomColor}
                  low={remaining(bottomColor) < 30000}
                />
              </div>
            )}
          </div>
          {chessGame ? (
            <Board
              board={chessGame.board?.board}
              orientation={orientation}
              selected={selected}
              targets={targets}
              lastMove={lastMove}
              myColor={isPlayer && !gameOver ? game.color : null}
              onSquareClick={handleSquareClick}
            />
          ) : (
            <div className="board-placeholder">Loading board…</div>
          )}
          {promotion && (
            <div className="promotion-picker">
              <span>Promote to:</span>
              {PROMOTION_PIECES.map((pieceType) => (
                <button key={pieceType} onClick={() => choosePromotion(pieceType)}>
                  {pieceType.toLowerCase()}
                </button>
              ))}
            </div>
          )}
        </div>

        <aside className="side-panels">
          <div className="moves-panel">
            <h2>Moves</h2>
            {movePairs.length === 0 && <p className="muted">No moves yet</p>}
            <ol className="move-list">
              {movePairs.map((pair) => (
                <li key={pair.number}>
                  <span className="move-number">{pair.number}.</span>
                  <span className="move">{pair.white}</span>
                  <span className="move">{pair.black ?? ""}</span>
                </li>
              ))}
            </ol>
          </div>

          <div className="log-panel">
            <h2>Game log</h2>
            <ul>
              {log.map((entry, i) => (
                <li key={i} className={entry.kind}>
                  {entry.text}
                </li>
              ))}
              <div ref={logEndRef} />
            </ul>
          </div>
        </aside>
      </div>
    </div>
  );
}
