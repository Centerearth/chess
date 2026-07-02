export function connectToGame({ authToken, gameID, username, color, onMessage, onClose }) {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const socket = new WebSocket(`${protocol}://${window.location.host}/ws`);
  let closedByUs = false;

  const send = (payload) => {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(payload));
    }
  };

  const base = { authToken, gameID, username, color: color ?? "OBSERVER" };

  socket.onopen = () => send({ commandType: "CONNECT", ...base });

  socket.onmessage = (event) => {
    try {
      onMessage(JSON.parse(event.data));
    } catch (e) {
      console.error("Bad server message", event.data, e);
    }
  };

  socket.onclose = () => {
    if (!closedByUs && onClose) onClose();
  };

  return {
    makeMove(move) {
      send({ commandType: "MAKE_MOVE", ...base, move });
    },
    resign() {
      send({ commandType: "RESIGN", ...base });
    },
    leave() {
      closedByUs = true;
      send({ commandType: "LEAVE", ...base });
      socket.close();
    },
    close() {
      closedByUs = true;
      socket.close();
    },
  };
}
