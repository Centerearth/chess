package service;

import chess.ChessGame;
import dataaccess.MemoryAuthDataAccess;
import dataaccess.MemoryGameDataAccess;
import dataaccess.MemoryUserDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
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


    public CreateGameResult createGame(CreateGameRequest createGameRequest) throws FailedLoginException {
        String authToken = createGameRequest.authToken();
        if (authToken.isBlank() || createGameRequest.gameName().isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        } else if (authDataAccess.getAuth(authToken) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            GameData newGameData = new GameData(generateID(), "", "",
                    createGameRequest.gameName(),
                    new ChessGame());
            gameDataAccess.addGameData();
            return new CreateGameResult(newGameData.gameID());
        }
    }
}