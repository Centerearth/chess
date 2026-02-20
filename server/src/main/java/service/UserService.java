package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;
import java.util.Objects;
import java.util.UUID;

public class UserService {
    private final SQLUserDataAccess userDataAccess = new SQLUserDataAccess();
    private final MemoryAuthDataAccess authDataAccess = new MemoryAuthDataAccess();
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();


    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public boolean authDataExists(String authToken) {
        return (authDataAccess.getAuth(authToken) != null);
    }

    public boolean userDataExists(String username) throws DataAccessException {
        return (userDataAccess.getUser(username) != null);
    }

    public RegisterResult register(RegisterRequest registerRequest) throws DataAccessException {
        if (registerRequest.username().isBlank() || registerRequest.password().isBlank()
        || registerRequest.email().isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        }

        if (userDataAccess.getUser(registerRequest.username()) != null) {
            throw new AlreadyTakenException("Error: This username is already taken.");
        } else {
            UserData newUserData = new UserData(registerRequest.username(),
                    registerRequest.password(), registerRequest.email());
            AuthData newAuthData = new AuthData(generateToken(), registerRequest.username());

            userDataAccess.addUserData(newUserData);
            authDataAccess.addAuthData(newAuthData);
            return new RegisterResult(registerRequest.username(), newAuthData.authToken());
        }

    }

    public LoginResult login(LoginRequest loginRequest) throws FailedLoginException, DataAccessException {
        if (loginRequest.username().isBlank() || loginRequest.password().isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        }

        if (userDataAccess.getUser(loginRequest.username()) == null) {
            throw new FailedLoginException("Error: No user exists with this username");
        } else if (!Objects.equals(userDataAccess.getUser(loginRequest.username()).password(), loginRequest.password())) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            AuthData newAuthData = new AuthData(generateToken(), loginRequest.username());
            authDataAccess.addAuthData(newAuthData);
            return new LoginResult(loginRequest.username(), newAuthData.authToken());
        }
    }

    public void logout(LogoutRequest logoutRequest) throws FailedLoginException {
        String authToken = logoutRequest.authToken();
        if (authToken.isBlank()) {
            throw new BadRequestException("Error: The fields cannot be left blank");
        } else if (authDataAccess.getAuth(authToken) == null ) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            authDataAccess.deleteAuth(authToken);
        }
    }

    public void clearAllData() throws DataAccessException {
        userDataAccess.removeAllUsers();
        authDataAccess.removeAllAuthData();
        gameDataAccess.removeAllGameData();
    }
}
