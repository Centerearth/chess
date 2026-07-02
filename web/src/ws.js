const MAX_RECONNECT_ATTEMPTS = 6;

export function connectToGame({ authToken, gameID, username, color, onMessage, onStatus }) {
  let socket = null;
  let closedByUs = false;
  let attempts = 0;

  const base = { authToken, gameID, username, color: color ?? "OBSERVER" };

  const send = (payload) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(payload));
    }
  };

  const open = () => {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    socket = new WebSocket(`${protocol}://${window.location.host}/ws`);

    socket.onopen = () => {
      attempts = 0;
      onStatus?.("connected");
      send({ commandType: "CONNECT", ...base });
    };

    socket.onmessage = (event) => {
      try {
        onMessage(JSON.parse(event.data));
      } catch (e) {
        console.error("Bad server message", event.data, e);
      }
    };

    socket.onclose = () => {
      if (closedByUs) return;
      if (attempts < MAX_RECONNECT_ATTEMPTS) {
        onStatus?.("reconnecting");
        const delay = Math.min(1000 * 2 ** attempts, 8000);
        attempts += 1;
        setTimeout(() => {
          if (!closedByUs) open();
        }, delay);
      } else {
        onStatus?.("disconnected");
      }
    };
  };

  open();

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
      socket?.close();
    },
    close() {
      closedByUs = true;
      socket?.close();
    },
  };
}
