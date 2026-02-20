package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import recordandrequest.*;
import service.UserService;

import java.util.ArrayList;


public class DataAccessTests {
    private final UserService userService = new UserService();
    private final SQLUserDataAccess userDataAccess = new SQLUserDataAccess();
    private final SQLAuthDataAccess authDataAccess = new SQLAuthDataAccess();
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();

    public DataAccessTests() throws DataAccessException {
    }

    @Test
    @Order(1)
    @DisplayName("Add new game")
    public void addGameSuccess() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        GameData testGame = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame);
        Assertions.assertEquals(testGame, gameDataAccess.getGame(10),
                "Returned game was not the same game as was added");
        gameDataAccess.removeAllGameData();
    }

    @Test
    @Order(2)
    @DisplayName("Game already exists")
    public void addGameFailure() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        GameData testGame1 = new GameData(10, null, null,
                "game1", new ChessGame());
        GameData testGame2 = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame1);
        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.addGameData(testGame2));
        gameDataAccess.removeAllGameData();
    }

    @Test
    @Order(3)
    @DisplayName("Clear all games")
    public void ClearGameSuccess() throws DataAccessException {
        GameData testGame1 = new GameData(10, null, null,
                "game1", new ChessGame());
        GameData testGame2 = new GameData(11, null, null,
                "game2", new ChessGame());
        GameData testGame3 = new GameData(12, null, null,
                "game3", new ChessGame());
        gameDataAccess.addGameData(testGame1);
        gameDataAccess.addGameData(testGame2);
        gameDataAccess.addGameData(testGame3);
        gameDataAccess.removeAllGameData();

        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(10));
        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(11));
        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(12));
    }


    @Test
    @Order(4)
    @DisplayName("Delete a game")
    public void RemoveGameSuccess() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        GameData testGame = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame);
        gameDataAccess.removeGameData(10);

        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(10));
    }

    @Test
    @Order(4)
    @DisplayName("Delete game that doesn't exist")
    public void RemoveGameFailure() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        gameDataAccess.removeGameData(10);
        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(10));

    }

    @Test
    @Order(5)
    @DisplayName("Get all games normally")
    public void getAllGamesSuccess() throws DataAccessException {

        gameDataAccess.removeAllGameData();
        GameData testGame1 = new GameData(10, null, null,
                "game1", new ChessGame());
        GameData testGame2 = new GameData(11, null, null,
                "game2", new ChessGame());
        GameData testGame3 = new GameData(12, null, null,
                "game3", new ChessGame());
        gameDataAccess.addGameData(testGame1);
        gameDataAccess.addGameData(testGame2);
        gameDataAccess.addGameData(testGame3);

        ArrayList<GameData> gamesExpected = new ArrayList<>();
        gamesExpected.add(testGame1);
        gamesExpected.add(testGame2);
        gamesExpected.add(testGame3);

        Assertions.assertEquals(gamesExpected, gameDataAccess.getAllGameData());
        gameDataAccess.removeAllGameData();
    }

    @Test
    @Order(6)
    @DisplayName("No games to get")
    public void getAllGamesFailure() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        ArrayList<GameData> emptyData = new ArrayList<>();
        Assertions.assertEquals(gameDataAccess.getAllGameData(), emptyData);
        gameDataAccess.removeAllGameData();
    }

    @Test
    @Order(7)
    @DisplayName("Update game")
    public void updateGameSuccess() throws DataAccessException {

        gameDataAccess.removeAllGameData();
        GameData testGame1 = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame1);
        gameDataAccess.updateGame(ChessGame.TeamColor.WHITE, 10, "white_username");

        Assertions.assertNotEquals(testGame1, gameDataAccess.getGame(10));
        gameDataAccess.removeAllGameData();

    }

    @Test
    @Order(8)
    @DisplayName("Update game but game doesn't exist")
    public void updateGameFailure() throws DataAccessException {

        gameDataAccess.removeAllGameData();
        Assertions.assertThrows(Exception.class, () -> gameDataAccess.updateGame(
                ChessGame.TeamColor.WHITE, 10, "white_username"));
        gameDataAccess.removeAllGameData();

    }

    @Test
    @Order(9)
    @DisplayName("Add new user")
    public void addUserSuccess() throws DataAccessException {
        userDataAccess.removeAllUsers();
        UserData testUser = new UserData("user1", "pswd", "abcd@yahoo.com");
        userDataAccess.addUserData(testUser);
        Assertions.assertEquals(testUser, userDataAccess.getUser("user1"),
                "Returned user was not the same user as was added");
    }

    @Test
    @Order(10)
    @DisplayName("User already exists")
    public void addUserFailure() throws DataAccessException {
        userDataAccess.removeAllUsers();
        UserData testUser = new UserData("user1", "pswd", "abcd@yahoo.com");
        userDataAccess.addUserData(testUser);
        Assertions.assertThrows(DataAccessException.class, () -> userDataAccess.addUserData(testUser));
        userDataAccess.removeAllUsers();
    }

    @Test
    @Order(11)
    @DisplayName("Clear all users")
    public void ClearUserSuccess() throws DataAccessException {
        userService.clearAllData();
        UserData testUser1 = new UserData("user1", "pswd", "abcd@yahoo.com");
        UserData testUser2 = new UserData("user2", "pswd", "abcd@yahoo.com");
        UserData testUser3 = new UserData("user3", "pswd", "abcd@yahoo.com");
        userDataAccess.addUserData(testUser1);
        userDataAccess.addUserData(testUser2);
        userDataAccess.addUserData(testUser3);
        userDataAccess.removeAllUsers();

        Assertions.assertThrows(DataAccessException.class, () -> userDataAccess.getUser("user1"));
        Assertions.assertThrows(DataAccessException.class, () -> userDataAccess.getUser("user2"));
        Assertions.assertThrows(DataAccessException.class, () -> userDataAccess.getUser("user3"));
    }

    @Test
    @Order(12)
    @DisplayName("Add new auth")
    public void addAuthSuccess() throws DataAccessException {
        authDataAccess.removeAllAuthData();
        AuthData testAuth = new AuthData("authToken1", "username1");
        authDataAccess.addAuthData(testAuth);
        Assertions.assertEquals(testAuth, authDataAccess.getAuth("authToken1"),
                "Returned auth was not the same auth as was added");
        authDataAccess.removeAllAuthData();
    }

    @Test
    @Order(13)
    @DisplayName("Auth already exists")
    public void addAuthFailure() throws DataAccessException {
        authDataAccess.removeAllAuthData();
        AuthData testAuth = new AuthData("authToken1", "username1");
        authDataAccess.addAuthData(testAuth);
        Assertions.assertThrows(DataAccessException.class, () -> authDataAccess.addAuthData(testAuth));
        authDataAccess.removeAllAuthData();
    }

    @Test
    @Order(14)
    @DisplayName("Clear all auth")
    public void ClearAuthSuccess() throws DataAccessException {
        AuthData testAuth1 = new AuthData("authToken1", "username1");
        AuthData testAuth2 = new AuthData("authToken2", "username2");
        AuthData testAuth3 = new AuthData("authToken3", "username3");
        authDataAccess.addAuthData(testAuth1);
        authDataAccess.addAuthData(testAuth2);
        authDataAccess.addAuthData(testAuth3);
        authDataAccess.removeAllAuthData();

        Assertions.assertThrows(DataAccessException.class, () -> authDataAccess.getAuth("authToken1"));
        Assertions.assertThrows(DataAccessException.class, () -> authDataAccess.getAuth("authToken2"));
        Assertions.assertThrows(DataAccessException.class, () -> authDataAccess.getAuth("authToken3"));
    }


    @Test
    @Order(15)
    @DisplayName("Delete an auth")
    public void RemoveAuthSuccess() throws DataAccessException {
        gameDataAccess.removeAllGameData();
        GameData testGame = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame);
        gameDataAccess.removeGameData(10);

        Assertions.assertThrows(DataAccessException.class, () -> gameDataAccess.getGame(10));
    }

    @Test
    @Order(16)
    @DisplayName("Delete an auth that doesn't exist")
    public void RemoveAuthFailure() throws DataAccessException {
        authDataAccess.removeAllAuthData();
        authDataAccess.deleteAuth("username1");
        Assertions.assertThrows(DataAccessException.class, () -> authDataAccess.getAuth("username1"));
    }
}