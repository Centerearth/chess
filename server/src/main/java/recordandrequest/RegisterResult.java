package recordandrequest;

import model.AuthData;

public record RegisterResult(String username, AuthData authToken) {
}
