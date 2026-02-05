package dataaccess;

import model.UserData;

public interface UserDataAccess {
    void addUserData(UserData newUser);
    UserData getUser(String username);
    void removeAllUsers();
}
