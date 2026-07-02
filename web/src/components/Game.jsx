import { useEffect, useRef, useState } from "react";
import Board from "./Board.jsx";
import { getValidMoves } from "../api.js";
import { connectToGame } from "../ws.js";

const PROMOTION_PIECES = ["QUEEN", "ROOK", "BISHOP", "KNIGHT"];

export default function Game({ auth, game, onExit }) {
  const [chessGame, setChessGame] = useState(null);
  const [gameOver, setGameOver] = useState(false);
  const [log, setLog] = useState([]);
  const [selected, setSelected] = useState(null);
  const [legalMoves, setLegalMoves] = useState([]);
  // when a pawn move needs a promotion choice: { moves: [...] }
  const [promotion, setPromotion] = useState(null);
  const socketRef = useRef(null);

  const isPlayer = game.color === "WHITE" || game.color === "BLACK";

  useEffect(() => {
    const socket = connectToGame({
      authToken: auth.authToken,
      gameID: game.gameID,
      username: auth.username,
      color: game.color,
      onMessage: (message) => {
        switch (message.serverMessageType) {
          case "LOAD_GAME":
            setChessGame(message.game);
            setGameOver(Boolean(message.gameOver));
            setSelected(null);
            setLegalMoves([]);
            setPromotion(null);
            break;
          case "NOTIFICATION":
            setLog((old) => [...old, { kind: "note", text: message.message }]);
            break;
          case "ERROR":
            setLog((old) => [...old, { kind: "error", text: message.errorMessage }]);
            break;
          default:
            break;
        }
      },
      onClose: () =>
        setLog((old) => [...old, { kind: "error", text: "Connection lost" }]),
    });
    socketRef.current = socket;
    return () => socket.close();
  }, [auth.authToken, auth.username, game.gameID, game.color]);

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

  const targets = new Set(
    legalMoves.map((m) => `${m.endPosition.row},${m.endPosition.col}`),
  );

  const turn = chessGame?.teamsTurn;
  const orientation = game.color === "BLACK" ? "BLACK" : "WHITE";

  let status;
  if (!chessGame) {
    status = "Connecting…";
  } else if (gameOver) {
    status = "Game over";
  } else if (!isPlayer) {
    status = `${turn === "WHITE" ? "White" : "Black"} to move`;
  } else if (turn === game.color) {
    status = "Your move";
  } else {
    status = "Waiting for opponent…";
  }

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
          <p className={`status ${gameOver ? "over" : ""}`}>{status}</p>
          {chessGame ? (
            <Board
              board={chessGame.board?.board}
              orientation={orientation}
              selected={selected}
              targets={targets}
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

        <aside className="log-panel">
          <h2>Game log</h2>
          <ul>
            {log.map((entry, i) => (
              <li key={i} className={entry.kind}>
                {entry.text}
              </li>
            ))}
          </ul>
        </aside>
      </div>
    </div>
  );
}
