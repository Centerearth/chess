package server;

import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.ErrorMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    public final ConcurrentHashMap<Integer, HashSet<Session>> allConnections = new ConcurrentHashMap<>();

    public void add(int gameID, Session session) {
        if (allConnections.get(gameID) == null) {
            addNewGame(gameID);
        }
        allConnections.get(gameID).add(session);
    }

    private void addNewGame(int gameID) {
        allConnections.put(gameID, new HashSet<Session>());
    }

    public void remove(int gameID) {
        allConnections.remove(gameID);
    }

    public void broadcastAll(ServerMessage serverMessage, int gameID) throws IOException {
        //String msg = notification.toString();
        //this needs to be a JSON
        String msg = "";
        HashSet<Session> connections = allConnections.get(gameID);
        for (Session s : connections) {
            if (s.isOpen()) {
                s.getRemote().sendString(msg);
            }
        }
    }

    public void broadcastSome(Session excludeSession, ServerMessage serverMessage, int gameID) throws IOException {
        //String msg = notification.toString();
        //this needs to be a JSON
        String msg = "";
        HashSet<Session> connections = allConnections.get(gameID);
        for (Session s : connections) {
            if (s.isOpen()) {
                if (!s.equals(excludeSession)) {
                    s.getRemote().sendString(msg);
                }
            }
        }
    }

    public void broadcastError(Session session, ErrorMessage errorMessage) throws IOException {
        String error = errorMessage.getMessage();
        if (session.isOpen()) {
            session.getRemote().sendString(error);
        }
    }

}