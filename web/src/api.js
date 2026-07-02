async function request(method, path, { authToken, body } = {}) {
  const headers = {};
  if (authToken) headers["authorization"] = authToken;
  if (body) headers["content-type"] = "application/json";

  const response = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    throw new Error(data?.message || `Request failed (${response.status})`);
  }
  return data;
}

export function register(username, password) {
  return request("POST", "/user", { body: { username, password } });
}

export function login(username, password) {
  return request("POST", "/session", { body: { username, password } });
}

export function logout(authToken) {
  return request("DELETE", "/session", { authToken });
}

export function listGames(authToken) {
  return request("GET", "/game", { authToken });
}

export function createGame(authToken, gameName, timeControlMinutes) {
  return request("POST", "/game", {
    authToken,
    body: { gameName, timeControlMinutes: timeControlMinutes ?? null },
  });
}

export function joinGame(authToken, gameID, playerColor) {
  return request("PUT", "/game", { authToken, body: { gameID, playerColor } });
}

export function addAiOpponent(authToken, gameID, playerColor, aiType, difficulty) {
  return request("PUT", "/game", {
    authToken,
    body: { gameID, playerColor, ai: aiType, aiDifficulty: difficulty ?? null },
  });
}

export function getValidMoves(authToken, gameID, row, col) {
  return request(
    "GET",
    `/moves?gameID=${gameID}&row=${row}&col=${col}`,
    { authToken },
  );
}

export async function downloadPgn(authToken, gameID) {
  const response = await fetch(`/pgn?gameID=${gameID}`, {
    headers: { authorization: authToken },
  });
  if (!response.ok) throw new Error("Could not export PGN");
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `game-${gameID}.pgn`;
  link.click();
  URL.revokeObjectURL(url);
}
