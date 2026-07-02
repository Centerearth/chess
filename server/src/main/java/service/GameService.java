package service;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.SQLAuthDataAccess;
import dataaccess.SQLGameDataAccess;
import model.AuthData;
import model.GameData;
import model.GameMetaData;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameService {
    public static final String AI_ALPHABETA_USERNAME = "ai";
    public static final String AI_ML_USERNAME = "ml";

    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();
    private final SQLAuthDataAccess authDataAccess = new SQLAuthDataAccess();
    private final ConcurrentHashMap<Integer, Object> gameLocks = new ConcurrentHashMap<>();

    public static boolean isAiUsername(String username) {
        return AI_ALPHABETA_USERNAME.equals(username) || AI_ML_USERNAME.equals(username);
    }

    public Object getLock(int gameID) {
        return gameLocks.computeIfAbsent(gameID, id -> new Object());
    }

    public GameService() throws DataAccessException {
    }

    public static int generateID() {
        Random r= new Random();
        return r.nextInt(Integer.MAX_VALUE);
    }

    public GameData getGame(int gameID) throws DataAccessException {
        return gameDataAccess.getGame(gameID);
    }

    public boolean gameDataExists(int gameID) {
        try {
            return (gameDataAccess.getGame(gameID) != null);
        } catch (DataAccessException e) {
            return false;
        }
    }

    public boolean authDataExists(String authToken) {
        try {
            return (authDataAccess.getAuth(authToken) != null);
        } catch (DataAccessException e) {
            return false;
        }
    }

    public AuthData getAuthData(String authToken) throws DataAccessException {
        return (authDataAccess.getAuth(authToken));
    }


    public CreateGameResult createGame(CreateGameRequest createGameRequest) throws FailedLoginException, DataAccessException {
        String authToken = createGameRequest.authToken();
        if (createGameRequest.gameName().isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        } else if (authDataAccess.getAuth(authToken) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            for (GameData existing : gameDataAccess.getAllGameData()) {
                if (existing.gameName().equals(createGameRequest.gameName())) {
                    throw new AlreadyTakenException("Error: a game with that name already exists");
                }
            }
            Integer minutes = createGameRequest.timeControlMinutes();
            if (minutes != null && (minutes < 1 || minutes > 180)) {
                throw new BadRequestException("Error: time control must be between 1 and 180 minutes");
            }
            Long timeMs = minutes == null ? null : minutes * 60_000L;

            int gameID = generateID();
            while (gameDataAccess.getGame(gameID) != null) {
                gameID = generateID();
            }

            ChessGame game = new ChessGame();
            ArrayList<String> positionHistory = new ArrayList<>();
            positionHistory.add(chess.FenSerializer.toFen(game));
            GameData newGameData = new GameData(gameID, null, null,
                    createGameRequest.gameName(), game, false,
                    new ArrayList<>(), null, positionHistory, null, timeMs, timeMs, null, null);
            gameDataAccess.addGameData(newGameData);
            return new CreateGameResult(newGameData.gameID());
        }
    }

    public ListGameResult listAllGameMetaData(ListGameRequest listGameRequest) throws FailedLoginException, DataAccessException {
        if (authDataAccess.getAuth(listGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            ArrayList<GameMetaData> allGameMetaData = new ArrayList<>();
            ArrayList<GameData> allGameData = gameDataAccess.getAllGameData();
            for (GameData game : allGameData) {
                allGameMetaData.add(new GameMetaData(
                        game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName(),
                        game.gameOver()));
            }
            return new ListGameResult(allGameMetaData);
        }
    }

    public void joinGame(JoinGameRequest joinGameRequest) throws FailedLoginException, DataAccessException {
        if (gameDataAccess.getGame(joinGameRequest.gameID()) == null) {
            throw new BadRequestException("Error: game does not exist");
        } else if (authDataAccess.getAuth(joinGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else if (joinGameRequest.teamColor() == null) {
            throw new BadRequestException("Error: Fields cannot be left blank");
        } else {
            synchronized (getLock(joinGameRequest.gameID())) {
                if (joinGameRequest.teamColor() == ChessGame.TeamColor.WHITE &&
                        gameDataAccess.getGame(joinGameRequest.gameID()).whiteUsername() != null) {
                    throw new AlreadyTakenException("Error: White is already taken");
                }
                if (joinGameRequest.teamColor() == ChessGame.TeamColor.BLACK &&
                        gameDataAccess.getGame(joinGameRequest.gameID()).blackUsername() != null) {
                    throw new AlreadyTakenException("Error: Black is already taken");
                }
                String username = authDataAccess.getAuth(joinGameRequest.authToken()).username();
                gameDataAccess.updateGame(joinGameRequest.teamColor(), joinGameRequest.gameID(), username);
            }
        }
    }

    public void addAiPlayer(String authToken, int gameID, ChessGame.TeamColor teamColor, String aiType, Integer difficulty)
            throws FailedLoginException, DataAccessException {
        if (!isAiUsername(aiType)) {
            throw new BadRequestException("Error: unknown AI type, use \"ai\" or \"ml\"");
        }
        if (difficulty != null && (difficulty < 1 || difficulty > 3)) {
            throw new BadRequestException("Error: difficulty must be 1 (easy), 2 (medium), or 3 (hard)");
        }
        if (gameDataAccess.getGame(gameID) == null) {
            throw new BadRequestException("Error: game does not exist");
        }
        if (authDataAccess.getAuth(authToken) == null) {
            throw new FailedLoginException("Error: unauthorized");
        }
        synchronized (getLock(gameID)) {
            GameData gameData = gameDataAccess.getGame(gameID);
            String existing = (teamColor == ChessGame.TeamColor.WHITE)
                    ? gameData.whiteUsername() : gameData.blackUsername();
            if (existing != null) {
                throw new AlreadyTakenException("Error: that color is already taken");
            }
            GameData updated = gameData.withUsername(teamColor, aiType);
            if (difficulty != null) {
                updated = updated.withAiDifficulty(difficulty);
            }
            gameDataAccess.putGame(updated);
        }
    }

    public void putGame(GameData gameData) throws DataAccessException {
        gameDataAccess.putGame(gameData);
    }

    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException {
        gameDataAccess.updateGame(teamColor, gameID, username);
    }

    public void updateBoard(int gameID, ChessGame game) throws DataAccessException {
        gameDataAccess.updateBoard(gameID, game);
    }

    public void updateGameWin(int gameID) throws DataAccessException {
        gameDataAccess.updateGameWin(gameID);
    }

    public void updateGameWin(int gameID, String result) throws DataAccessException {
        gameDataAccess.updateGameWin(gameID, result);
    }

    public boolean isGameWon(int gameID) throws DataAccessException {
        return gameDataAccess.isGameWon(gameID);
    }

    public ChessGame.TeamColor getColor(String username, int gameID) throws DataAccessException {
        String color = gameDataAccess.giveColorGivenUsername(username, gameID);
        if (Objects.equals(color, "BLACK")) {
            return ChessGame.TeamColor.BLACK;
        } else if (Objects.equals(color, "WHITE")) {
            return ChessGame.TeamColor.WHITE;
        }
        return null;
    }
}