package dataaccess;

import model.AuthData;
import java.util.HashMap;

public class MemoryAuthDataAccess implements AuthDataAccess {
    private static final HashMap<String, AuthData> AllAUTHDATA = new HashMap<>();

    public void addAuthData(AuthData newAuth) {
        AllAUTHDATA.put(newAuth.authToken(), newAuth);
    }
    public AuthData getAuth(String authToken) {
        return AllAUTHDATA.getOrDefault(authToken, null);
    }
    public void deleteAuth(String authToken) {
        AllAUTHDATA.remove(authToken);
    }
    public void removeAllAuthData() {
        AllAUTHDATA.clear();
    }
}
