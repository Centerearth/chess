package client;

import websocket.messages.LoadGameMessage;
import websocket.messages.ServerMessage;

public interface ServerMessageObserver {
    void notify(ServerMessage serverMessage);

    void displayGame(LoadGameMessage loadGameMessage);
}

