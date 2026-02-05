package dataaccess;

import model.AuthData;
import java.util.HashMap;

public class MemoryAuthDataAccess implements AuthDataAccess {
    private static final HashMap<String, AuthData> allAuthData = new HashMap<>();

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
