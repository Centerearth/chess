package dataaccess;

import model.AuthData;
import java.util.HashMap;

public class MemoryAuthDataAccess implements AuthDataAccess {
    private static final HashMap<String, AuthData> AllAuthData = new HashMap<>();

    public void addAuthData(AuthData newAuth) {
        AllAuthData.put(newAuth.authToken(), newAuth);
    }
    public AuthData getAuth(String authToken) {
        return AllAuthData.getOrDefault(authToken, null);
    }
    public void deleteAuth(String authToken) {
        AllAuthData.remove(authToken);
    }
    public void removeAllAuthData() {
        AllAuthData.clear();
    }
}
