package serverfacade;

import chess.ChessMove;
import client.ServerMessageObserver;
import com.google.gson.Gson;

import jakarta.websocket.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static websocket.commands.UserGameCommand.CommandType.*;

public class WebsocketFacade extends Endpoint {

    Session session;
    ServerMessageObserver serverMessageObserver;

    public WebsocketFacade(String url, ServerMessageObserver serverMessageObserver) throws Exception {
        try {
            url = url.replace("http", "ws");
            URI socketURI = new URI(url + "/ws");
            this.serverMessageObserver = serverMessageObserver;

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            //set message handler
            this.session.addMessageHandler(new MessageHandler.Whole<String>() { //can replace with lambda
                @Override
                public void onMessage(String message) {

                    ServerMessage serverMessage = new Gson().fromJson(message, ServerMessage.class);

                    if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.LOAD_GAME) {
                        LoadGameMessage loadGameMessage = new Gson().fromJson(message, LoadGameMessage.class); //reassigning
                        serverMessageObserver.displayGame(loadGameMessage);
                    } else if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.NOTIFICATION) {
                        NotificationMessage notificationMessage = new Gson().fromJson(message, NotificationMessage.class); //reassigning
                        serverMessageObserver.notify(notificationMessage);
                    } else if (serverMessage.getServerMessageType() == ServerMessage.ServerMessageType.ERROR) {
                        ErrorMessage errorMessage = new Gson().fromJson(message, ErrorMessage.class); //reassigning
                        serverMessageObserver.notifyError(errorMessage);
                    } else {
                        serverMessageObserver.notifyDefault(serverMessage);
                    }
                }
            });
        } catch (DeploymentException | IOException | URISyntaxException ex) {
            throw new Exception(ex.getMessage()); // change this ??
        }
    }

    //don't change
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
    }

    //have each of the different requests have a function here

    public void connect(String authToken, int gameID, String username, String color) throws Exception {
        try {
            UserGameCommand userGameCommand = new UserGameCommand(CONNECT, authToken, gameID, username, color);
            this.session.getBasicRemote().sendText(new Gson().toJson(userGameCommand));
        } catch (IOException ex) {
            throw new Exception(ex.getMessage());
        }
    }

    public void leave(String authToken, int gameID, String username, String color) throws IOException {
        UserGameCommand userGameCommand = new UserGameCommand(LEAVE, authToken, gameID, username, color);
        this.session.getBasicRemote().sendText(new Gson().toJson(userGameCommand));
    }

    public void makeMove(String authToken, int gameID, String username, String color, ChessMove move) throws IOException {
        MakeMoveCommand makeMoveCommand = new MakeMoveCommand(MAKE_MOVE, authToken, gameID, username, color, move);
        this.session.getBasicRemote().sendText(new Gson().toJson(makeMoveCommand));
    }

    public void resign(String authToken, int gameID, String username, String color) throws IOException {
        UserGameCommand userGameCommand = new UserGameCommand(RESIGN, authToken, gameID, username, color);
        this.session.getBasicRemote().sendText(new Gson().toJson(userGameCommand));
    }

}