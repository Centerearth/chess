package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import recordandrequest.*;

import java.util.UUID;

public class UserService {
    private final MemoryUserDataAccess userDataAccess = new MemoryUserDataAccess();
    private final MemoryAuthDataAccess authDataAccess = new MemoryAuthDataAccess();

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public RegisterResult register(RegisterRequest registerRequest) {
        if (registerRequest.username().isBlank() || registerRequest.password().isBlank()
        || registerRequest.email().isBlank()) {
            throw new BadRequestException("The fields cannot be left blank");
        }

        if (userDataAccess.getUser(registerRequest.username()) != null) {
            throw new AlreadyTakenException("This username is already taken.");
        } else {
            UserData newUserData = new UserData(registerRequest.username(),
                    registerRequest.password(), registerRequest.email());
            AuthData newAuthData = new AuthData(generateToken(), registerRequest.username());

            userDataAccess.addUserData(newUserData);
            authDataAccess.addAuthData(newAuthData);
            return new RegisterResult(registerRequest.username(), newAuthData);
        }

    }

    //public LoginResult login(LoginRequest loginRequest) {}
    //public void logout(LogoutRequest logoutRequest) {}
}
