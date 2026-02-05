package dataaccess;

import model.UserData;

import java.util.HashMap;

public class MemoryUserDataAccess implements UserDataAccess{
    private HashMap<String, UserData> allUsers;

    public void addUserData(UserData newUser) {
        allUsers.put(newUser.username(), newUser);
    }
    public UserData getUser(String username) {
        return allUsers.getOrDefault(username, null);
    }
    public void removeAllUsers() {
        allUsers.clear();
    }
}
