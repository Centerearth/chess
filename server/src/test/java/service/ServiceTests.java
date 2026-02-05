package service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import passoff.model.TestAuthResult;
import passoff.model.TestCreateRequest;
import passoff.model.TestUser;
import passoff.server.TestServerFacade;
import recordandrequest.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceTests {
    private final UserService userService = new UserService();
    //add static to declaration if wanted to be persistent

    @Order(1)
    @DisplayName("Register normally")
    @ParameterizedTest
    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
    public void registerSuccess(String username) {
        //submit register request
        //is it a problem that it doesn't save usernames between tests?

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
        assertThrows(AlreadyTakenException.class, () -> {
            userService.register(
                    new RegisterRequest("basic_username", "pswd", "abcd@yahoo.com"));
        });
    }

    @Test
    @Order(3)
    @DisplayName("Register with bad request")
    public void registerBadRequest() {
        //attempt to register a user without a password
        assertThrows(BadRequestException.class, () -> {
            userService.register(
                    new RegisterRequest("", "pswd", "abcd@yahoo.com"));
        });
    }
}
