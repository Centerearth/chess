package server;

import chess.ChessGame;
import com.google.gson.Gson;
import io.javalin.*;
import io.javalin.http.Context;
import recordandrequest.*;
import service.GameService;
import service.UserService;

import java.util.HashMap;

public class Server {

    private final Javalin javalin;
    private final UserService userService = new UserService();
    private final GameService gameService = new GameService();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"))
                .delete("/db", this::clearApplication)
                .delete("/session", this::logoutUser)
                .post("/session", this::loginUser)
                .post("/user", this::createNewUser)
                .post("/game", this::createNewGame)
                .put("/game", this::joinGame)
                .get("/game", this::listGames)
                .start();

        // Register your endpoints and exception handlers here.

    }

    private void clearApplication(Context context) {
            userService.clearAllData();
    }

    private void createNewUser(Context context) {
        HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
        String username = bodyObject.get("username");
        String password = bodyObject.get("password");
        String email = bodyObject.get("email");
        RegisterResult registerResult = userService.register(new RegisterRequest(
                username, password, email));

        String json = new Gson().toJson(registerResult);
        context.json(json);

    }

    private void loginUser(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String username = bodyObject.get("username");
            String password = bodyObject.get("password");
            LoginResult loginResult = userService.login(new LoginRequest(
                    username, password));

            String json = new Gson().toJson(loginResult);
            context.json(json);
        } catch (Exception e) { // change this later
            System.out.println("Hi2");
        }
    }

    private void logoutUser(Context context) {
        try {
            String authToken = getAuthToken(context);
            userService.logout(new LogoutRequest(authToken));
        } catch (Exception e) { // change all this
            System.out.println("Hi");
        }
    }

    private void createNewGame(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String gameName = bodyObject.get("gameName");
            String authToken = getAuthToken(context);
            CreateGameResult createGameResult = gameService.createGame(new CreateGameRequest(authToken,
                    gameName));

            String json = new Gson().toJson(createGameResult);
            context.json(json);
        } catch (Exception e) {
            System.out.println("hi3");
        }

    }

    private void joinGame(Context context) {
        try {
            HashMap<String, Object> bodyObject = getBodyObject(context, HashMap.class);
            String teamColorString = bodyObject.get("playerColor").toString();
            ChessGame.TeamColor teamColor = null;
            if (teamColorString.equals("WHITE")) {
                teamColor = ChessGame.TeamColor.WHITE;
            } else if (teamColorString.equals("BLACK")) {
                teamColor = ChessGame.TeamColor.BLACK;
            }

            String authToken = getAuthToken(context);
            int gameID = (int) (double) bodyObject.get("gameID"); //why does it convert to double?
            System.out.println(gameID);
            gameService.joinGame(new JoinGameRequest(authToken,
                    teamColor, gameID));
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private void listGames(Context context) {
        try {
            String authToken = getAuthToken(context);
            ListGameResult listGameResult = gameService.listAllGameMetaData(new ListGameRequest(authToken));

            String json = new Gson().toJson(listGameResult);
            context.json(json);
        } catch (Exception e) { // change all this
            System.out.println(e);
        }
    }

    private String getAuthToken (Context context) {
        return context.header("authorization");
    }

    private static <T> T getBodyObject(Context context, Class<T> classType) {
        var bodyObject = new Gson().fromJson(context.body(), classType);

        if (bodyObject == null) {
            throw new RuntimeException("missing required body");
        }

        return bodyObject;
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
