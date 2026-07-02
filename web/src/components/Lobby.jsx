import { useCallback, useEffect, useState } from "react";
import { addAiOpponent, createGame, joinGame, listGames } from "../api.js";

const AI_LABELS = { ai: "AI (alpha-beta)", ml: "AI (neural net)" };
const DIFFICULTIES = [
  { value: 1, label: "Easy" },
  { value: 2, label: "Medium" },
  { value: 3, label: "Hard" },
];
const TIME_CONTROLS = [
  { value: "", label: "No clock" },
  { value: "5", label: "5 minutes" },
  { value: "10", label: "10 minutes" },
  { value: "30", label: "30 minutes" },
];

function playerLabel(username) {
  if (!username) return null;
  return AI_LABELS[username] || username;
}

function isAiName(username) {
  return username === "ai" || username === "ml";
}

export default function Lobby({ auth, onEnterGame, onLogout }) {
  const [games, setGames] = useState([]);
  const [error, setError] = useState(null);
  const [showFinished, setShowFinished] = useState(false);

  const [gameName, setGameName] = useState("");
  const [opponent, setOpponent] = useState("open");
  const [myColor, setMyColor] = useState("WHITE");
  const [difficulty, setDifficulty] = useState(3);
  const [timeControl, setTimeControl] = useState("");
  const [whiteAi, setWhiteAi] = useState("ai");
  const [blackAi, setBlackAi] = useState("ml");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const result = await listGames(auth.authToken);
      setGames(result.games || []);
    } catch (e) {
      setError(e.message);
    }
  }, [auth.authToken]);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 3000);
    return () => clearInterval(interval);
  }, [refresh]);

  const vsAi = opponent === "ai" || opponent === "ml";
  const aiShowdown = opponent === "aivsai";

  const handleCreate = async (event) => {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const minutes = timeControl ? Number(timeControl) : null;
      const { gameID } = await createGame(auth.authToken, gameName.trim(), minutes);

      if (aiShowdown) {
        await addAiOpponent(auth.authToken, gameID, "WHITE", whiteAi, difficulty);
        await addAiOpponent(auth.authToken, gameID, "BLACK", blackAi, difficulty);
        onEnterGame({ gameID, gameName: gameName.trim(), color: null });
      } else {
        await joinGame(auth.authToken, gameID, myColor);
        if (vsAi) {
          const aiColor = myColor === "WHITE" ? "BLACK" : "WHITE";
          await addAiOpponent(auth.authToken, gameID, aiColor, opponent, difficulty);
        }
        onEnterGame({ gameID, gameName: gameName.trim(), color: myColor });
      }
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  const handleJoin = async (game, color) => {
    setError(null);
    try {
      // rejoining a game you're already in shouldn't hit the join endpoint
      const seat = color === "WHITE" ? game.whiteUsername : game.blackUsername;
      if (seat !== auth.username) {
        await joinGame(auth.authToken, game.gameID, color);
      }
      onEnterGame({ gameID: game.gameID, gameName: game.gameName, color });
    } catch (e) {
      setError(e.message);
    }
  };

  const handleObserve = (game) => {
    onEnterGame({ gameID: game.gameID, gameName: game.gameName, color: null });
  };

  const visibleGames = games.filter((g) => showFinished || !g.gameOver);

  return (
    <div className="lobby-page">
      <header className="topbar">
        <h1>♔ Chess</h1>
        <div>
          <span className="me">{auth.username}</span>
          <button onClick={onLogout}>Log out</button>
        </div>
      </header>

      <section className="create-card">
        <h2>New game</h2>
        <form onSubmit={handleCreate}>
          <input
            placeholder="Game name"
            value={gameName}
            onChange={(e) => setGameName(e.target.value)}
          />
          <label>
            Opponent
            <select value={opponent} onChange={(e) => setOpponent(e.target.value)}>
              <option value="open">Open seat (another player)</option>
              <option value="ai">AI — alpha-beta search</option>
              <option value="ml">AI — neural net</option>
              <option value="aivsai">AI vs AI (watch them fight)</option>
            </select>
          </label>
          {aiShowdown ? (
            <>
              <label>
                White
                <select value={whiteAi} onChange={(e) => setWhiteAi(e.target.value)}>
                  <option value="ai">Alpha-beta</option>
                  <option value="ml">Neural net</option>
                </select>
              </label>
              <label>
                Black
                <select value={blackAi} onChange={(e) => setBlackAi(e.target.value)}>
                  <option value="ai">Alpha-beta</option>
                  <option value="ml">Neural net</option>
                </select>
              </label>
            </>
          ) : (
            <label>
              Play as
              <select value={myColor} onChange={(e) => setMyColor(e.target.value)}>
                <option value="WHITE">White</option>
                <option value="BLACK">Black</option>
              </select>
            </label>
          )}
          {(vsAi || aiShowdown) && (
            <label>
              Difficulty
              <select value={difficulty} onChange={(e) => setDifficulty(Number(e.target.value))}>
                {DIFFICULTIES.map((d) => (
                  <option key={d.value} value={d.value}>{d.label}</option>
                ))}
              </select>
            </label>
          )}
          <label>
            Time control
            <select value={timeControl} onChange={(e) => setTimeControl(e.target.value)}>
              {TIME_CONTROLS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={busy || !gameName.trim()}>
            {aiShowdown ? "Create & watch" : "Create game"}
          </button>
        </form>
      </section>

      {error && <p className="error">{error}</p>}

      <section className="games-card">
        <div className="games-header">
          <h2>Games</h2>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={showFinished}
              onChange={(e) => setShowFinished(e.target.checked)}
            />
            Show finished
          </label>
        </div>
        {visibleGames.length === 0 && <p className="muted">No games yet — create one!</p>}
        <table>
          <tbody>
            {visibleGames.map((game) => {
              const aiOnly = isAiName(game.whiteUsername) && isAiName(game.blackUsername);
              return (
                <tr key={game.gameID} className={game.gameOver ? "finished" : ""}>
                  <td className="game-name">
                    {game.gameName}
                    {game.gameOver && <span className="badge">finished</span>}
                    {aiOnly && !game.gameOver && <span className="badge ai">AI vs AI</span>}
                  </td>
                  <td>
                    <span className="seat">♔ {playerLabel(game.whiteUsername) || "—"}</span>
                    <span className="seat">♚ {playerLabel(game.blackUsername) || "—"}</span>
                  </td>
                  <td className="actions">
                    {!game.gameOver && (!game.whiteUsername || game.whiteUsername === auth.username) && (
                      <button onClick={() => handleJoin(game, "WHITE")}>
                        {game.whiteUsername === auth.username ? "Rejoin as White" : "Join as White"}
                      </button>
                    )}
                    {!game.gameOver && (!game.blackUsername || game.blackUsername === auth.username) && (
                      <button onClick={() => handleJoin(game, "BLACK")}>
                        {game.blackUsername === auth.username ? "Rejoin as Black" : "Join as Black"}
                      </button>
                    )}
                    <button className="secondary" onClick={() => handleObserve(game)}>
                      {aiOnly && !game.gameOver ? "Watch" : game.gameOver ? "Review" : "Observe"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
    </div>
  );
}
