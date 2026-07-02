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
      // the server double-encodes some responses as JSON strings
      if (typeof data === "string") data = JSON.parse(data);
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

export function createGame(authToken, gameName) {
  return request("POST", "/game", { authToken, body: { gameName } });
}

export function joinGame(authToken, gameID, playerColor) {
  return request("PUT", "/game", { authToken, body: { gameID, playerColor } });
}

export function addAiOpponent(authToken, gameID, playerColor, aiType) {
  return request("PUT", "/game", {
    authToken,
    body: { gameID, playerColor, ai: aiType },
  });
}

export function getValidMoves(authToken, gameID, row, col) {
  return request(
    "GET",
    `/moves?gameID=${gameID}&row=${row}&col=${col}`,
    { authToken },
  );
}
