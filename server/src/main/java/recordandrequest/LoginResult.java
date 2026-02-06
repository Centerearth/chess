package recordandrequest;

import model.AuthData;
//Do I want AuthData or a string?
public record LoginResult(String username, AuthData authToken) {
}
