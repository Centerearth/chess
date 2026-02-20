package dataaccess;

import model.AuthData;

public interface AuthDataAccess {
    void addAuthData(AuthData newAuth) throws DataAccessException;
    AuthData getAuth(String authToken)  throws DataAccessException;
    void deleteAuth(String authToken)  throws DataAccessException;
    void removeAllAuthData() throws DataAccessException;
}
