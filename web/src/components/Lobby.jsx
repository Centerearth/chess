import { useCallback, useEffect, useState } from "react";
import { addAiOpponent, createGame, joinGame, listGames } from "../api.js";

const AI_LABELS = { ai: "AI (alpha-beta)", ml: "AI (neural net)" };

function playerLabel(username) {
  if (!username) return null;
  return AI_LABELS[username] || username;
}

export default function Lobby({ auth, onEnterGame, onLogout }) {
  const [games, setGames] = useState([]);
  const [error, setError] = useState(null);

  const [gameName, setGameName] = useState("");
  const [opponent, setOpponent] = useState("open");
  const [myColor, setMyColor] = useState("WHITE");
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

  const handleCreate = async (event) => {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const { gameID } = await createGame(auth.authToken, gameName.trim());
      await joinGame(auth.authToken, gameID, myColor);
      if (opponent !== "open") {
        const aiColor = myColor === "WHITE" ? "BLACK" : "WHITE";
        await addAiOpponent(auth.authToken, gameID, aiColor, opponent);
      }
      onEnterGame({ gameID, gameName: gameName.trim(), color: myColor });
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
            </select>
          </label>
          <label>
            Play as
            <select value={myColor} onChange={(e) => setMyColor(e.target.value)}>
              <option value="WHITE">White</option>
              <option value="BLACK">Black</option>
            </select>
          </label>
          <button type="submit" disabled={busy || !gameName.trim()}>
            Create game
          </button>
        </form>
      </section>

      {error && <p className="error">{error}</p>}

      <section className="games-card">
        <h2>Games</h2>
        {games.length === 0 && <p className="muted">No games yet — create one!</p>}
        <table>
          <tbody>
            {games.map((game) => (
              <tr key={game.gameID}>
                <td className="game-name">{game.gameName}</td>
                <td>
                  <span className="seat">♔ {playerLabel(game.whiteUsername) || "—"}</span>
                  <span className="seat">♚ {playerLabel(game.blackUsername) || "—"}</span>
                </td>
                <td className="actions">
                  {(!game.whiteUsername || game.whiteUsername === auth.username) && (
                    <button onClick={() => handleJoin(game, "WHITE")}>
                      {game.whiteUsername === auth.username ? "Rejoin as White" : "Join as White"}
                    </button>
                  )}
                  {(!game.blackUsername || game.blackUsername === auth.username) && (
                    <button onClick={() => handleJoin(game, "BLACK")}>
                      {game.blackUsername === auth.username ? "Rejoin as Black" : "Join as Black"}
                    </button>
                  )}
                  <button className="secondary" onClick={() => handleObserve(game)}>
                    Observe
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
