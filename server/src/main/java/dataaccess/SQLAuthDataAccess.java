package dataaccess;

import com.google.gson.Gson;
import model.AuthData;

import static dataaccess.DatabaseManager.*;

public class SQLAuthDataAccess implements AuthDataAccess {
    public SQLAuthDataAccess() throws DataAccessException {
        createDatabase();
    }

    public void addAuthData (AuthData newAuth) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            var serializer = new Gson();
            var authJSON = serializer.toJson(newAuth);

            try (var preparedStatement = conn.prepareStatement("INSERT INTO auth (authToken, authData) VALUES(?, ?)")) {
                conn.setCatalog(databaseName);
                preparedStatement.setString(1, newAuth.authToken());
                preparedStatement.setString(2, authJSON);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error: the auth failed to add", e);
        }
    }


    public AuthData getAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement("SELECT authData FROM auth WHERE authToken=?")) {
                preparedStatement.setString(1, authToken);
                conn.setCatalog(databaseName);

                try (var rs = preparedStatement.executeQuery()) {
                    rs.next();
                    var authDataString = rs.getString("authData");
                    return new Gson().fromJson(authDataString, AuthData.class);
                }
            }

        } catch (DataAccessException e) {
            throw new DataAccessException("Error: auth Token could not be retrieved");
        } catch (Exception e) {
            return null;
        }
    }





    public void deleteAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            conn.setCatalog(databaseName);
            try (var preparedStatement = conn.prepareStatement("DELETE FROM auth WHERE authToken=?")) {
                preparedStatement.setString(1, authToken);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            throw new DataAccessException("Error: failed to remove the auth data", e);
        }
    }

    public void removeAllAuthData() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            conn.setCatalog(databaseName);
            var statement = "TRUNCATE TABLE auth";
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error: the auth data was not deleted", e);
        }
    }

}
