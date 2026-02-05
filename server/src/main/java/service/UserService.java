package service;

import model.AuthData;
import recordandrequest.*;

public class UserService {
    public RegisterResult register(RegisterRequest registerRequest) {
        if (registerRequest.username().isBlank() || registerRequest.password().isBlank()
        || registerRequest.email().isBlank()) {
            throw new BadRequestException("The fields cannot be left blank");
        }
        return new RegisterResult("placeholder", new AuthData("placeholder", "placeholder"));
    }
    //public LoginResult login(LoginRequest loginRequest) {}
    //public void logout(LogoutRequest logoutRequest) {}
}
