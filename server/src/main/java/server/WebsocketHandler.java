package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsMessageHandler;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;

import static websocket.messages.ServerMessage.ServerMessageType.*;


public class WebsocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private final ConnectionManager allConnections = new ConnectionManager();
    private final GameService gameService = new GameService();

    public WebsocketHandler() throws DataAccessException {
    }

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("Websocket connected"); //probably should delete this but need some visual confirmation for now
        ctx.enableAutomaticPings();
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        try {
            UserGameCommand userGameCommand = new Gson().fromJson(ctx.message(), UserGameCommand.class);
            switch (userGameCommand.getCommandType()) {
                case CONNECT -> connect(userGameCommand, ctx.session);
                case MAKE_MOVE -> makeMove(userGameCommand, ctx.session);
                case LEAVE -> leave(userGameCommand, ctx.session);
                case RESIGN -> resign(userGameCommand, ctx.session);
            }
        } catch (IOException ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed"); //same thing here? delete?
    }

    private void connect(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
        allConnections.add(userGameCommand.getGameID(), session);

        ChessGame game = gameService.getGame(userGameCommand.getGameID()).game();
        LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, game);

        String notification = String.format("%s has joined the game", userGameCommand.getUsername());
        NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

        allConnections.broadcastAll(loadGameMessage, userGameCommand.getGameID());
        allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

        } catch (Exception e) {
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to connect"));
        }

    }

    private void makeMove(UserGameCommand userGameCommand, Session session) {
        //authToken check
    }
    private void leave(UserGameCommand userGameCommand, Session session) {

    }
    private void resign(UserGameCommand userGameCommand, Session session) {

    }

//    private void enter(String visitorName, Session session) throws IOException {
//        connections.add(session);
//        var message = String.format("%s is in the shop", visitorName);
//        var notification = new Notification(Notification.Type.ARRIVAL, message);
//        connections.broadcast(session, notification);
//    }
//
//    private void exit(String visitorName, Session session) throws IOException {
//        var message = String.format("%s left the shop", visitorName);
//        var notification = new Notification(Notification.Type.DEPARTURE, message);
//        connections.broadcast(session, notification);
//        connections.remove(session);
//    }
//
//    public void makeNoise(String petName, String sound) throws ResponseException {
//        try {
//            var message = String.format("%s says %s", petName, sound);
//            var notification = new Notification(Notification.Type.NOISE, message);
//            connections.broadcast(null, notification);
//        } catch (Exception ex) {
//            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
//        }
//    }
}