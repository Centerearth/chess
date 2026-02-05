package dataaccess;

import model.AuthData;
import model.UserData;

import java.util.ArrayList;
import java.util.HashMap;

public class MemoryAuthDataAccess implements AuthDataAccess {
    private HashMap<String, AuthData> allAuthData;

    public void addAuthData(AuthData newAuth) {
        allAuthData.put(newAuth.authToken(), newAuth);
    }
    public AuthData getAuth(String authToken) {
        return allAuthData.getOrDefault(authToken, null);
    }
    public void deleteAuth(String authToken) {
        allAuthData.remove(authToken);
    }
    public void removeAllAuthData() {
        allAuthData.clear();
    }
}
