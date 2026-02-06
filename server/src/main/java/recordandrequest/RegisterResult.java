package recordandrequest;

import model.AuthData;
//Do I want the authToken or the string?
public record RegisterResult(String username, AuthData authToken) {
}
