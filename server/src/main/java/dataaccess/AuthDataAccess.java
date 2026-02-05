package dataaccess;

import model.AuthData;

public interface AuthDataAccess {
    void addAuthData(AuthData newAuth);
    AuthData getAuth(String authToken);
    void deleteAuth(String authToken);
    void removeAllAuthData();
}
