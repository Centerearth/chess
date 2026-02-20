package service;

import chess.ChessGame;
import dataaccess.DataAccessException;
import dataaccess.SQLAuthDataAccess;
import dataaccess.SQLGameDataAccess;
import model.GameData;
import model.GameMetaData;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;
import java.util.ArrayList;
import java.util.Random;

public class GameService {
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();
    private final SQLAuthDataAccess authDataAccess = new SQLAuthDataAccess();

    public static int generateID() {
        Random r= new Random();
        return r.nextInt(500);
    }

    //for testing purposes
    public GameData getGame(int gameID) throws DataAccessException {
        return gameDataAccess.getGame(gameID);
    }

    public boolean gameDataExists(int gameID) throws DataAccessException {
        return (gameDataAccess.getGame(gameID) != null);
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
                    new ChessGame());
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
        if (authDataAccess.getAuth(joinGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else if (joinGameRequest.teamColor() == null) {
            throw new BadRequestException("Error: Fields cannot be left blank");
        } else if (gameDataAccess.getGame(joinGameRequest.gameID()) == null) {
            throw new BadRequestException("Error: game does not exist");
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
}