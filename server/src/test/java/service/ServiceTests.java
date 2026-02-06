package service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceTests {
    private final UserService userService = new UserService();
    private final GameService gameService = new GameService();
    //add static to declaration if wanted to be persistent

    @Order(1)
    @DisplayName("Register normally")
    @ParameterizedTest
    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
    public void registerSuccess(String username) {
        //submit register request

        RegisterResult registerResult = userService.register(
                new RegisterRequest(username, "pswd", "abcd@yahoo.com"));

        Assertions.assertEquals(username, registerResult.username(),
                "Response did not have the same username as was registered");
        Assertions.assertNotNull(registerResult.authToken(), "Response did not contain an authentication string");
    }

    @Test
    @Order(2)
    @DisplayName("User already exists")
    public void registerTwice() {
        userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        //submit register request trying to register existing user
        assertThrows(AlreadyTakenException.class, () -> userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com")));
    }

    @Test
    @Order(3)
    @DisplayName("Register with bad request")
    public void registerBadRequest() {
        //attempt to register a user without a password
        assertThrows(BadRequestException.class, () -> userService.register(
                    new RegisterRequest("", "pswd", "abcd@yahoo.com")));
    }



    @Order(4)
    @DisplayName("Login normally")
    @ParameterizedTest
    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
    public void loginSuccess(String username) throws FailedLoginException {
        userService.register(new RegisterRequest(username, "pswd", "abcd@yahoo.com"));
        //submit login request

        LoginResult loginResult = userService.login(
                new LoginRequest(username, "pswd"));

        Assertions.assertEquals(username, loginResult.username(),
                "Response did not have the same username as was used for login");
        Assertions.assertNotNull(loginResult.authToken(), "Response did not contain an authentication string");
    }

    @Test
    @Order(5)
    @DisplayName("Login - User is unauthorized")
    public void loginUnauthorized() {
        userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        //submit login request with wrong password
        assertThrows(FailedLoginException.class, () -> userService.login(
                new LoginRequest("basic_username", "1234")));
    }

    @Test
    @Order(6)
    @DisplayName("Login with bad request")
    public void loginBadRequest() {
        //attempt to log in a user without a password
        assertThrows(BadRequestException.class, () -> userService.login(
                new LoginRequest("hello", "")));
    }

    @Order(7)
    @DisplayName("Logout normally")
    @ParameterizedTest
    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
    public void logoutSuccess(String username) throws FailedLoginException {
        RegisterResult registerResult = userService.register(
                new RegisterRequest(username, "pswd", "abcd@yahoo.com"));
        //submit logout request

        userService.logout(new LogoutRequest(registerResult.authToken().authToken()));

        Assertions.assertFalse(userService.authDataExists(registerResult.authToken().authToken()),
                "Logout did complete successfully and the token was not deleted");
    }

    @Test
    @Order(8)
    @DisplayName("Logout - User is unauthorized")
    public void logoutUnauthorized() {
        userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        //submit logout request with wrong authToken
        assertThrows(FailedLoginException.class, () -> userService.logout(new LogoutRequest("fake_token")));
    }


    @Order(9)
    @DisplayName("Create game normally")
    @ParameterizedTest
    @ValueSource(strings = {"first_name", "second_name", "third_name"})
    public void createGameSuccess(String gameID) throws FailedLoginException {
        //submit create request
        RegisterResult registerResult = userService.register(
                new RegisterRequest("first_username", "pswd", "abcd@yahoo.com"));

        String authToken = registerResult.authToken().authToken();
        CreateGameResult createGameResult = gameService.createGame(
                new CreateGameRequest(authToken, gameID));

        Assertions.assertNotNull(createGameResult, "Nothing was created");
    }

    @Test
    @Order(10)
    @DisplayName("Create game with bad request")
    public void createGameBadRequest() {
        //attempt to create a game without a name
        RegisterResult registerResult =  userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        assertThrows(BadRequestException.class, () -> gameService.createGame(
                new CreateGameRequest(registerResult.authToken().authToken(), "")));
    }

    @Test
    @Order(11)
    @DisplayName("Create Game - User is unauthorized")
    public void createGameUnauthorized() {
        userService.register(
                new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        //submit logout request with wrong authToken
        assertThrows(FailedLoginException.class,
                () -> gameService.createGame(new CreateGameRequest("", "new_game")));
    }

    //write a test to test persistency of the hash maps
}
