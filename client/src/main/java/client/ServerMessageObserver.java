package client;

import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

public interface ServerMessageObserver {

    void notify(NotificationMessage notificationMessage);

    void notifyError(ErrorMessage errorMessage);

    void notifyDefault(ServerMessage serverMessage);

    void displayGame(LoadGameMessage loadGameMessage);
}

