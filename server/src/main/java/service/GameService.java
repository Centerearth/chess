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

public class GameService {
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();
    private final SQLAuthDataAccess authDataAccess = new SQLAuthDataAccess();

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
            GameData newGameData = new GameData(generateID(), null, null,
                    createGameRequest.gameName(),
                    new ChessGame(),
                    ChessGame.TeamColor.WHITE, false);
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
                        game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName()));
            }
            return new ListGameResult(allGameMetaData);
        }
    }

    public void joinGame(JoinGameRequest joinGameRequest) throws FailedLoginException, DataAccessException {
        if (!gameDataExists(joinGameRequest.gameID())) {
            throw new DataAccessException("Error: game does not exist");
        } else if (!authDataExists(joinGameRequest.authToken())) {
            throw new FailedLoginException("Error: unauthorized");
        } else if (joinGameRequest.teamColor() == null) {
            throw new BadRequestException("Error: Fields cannot be left blank");
        } else {
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

    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException {
        gameDataAccess.updateGame(teamColor, gameID, username);
    }

    public void updateBoard(int gameID, ChessGame game) throws DataAccessException {
        gameDataAccess.updateBoard(gameID, game);
    }

    public void updateTurn(int gameID, ChessGame.TeamColor whoseTurn) throws DataAccessException {
        gameDataAccess.updateTurn(gameID, whoseTurn);
    }

    public void updateGameWin(int gameID) throws DataAccessException {
        gameDataAccess.updateGameWin(gameID);
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