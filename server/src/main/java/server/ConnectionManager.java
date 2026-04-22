package server;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.ErrorMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    public final ConcurrentHashMap<Integer, Set<Session>> allConnections = new ConcurrentHashMap<>();

    public void add(int gameID, Session session) {
        allConnections.computeIfAbsent(gameID, id -> ConcurrentHashMap.newKeySet());
        allConnections.get(gameID).add(session);
    }

    public void remove(int gameID) {
        allConnections.remove(gameID);
    }

    public void removeSession(int gameID, Session session) {
        allConnections.get(gameID).remove(session);
    }

    public void broadcastAll(ServerMessage serverMessage, int gameID) throws IOException {
        String messageJson = new Gson().toJson(serverMessage);
        Set<Session> connections = allConnections.get(gameID);
        for (Session s : connections) {
            if (s.isOpen()) {
                s.getRemote().sendString(messageJson);
            }
        }
    }

    public void broadcastSome(Session excludeSession, ServerMessage serverMessage, int gameID) throws IOException {
        String messageJson = new Gson().toJson(serverMessage);
        Set<Session> connections = allConnections.get(gameID);
        for (Session s : connections) {
            if (s.isOpen()) {
                if (!s.equals(excludeSession)) {
                    s.getRemote().sendString(messageJson);
                }
            }
        }
    }

    public void broadcastOne(Session session, ServerMessage serverMessage, int gameID) throws IOException {
        String messageJson = new Gson().toJson(serverMessage);
        Set<Session> connections = allConnections.get(gameID);
        for (Session s : connections) {
            if (s.isOpen()) {
                if (s.equals(session)) {
                    s.getRemote().sendString(messageJson);
                }
            }
        }
    }

    public void broadcastError(Session session, ErrorMessage errorMessage) throws IOException {
        String errorJson = new Gson().toJson(errorMessage);
        if (session.isOpen()) {
            System.out.println(errorJson);
            session.getRemote().sendString(errorJson);
        }
    }

}