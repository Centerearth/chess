package server;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsMessageHandler;
import org.eclipse.jetty.websocket.api.Session;
import websocket.commands.UserGameCommand;

import java.io.IOException;

public class WebsocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private final ConnectionManager allConnections = new ConnectionManager();

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("Websocket connected"); //probably should delete this but need some visual confirmation for now
        ctx.enableAutomaticPings();
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        UserGameCommand userGameCommand = new Gson().fromJson(ctx.message(), UserGameCommand.class);
        switch (userGameCommand.getCommandType()) {
            case CONNECT -> connect(ctx);
            case MAKE_MOVE -> makeMove(ctx);
            case LEAVE -> leave(ctx);
            case RESIGN -> resign(ctx);
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed"); //same thing here? delete?
    }

    private void connect(WsMessageContext ctx) {

    }

    private void makeMove(WsMessageContext ctx) {

    }

    private void leave(WsMessageContext ctx) {

    }

    private void resign(WsMessageContext ctx) {

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