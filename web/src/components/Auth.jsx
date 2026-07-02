import { useState } from "react";
import { login, register } from "../api.js";

export default function Auth({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const action = mode === "login" ? login : register;
      const result = await action(username, password);
      onLogin({ username: result.username, authToken: result.authToken });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <h1>♔ Chess</h1>
      <form className="auth-card" onSubmit={submit}>
        <h2>{mode === "login" ? "Log in" : "Create an account"}</h2>
        <input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoFocus
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={busy || !username || !password}>
          {mode === "login" ? "Log in" : "Register"}
        </button>
        <button
          type="button"
          className="link"
          onClick={() => {
            setMode(mode === "login" ? "register" : "login");
            setError(null);
          }}
        >
          {mode === "login"
            ? "Need an account? Register"
            : "Have an account? Log in"}
        </button>
      </form>
    </div>
  );
}
