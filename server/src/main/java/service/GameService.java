package service;

import chess.ChessGame;
import dataaccess.MemoryAuthDataAccess;
import dataaccess.MemoryGameDataAccess;
import model.GameData;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;
import java.util.Random;

public class GameService {
    private final MemoryGameDataAccess gameDataAccess = new MemoryGameDataAccess();
    private final MemoryAuthDataAccess authDataAccess = new MemoryAuthDataAccess();

    public static int generateID() {
        Random r= new Random();
        return r.nextInt(500);
    }

    //for testing purposes
    public GameData getGame(int gameID) {
        return gameDataAccess.getGame(gameID);
    }

    public boolean gameDataExists(int gameID) {
        return (gameDataAccess.getGame(gameID) != null);
    }

    public CreateGameResult createGame(CreateGameRequest createGameRequest) throws FailedLoginException {
        String authToken = createGameRequest.authToken();
        if (createGameRequest.gameName().isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        } else if (authDataAccess.getAuth(authToken) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            GameData newGameData = new GameData(generateID(), "", "",
                    createGameRequest.gameName(),
                    new ChessGame());
            gameDataAccess.addGameData(newGameData);
            return new CreateGameResult(newGameData.gameID());
        }
    }

    public ListGameResult getAllGameData(ListGameRequest listGameRequest) throws FailedLoginException {
        if (authDataAccess.getAuth(listGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            return new ListGameResult(gameDataAccess.getAllGameData());
        }
    }
}