import { useState } from "react";
import Auth from "./components/Auth.jsx";
import Lobby from "./components/Lobby.jsx";
import Game from "./components/Game.jsx";
import { logout } from "./api.js";

const AUTH_KEY = "chess-auth";

function loadAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY));
  } catch {
    return null;
  }
}

export default function App() {
  const [auth, setAuth] = useState(loadAuth);
  // activeGame: { gameID, gameName, color } — color null means observing
  const [activeGame, setActiveGame] = useState(null);

  const handleLogin = (result) => {
    localStorage.setItem(AUTH_KEY, JSON.stringify(result));
    setAuth(result);
  };

  const handleLogout = async () => {
    try {
      await logout(auth.authToken);
    } catch {
      // token may already be expired; log out locally regardless
    }
    localStorage.removeItem(AUTH_KEY);
    setAuth(null);
    setActiveGame(null);
  };

  if (!auth) {
    return <Auth onLogin={handleLogin} />;
  }

  if (activeGame) {
    return (
      <Game
        auth={auth}
        game={activeGame}
        onExit={() => setActiveGame(null)}
      />
    );
  }

  return (
    <Lobby
      auth={auth}
      onEnterGame={setActiveGame}
      onLogout={handleLogout}
    />
  );
}
