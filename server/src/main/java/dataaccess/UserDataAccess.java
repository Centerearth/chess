package dataaccess;

import model.UserData;

public interface UserDataAccess {
    void addUserData(UserData newUser) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    void removeAllUsers() throws DataAccessException;
}
