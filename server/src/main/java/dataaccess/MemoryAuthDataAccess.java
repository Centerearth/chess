package dataaccess;

import model.AuthData;
import java.util.HashMap;

public class MemoryAuthDataAccess implements AuthDataAccess {
    private static final HashMap<String, AuthData> ALLAUTHDATA = new HashMap<>();

    public void addAuthData(AuthData newAuth) {
        ALLAUTHDATA.put(newAuth.authToken(), newAuth);
    }
    public AuthData getAuth(String authToken) {
        return ALLAUTHDATA.getOrDefault(authToken, null);
    }
    public void deleteAuth(String authToken) {
        ALLAUTHDATA.remove(authToken);
    }
    public void removeAllAuthData() {
        ALLAUTHDATA.clear();
    }
}
