package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import model.UserData;

import java.util.ArrayList;

import static dataaccess.DatabaseManager.*;

public class SQLUserDataAccess implements UserDataAccess{
    public SQLUserDataAccess() {
        createDatabase();
    }

    public void addUserData (UserData newUser) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            var serializer = new Gson();
            var gameJSON = serializer.toJson(newGame);

            try (var preparedStatement = conn.prepareStatement("INSERT INTO game (gameID, gameData) VALUES(?, ?)")) {
                conn.setCatalog("chess");
                preparedStatement.setInt(1, newGame.gameID());
                preparedStatement.setString(2, gameJSON);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error: the game failed to add", e);
        }
    }


    public GameData getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement("SELECT gameData FROM game WHERE gameID=?")) {
                preparedStatement.setInt(1, gameID);
                conn.setCatalog("chess");

                try (var rs = preparedStatement.executeQuery()) {
                    rs.next();
                    var gameDataString = rs.getString("gameData");
                    return new Gson().fromJson(gameDataString, GameData.class);
                }
            }

        } catch (Exception e) {
            throw new DataAccessException("Error: failed to fetch the game", e);
        }
    }


    public void removeAllUsers() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            conn.setCatalog("chess");
            var statement = "TRUNCATE TABLE game";
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error: the game failed to add", e);
        }
    }

}