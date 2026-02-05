package service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import passoff.model.TestAuthResult;
import passoff.model.TestUser;
import recordandrequest.RegisterRequest;
import recordandrequest.RegisterResult;

public class ServiceTests {

    private final UserService userService = new UserService();

    @Order(1)
    @DisplayName("Register normally")
    @ParameterizedTest
    @ValueSource(strings = {"basic_username", "second_username", "third_username"})
    public void registerSuccess(String username) {
        //submit register request
        //String username = "basic_username";

        RegisterResult registerResult = userService.register(
                new RegisterRequest(username, "pswd", "abcd@yahoo.com"));

        Assertions.assertEquals(username, registerResult.username(),
                "Response did not have the same username as was registered");
        Assertions.assertNotNull(registerResult.authToken(), "Response did not contain an authentication string");
    }
//
//    @Test
//    @Order(2)
//    @DisplayName("User already exists")
//    public void registerTwice() {
//        //submit register request trying to register existing user
//        TestAuthResult registerResult = serverFacade.register(existingUser);
//
//        assertHttpForbidden(registerResult);
//        assertAuthFieldsMissing(registerResult);
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("Register with bad request")
//    public void registerBadRequest() {
//        //attempt to register a user without a password
//        TestUser registerRequest = new TestUser(newUser.getUsername(), null, newUser.getEmail());
//        TestAuthResult registerResult = serverFacade.register(registerRequest);
//
//        assertHttpBadRequest(registerResult);
//        assertAuthFieldsMissing(registerResult);
//    }
}
