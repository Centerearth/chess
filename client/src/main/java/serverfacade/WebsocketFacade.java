package serverfacade;

import client.ServerMessageObserver;
import com.google.gson.Gson;

import jakarta.websocket.*;
import websocket.commands.UserGameCommand;
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
                public void onMessage(String message) { //should this be String??
                    System.out.println("I am inside of onMessage");
                    ServerMessage serverMessage = new Gson().fromJson(message, ServerMessage.class);
                    System.out.println("\nI received a message\n");
                    System.out.println(serverMessage);
                    serverMessageObserver.notify(serverMessage);
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

    public void connect(String authToken, int gameID, String username) throws Exception {
        try {
            UserGameCommand userGameCommand = new UserGameCommand(CONNECT, authToken, gameID, username);
            this.session.getBasicRemote().sendText(new Gson().toJson(userGameCommand));
        } catch (IOException ex) {
            throw new Exception(ex.getMessage());
        }
    }

//    public void enterPetShop(String visitorName) throws ResponseException {
//        try {
//            var action = new Action(Action.Type.ENTER, visitorName);
//            this.session.getBasicRemote().sendText(new Gson().toJson(action));
//        } catch (IOException ex) {
//            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
//        }
//    }
//
//    public void leavePetShop(String visitorName) throws ResponseException {
//        try {
//            var action = new Action(Action.Type.EXIT, visitorName);
//            this.session.getBasicRemote().sendText(new Gson().toJson(action));
//        } catch (IOException ex) {
//            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
//        }
//    }

}