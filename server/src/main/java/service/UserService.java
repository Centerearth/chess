package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import recordandrequest.*;

import javax.security.auth.login.FailedLoginException;
import java.util.UUID;

public class UserService {
    private final SQLUserDataAccess userDataAccess = new SQLUserDataAccess();
    private final SQLAuthDataAccess authDataAccess = new SQLAuthDataAccess();
    private final SQLGameDataAccess gameDataAccess = new SQLGameDataAccess();

    public UserService() throws DataAccessException {
    }


    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public boolean authDataExists(String authToken) {
        return (authDataAccess.getAuth(authToken) != null);
    }

    public boolean userDataExists(String username) {
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
            String hashedPassword = BCrypt.hashpw(registerRequest.password(), BCrypt.gensalt());
            UserData newUserData = new UserData(registerRequest.username(),
                    hashedPassword, registerRequest.email());
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
        } else if (!BCrypt.checkpw(loginRequest.password(), userDataAccess.getUser(loginRequest.username()).password())) {
            throw new FailedLoginException("Error: unauthorized");
        } else {
            AuthData newAuthData = new AuthData(generateToken(), loginRequest.username());
            authDataAccess.addAuthData(newAuthData);
            return new LoginResult(loginRequest.username(), newAuthData.authToken());
        }
    }

    public void logout(LogoutRequest logoutRequest) throws FailedLoginException, DataAccessException {
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
