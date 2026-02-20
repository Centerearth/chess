package dataaccess;

import com.google.gson.Gson;
import model.UserData;


import static dataaccess.DatabaseManager.*;

public class SQLUserDataAccess implements UserDataAccess{
    public SQLUserDataAccess() throws DataAccessException {
        createDatabase();
    }

    public void addUserData (UserData newUser) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            var serializer = new Gson();
            var userJSON = serializer.toJson(newUser);

            try (var preparedStatement = conn.prepareStatement("INSERT INTO user (username, userData) VALUES(?, ?)")) {
                conn.setCatalog(databaseName);
                preparedStatement.setString(1, newUser.username());
                preparedStatement.setString(2, userJSON);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error: the user failed to add", e);
        }
    }


    public UserData getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement("SELECT userData FROM user WHERE username=?")) {
                preparedStatement.setString(1, username);
                conn.setCatalog("chess");

                try (var rs = preparedStatement.executeQuery()) {
                    rs.next();
                    var userDataString = rs.getString("userData");
                    return new Gson().fromJson(userDataString, UserData.class);
                }
            }

        } catch (DataAccessException e) {
            throw new DataAccessException("Error: user could not be retrieved");
        } catch (Exception e) {
            return null;
        }
    }


    public void removeAllUsers() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            conn.setCatalog(databaseName);
            var statement = "TRUNCATE TABLE user";
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error: failed to remove all users", e);
        }
    }

}