package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.*;
import io.javalin.http.Context;
import recordandrequest.*;
import service.GameService;
import service.UserService;

import javax.security.auth.login.FailedLoginException;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private Javalin javalin;
    private UserService userService;
    private GameService gameService;

    public Server() {
        try {
            userService = new UserService();
            gameService = new GameService();
            javalin = Javalin.create(config -> config.staticFiles.add("web"))
                    .delete("/db", this::clearApplication)
                    .delete("/session", this::logoutUser)
                    .post("/session", this::loginUser)
                    .post("/user", this::createNewUser)
                    .post("/game", this::createNewGame)
                    .put("/game", this::joinGame)
                    .get("/game", this::listGames);
    } catch (Exception e) {
        javalin = Javalin.create(config -> config.staticFiles.add("web"))
                .before(ctx -> {exceptionHandler(ctx, e);});
    }
    }

    private void clearApplication(Context context) {
        try {
            userService.clearAllData();
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void createNewUser(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String username = bodyObject.get("username");
            String password = bodyObject.get("password");
            String email = bodyObject.get("email");

            if (username == null || password == null || email == null) {
                throw new BadRequestException("Error: bad request");
            }

            RegisterResult registerResult = userService.register(new RegisterRequest(
                    username, password, email));

            String json = new Gson().toJson(registerResult);
            context.json(json);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }

    }

    private void loginUser(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String username = bodyObject.get("username");
            String password = bodyObject.get("password");

            if (username == null || password == null) {
                throw new BadRequestException("Error: bad request");
            }

            LoginResult loginResult = userService.login(new LoginRequest(
                    username, password));

            String json = new Gson().toJson(loginResult);
            context.json(json);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void logoutUser(Context context) {
        try {
            String authToken = getAuthToken(context);
            if (authToken == null) {
                throw new BadRequestException("Error: bad request");
            }
            userService.logout(new LogoutRequest(authToken));
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void createNewGame(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String gameName = bodyObject.get("gameName");
            String authToken = getAuthToken(context);
            if (authToken == null || gameName == null) {
                throw new BadRequestException("Error: bad request");
            }
            CreateGameResult createGameResult = gameService.createGame(new CreateGameRequest(authToken,
                    gameName));

            String json = new Gson().toJson(createGameResult);
            context.json(json);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }

    }

    private void joinGame(Context context) {
        try {
            HashMap<String, Object> bodyObject = getBodyObject(context, HashMap.class);
            String authToken = getAuthToken(context);

            if (authToken == null || bodyObject.get("gameID") == null || bodyObject.get("playerColor") == null) {
                throw new BadRequestException("Error: bad request");
            }

            String teamColorString = bodyObject.get("playerColor").toString();
            ChessGame.TeamColor teamColor = null;
            if (teamColorString.equals("WHITE")) {
                teamColor = ChessGame.TeamColor.WHITE;
            } else if (teamColorString.equals("BLACK")) {
                teamColor = ChessGame.TeamColor.BLACK;
            }

            int gameID = (int) (double) bodyObject.get("gameID"); //why does it convert to double?

            gameService.joinGame(new JoinGameRequest(authToken,
                    teamColor, gameID));
        } catch (Exception e) {
            exceptionHandler(context, e);
        }

    }

    private void listGames(Context context) {
        try {
            String authToken = getAuthToken(context);
            if (authToken == null) {
                throw new BadRequestException("Error: bad request");
            }
            ListGameResult listGameResult = gameService.listAllGameMetaData(new ListGameRequest(authToken));

            String json = new Gson().toJson(listGameResult);
            context.json(json);
        } catch (Exception e) {
            exceptionHandler(context, e);
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

    public void exceptionHandler(Context context, Exception e) {
        context.contentType("application/json");
        if (e instanceof AlreadyTakenException) {
            context.status(403);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof BadRequestException) {
            context.status(400);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof FailedLoginException) {
            context.status(401);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof DataAccessException) {
            context.status(500);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else {
            context.status(500);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        }
    }
    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
