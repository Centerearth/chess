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

    public ListGameResult listAllGameData(ListGameRequest listGameRequest) throws FailedLoginException {
        if (authDataAccess.getAuth(listGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            return new ListGameResult(gameDataAccess.getAllGameData());
        }
    }

    public void joinGame(JoinGameRequest joinGameRequest) throws FailedLoginException {
        if (authDataAccess.getAuth(joinGameRequest.authToken()) == null) {
            throw new FailedLoginException("Error: unauthorized");
        } else if (joinGameRequest.teamColor() == null) {
            throw new BadRequestException("Error: Fields cannot be left blank");
        } else if (gameDataAccess.getGame(joinGameRequest.gameID()) == null) {
            throw new BadRequestException("Error: game does not exist");
        } else {
            if (joinGameRequest.teamColor() == ChessGame.TeamColor.WHITE &&
                    !gameDataAccess.getGame(joinGameRequest.gameID()).whiteUsername().isBlank()) {
                throw new AlreadyTakenException("Error: White is already taken");
            }
            if (joinGameRequest.teamColor() == ChessGame.TeamColor.BLACK &&
                    !gameDataAccess.getGame(joinGameRequest.gameID()).blackUsername().isBlank()) {
                throw new AlreadyTakenException("Error: Black is already taken");
            }
            String username = authDataAccess.getAuth(joinGameRequest.authToken()).username();
            gameDataAccess.updateGame(joinGameRequest.teamColor(), joinGameRequest.gameID(), username);
        }
    }
}