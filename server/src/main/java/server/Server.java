package server;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.*;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import recordandrequest.*;
import service.GameService;
import service.UserService;

import javax.security.auth.login.FailedLoginException;
import java.util.Map;

public class Server {

    private Javalin javalin;
    private UserService userService;
    private GameService gameService;
    private WebsocketHandler websocketHandler;

    public Server() {
        try {
            userService = new UserService();
            gameService = new GameService();
            websocketHandler = new WebsocketHandler(gameService);

            javalin = Javalin.create(config -> {config.staticFiles.add("web");
                config.bundledPlugins.enableDevLogging();})
                    .delete("/db", this::clearApplication)
                    .delete("/session", this::logoutUser)
                    .post("/session", this::loginUser)
                    .post("/user", this::createNewUser)
                    .post("/game", this::createNewGame)
                    .put("/game", this::joinGame)
                    .get("/game", this::listGames)
                    .get("/moves", this::validMoves)
                    .get("/pgn", this::exportPgn)
                    .exception(Exception.class, this::httpExceptionHandler)
                    .ws("/ws", ws -> {
                        ws.onConnect(websocketHandler);
                        ws.onMessage(websocketHandler);
                        ws.onClose(websocketHandler);
                    });
    } catch (Exception e) {
        javalin = Javalin.create(config -> config.staticFiles.add("web"))
                .before(ctx -> exceptionHandler(ctx, e));
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
            RegisterRequest body = getBodyObject(context, RegisterRequest.class);

            if (body.username() == null || body.password() == null) {
                throw new BadRequestException("Error: bad request");
            }

            RegisterResult registerResult = userService.register(body);

            sendJson(context, registerResult);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void loginUser(Context context) {
        try {
            LoginRequest body = getBodyObject(context, LoginRequest.class);

            if (body.username() == null || body.password() == null) {
                throw new BadRequestException("Error: bad request");
            }

            LoginResult loginResult = userService.login(body);

            sendJson(context, loginResult);
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

    private record CreateGameBody(String gameName, Integer timeControlMinutes) {}
    private record JoinGameBody(String playerColor, int gameID, String ai, Integer aiDifficulty) {}

    private void createNewGame(Context context) {
        try {
            CreateGameBody body = getBodyObject(context, CreateGameBody.class);
            String authToken = getAuthToken(context);
            if (authToken == null || body.gameName() == null) {
                throw new BadRequestException("Error: bad request");
            }
            CreateGameResult createGameResult = gameService.createGame(
                    new CreateGameRequest(authToken, body.gameName(), body.timeControlMinutes()));

            sendJson(context, createGameResult);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void joinGame(Context context) {
        try {
            JoinGameBody body = getBodyObject(context, JoinGameBody.class);
            String authToken = getAuthToken(context);

            if (authToken == null || body.playerColor() == null) {
                throw new BadRequestException("Error: bad request");
            }

            ChessGame.TeamColor teamColor = switch (body.playerColor()) {
                case "WHITE" -> ChessGame.TeamColor.WHITE;
                case "BLACK" -> ChessGame.TeamColor.BLACK;
                default -> throw new BadRequestException("Error: bad request");
            };

            if (body.ai() != null) {
                gameService.addAiPlayer(authToken, body.gameID(), teamColor, body.ai(), body.aiDifficulty());
                websocketHandler.scheduleAiMoveIfNeeded(body.gameID());
            } else {
                gameService.joinGame(new JoinGameRequest(authToken, teamColor, body.gameID()));
            }
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void validMoves(Context context) {
        try {
            String authToken = getAuthToken(context);
            if (authToken == null || !gameService.authDataExists(authToken)) {
                throw new FailedLoginException("Error: unauthorized");
            }
            int gameID = Integer.parseInt(context.queryParam("gameID"));
            int row = Integer.parseInt(context.queryParam("row"));
            int col = Integer.parseInt(context.queryParam("col"));

            var gameData = gameService.getGame(gameID);
            if (gameData == null) {
                throw new BadRequestException("Error: game does not exist");
            }
            var moves = gameData.game().validMoves(new chess.ChessPosition(row, col));

            sendJson(context, Map.of("moves", moves == null ? java.util.List.of() : moves));
        } catch (NumberFormatException e) {
            exceptionHandler(context, new BadRequestException("Error: bad request"));
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

            sendJson(context, listGameResult);
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private void exportPgn(Context context) {
        try {
            String authToken = getAuthToken(context);
            if (authToken == null || !gameService.authDataExists(authToken)) {
                throw new FailedLoginException("Error: unauthorized");
            }
            int gameID = Integer.parseInt(context.queryParam("gameID"));
            var gameData = gameService.getGame(gameID);
            if (gameData == null) {
                throw new BadRequestException("Error: game does not exist");
            }
            context.contentType("application/x-chess-pgn");
            context.header("Content-Disposition", "attachment; filename=\"game-" + gameID + ".pgn\"");
            context.result(PgnBuilder.build(gameData));
        } catch (NumberFormatException e) {
            exceptionHandler(context, new BadRequestException("Error: bad request"));
        } catch (Exception e) {
            exceptionHandler(context, e);
        }
    }

    private String getAuthToken (Context context) {
        return context.header("authorization");
    }

    private static void sendJson(Context context, Object body) {
        context.contentType("application/json");
        context.result(new Gson().toJson(body));
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
            context.status(HttpStatus.FORBIDDEN);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof BadRequestException) {
            context.status(HttpStatus.BAD_REQUEST);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof FailedLoginException) {
            context.status(HttpStatus.UNAUTHORIZED);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else if (e instanceof DataAccessException) {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        } else {
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
            context.result(new Gson().toJson(Map.of("message", e.getMessage())));
        }
    }

    private void httpExceptionHandler(Exception ex, Context ctx) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.result(new Gson().toJson(Map.of("message", ex.getMessage())));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
