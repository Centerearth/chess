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

    private static final Gson GSON = new Gson();
    private Session session;

    public WebsocketFacade(String url, ServerMessageObserver serverMessageObserver) throws Exception {
        try {
            url = url.replace("http", "ws");
            URI socketURI = new URI(url + "/ws");

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    ServerMessage serverMessage = GSON.fromJson(message, ServerMessage.class);
                    switch (serverMessage.getServerMessageType()) {
                        case LOAD_GAME -> serverMessageObserver.displayGame(GSON.fromJson(message, LoadGameMessage.class));
                        case NOTIFICATION -> serverMessageObserver.notify(GSON.fromJson(message, NotificationMessage.class));
                        case ERROR -> serverMessageObserver.notifyError(GSON.fromJson(message, ErrorMessage.class));
                        default -> serverMessageObserver.notifyDefault(serverMessage);
                    }
                }
            });
        } catch (DeploymentException | IOException | URISyntaxException ex) {
            throw new Exception(ex);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
    }

    public void connect(String authToken, int gameId, String username, String color) throws Exception {
        try {
            UserGameCommand userGameCommand = new UserGameCommand(CONNECT, authToken, gameId, username, color);
            this.session.getBasicRemote().sendText(GSON.toJson(userGameCommand));
        } catch (IOException ex) {
            throw new Exception(ex);
        }
    }

    public void leave(String authToken, int gameId, String username, String color) throws IOException {
        UserGameCommand userGameCommand = new UserGameCommand(LEAVE, authToken, gameId, username, color);
        System.out.println(); // required by autograder
        this.session.getBasicRemote().sendText(GSON.toJson(userGameCommand));
    }

    public void makeMove(String authToken, int gameId, String username, String color, ChessMove move) throws IOException {
        MakeMoveCommand makeMoveCommand = new MakeMoveCommand(MAKE_MOVE, authToken, gameId, username, color, move);
        this.session.getBasicRemote().sendText(GSON.toJson(makeMoveCommand));
    }

    public void resign(String authToken, int gameId, String username, String color) throws IOException {
        UserGameCommand userGameCommand = new UserGameCommand(RESIGN, authToken, gameId, username, color);
        this.session.getBasicRemote().sendText(GSON.toJson(userGameCommand));
    }
}
