package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import recordandrequest.*;
import service.GameService;
import service.UserService;

import javax.security.auth.login.FailedLoginException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataAccessTests {
    private final UserService userService = new UserService();
    private final GameService gameService = new GameService();
    private final MemoryUserDataAccess userDataAccess = new MemoryUserDataAccess();
    private final MemoryAuthDataAccess authDataAccess = new MemoryAuthDataAccess();
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();

    @Test
    @Order(1)
    @DisplayName("Add new game")
    public void addGameSuccess() {
        gameDataAccess.removeAllGameData();
        GameData testGame = new GameData(10, null, null,
                "game1", new ChessGame());
        gameDataAccess.addGameData(testGame);
        Assertions.assertEquals(testGame, gameDataAccess.getGame(10),
                "Response was not the same game as was added");
        gameDataAccess.removeAllGameData();
    }

//    @Test
//    @Order(2)
//    @DisplayName("User already exists")
//    public void registerTwice() {
//        userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
//        //submit register request trying to register existing user
//        assertThrows(AlreadyTakenException.class, () -> userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com")));
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("Register with bad request")
//    public void registerBadRequest() {
//        //attempt to register a user without a password
//        assertThrows(BadRequestException.class, () -> userService.register(
//                    new RegisterRequest("", "pswd", "abcd@yahoo.com")));
//    }
//
//
//
//    @Order(4)
//    @DisplayName("Login normally")
//    @ParameterizedTest
//    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
//    public void loginSuccess(String username) throws FailedLoginException {
//        userService.register(new RegisterRequest(username, "pswd", "abcd@yahoo.com"));
//        //submit login request
//
//        LoginResult loginResult = userService.login(
//                new LoginRequest(username, "pswd"));
//
//        Assertions.assertEquals(username, loginResult.username(),
//                "Response did not have the same username as was used for login");
//        Assertions.assertNotNull(loginResult.authToken(), "Response did not contain an authentication string");
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("Login - User is unauthorized")
//    public void loginUnauthorized() {
//        userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
//        //submit login request with wrong password
//        assertThrows(FailedLoginException.class, () -> userService.login(
//                new LoginRequest("basic_username", "1234")));
//    }
//
//    @Test
//    @Order(6)
//    @DisplayName("Login with bad request")
//    public void loginBadRequest() {
//        //attempt to log in a user without a password
//        assertThrows(BadRequestException.class, () -> userService.login(
//                new LoginRequest("hello", "")));
//    }
//
//    @Order(7)
//    @DisplayName("Logout normally")
//    @ParameterizedTest
//    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
//    public void logoutSuccess(String username) throws FailedLoginException {
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest(username, "pswd", "abcd@yahoo.com"));
//        //submit logout request
//
//        userService.logout(new LogoutRequest(registerResult.authToken()));
//
//        Assertions.assertFalse(userService.authDataExists(registerResult.authToken()),
//                "Logout did complete successfully and the token was not deleted");
//    }
//
//    @Test
//    @Order(8)
//    @DisplayName("Logout - User is unauthorized")
//    public void logoutUnauthorized() {
//        userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
//        //submit logout request with wrong authToken
//        assertThrows(FailedLoginException.class, () -> userService.logout(new LogoutRequest("fake_token")));
//    }
//
//
//    @Order(9)
//    @DisplayName("Create game normally")
//    @ParameterizedTest
//    @ValueSource(strings = {"first_name", "second_name", "third_name"})
//    public void createGameSuccess(String gameName) throws FailedLoginException {
//        //submit create request
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken = registerResult.authToken();
//        CreateGameResult createGameResult = gameService.createGame(
//                new CreateGameRequest(authToken, gameName));
//
//        Assertions.assertNotNull(createGameResult, "Nothing was created");
//    }
//
//    @Test
//    @Order(10)
//    @DisplayName("Create game with bad request")
//    public void createGameBadRequest() {
//        //attempt to create a game without a name
//        RegisterResult registerResult =  userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
//        assertThrows(BadRequestException.class, () -> gameService.createGame(
//                new CreateGameRequest(registerResult.authToken(), "")));
//    }
//
//    @Test
//    @Order(11)
//    @DisplayName("Create Game - User is unauthorized")
//    public void createGameUnauthorized() {
//        userService.register(
//                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
//        //submit logout request with wrong authToken
//        assertThrows(FailedLoginException.class,
//                () -> gameService.createGame(new CreateGameRequest("", "new_game")));
//    }
//
//    @Test
//    @Order(12)
//    @DisplayName("Clear everything")
//    public void clearData() throws FailedLoginException {
//
//        RegisterResult registerResult1 = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//        RegisterResult registerResult2 = userService.register(
//                new RegisterRequest("second_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken1 = registerResult1.authToken();
//        String authToken2 = registerResult2.authToken();
//
//
//        CreateGameResult createGameResult1 = gameService.createGame(
//                new CreateGameRequest(authToken1, "first_game"));
//        CreateGameResult createGameResult2 = gameService.createGame(
//                new CreateGameRequest(authToken2, "second_game"));
//
//        userService.clearAllData();
//
//        Assertions.assertFalse(userService.authDataExists(authToken1), "authToken1 was not deleted");
//        Assertions.assertFalse(userService.authDataExists(authToken2), "authToken2 was not deleted");
//        Assertions.assertFalse(userService.userDataExists(registerResult1.username()), "user1 was not deleted");
//        Assertions.assertFalse(userService.userDataExists(registerResult2.username()), "user2 was not deleted");
//        Assertions.assertFalse(gameService.gameDataExists(createGameResult1.gameID()), "game1 was not deleted");
//        Assertions.assertFalse(gameService.gameDataExists(createGameResult2.gameID()), "game2 was not deleted");
//
//    }
//
//    @Test
//    @Order(13)
//    @DisplayName("List games normally")
//    public void listGameSuccess() throws FailedLoginException {
//        //submit list request
//        userService.clearAllData();
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken = registerResult.authToken();
//
//        gameService.createGame(
//                new CreateGameRequest(authToken, "first_game"));
//        gameService.createGame(
//                new CreateGameRequest(authToken, "second_game"));
//
//        ListGameResult listGameResult = gameService.listAllGameMetaData(new ListGameRequest(authToken));
//        Assertions.assertEquals(2,listGameResult.games().size());
//    }
//
//    @Test
//    @Order(14)
//    @DisplayName("List Games - User is unauthorized")
//    public void listGameUnauthorized() throws FailedLoginException {
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken = registerResult.authToken();
//
//        gameService.createGame(
//                new CreateGameRequest(authToken, "first_game"));
//        gameService.createGame(
//                new CreateGameRequest(authToken, "second_game"));
//
//        //submit list request with wrong authToken
//        assertThrows(FailedLoginException.class,
//                () -> gameService.listAllGameMetaData(new ListGameRequest("")));
//    }
//
//    @Test
//    @Order(15)
//    @DisplayName("Join game normally")
//    public void joinGameSuccess() throws FailedLoginException {
//
//        //submit join request
//        userService.clearAllData();
//        RegisterResult registerResult1 = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//        RegisterResult registerResult2 = userService.register(
//                new RegisterRequest("second_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken1 = registerResult1.authToken();
//        String authToken2 = registerResult2.authToken();
//
//        CreateGameResult createGameResult = gameService.createGame(
//                new CreateGameRequest(authToken1, "first_game"));
//        int gameID = createGameResult.gameID();
//
//        gameService.joinGame(new JoinGameRequest(authToken1, ChessGame.TeamColor.WHITE, gameID));
//        gameService.joinGame(new JoinGameRequest(authToken2, ChessGame.TeamColor.BLACK, gameID));
//        Assertions.assertFalse(gameService.getGame(gameID).whiteUsername().isBlank());
//        Assertions.assertFalse(gameService.getGame(gameID).blackUsername().isBlank());
//    }
//
//    @Test
//    @Order(16)
//    @DisplayName("Join Game - User is unauthorized")
//    public void joinGameUnauthorized() throws FailedLoginException {
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken = registerResult.authToken();
//
//        CreateGameResult createGameResult = gameService.createGame(
//                new CreateGameRequest(authToken, "first_game"));
//        int gameID = createGameResult.gameID();
//
//        //submit list request with wrong authToken
//        assertThrows(FailedLoginException.class,
//                () -> gameService.joinGame(new JoinGameRequest("", ChessGame.TeamColor.WHITE, gameID)));
//    }
//
//    @Test
//    @Order(17)
//    @DisplayName("Join Game - Place is already taken")
//    public void joinTakenGame() throws FailedLoginException {
//        RegisterResult registerResult = userService.register(
//                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));
//
//        String authToken = registerResult.authToken();
//
//        CreateGameResult createGameResult = gameService.createGame(
//                new CreateGameRequest(authToken, "first_game"));
//        int gameID = createGameResult.gameID();
//
//        gameService.joinGame(new JoinGameRequest(authToken, ChessGame.TeamColor.WHITE, gameID));
//        //submit join request again
//        assertThrows(AlreadyTakenException.class,
//                () -> gameService.joinGame(new JoinGameRequest(authToken, ChessGame.TeamColor.WHITE, gameID)));
//    }


}
