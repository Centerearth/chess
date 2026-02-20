package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import java.util.ArrayList;

import static dataaccess.DatabaseManager.*;

public class SQLGameDataAccess implements GameDataAccess{
    public SQLGameDataAccess() throws DataAccessException {
        createDatabase();
    }

    public void addGameData (GameData newGame) throws DataAccessException {
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


    public GameData getGame(int gameID) throws DataAccessException {
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
            return null;
        }
    }



    public void removeGameData(int gameID) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            conn.setCatalog("chess");
            try (var preparedStatement = conn.prepareStatement("DELETE FROM game WHERE gameID=?")) {
                preparedStatement.setInt(1, gameID);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            throw new DataAccessException("Error: failed to remove game", e);
        }
    }

    public void removeAllGameData() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            conn.setCatalog("chess");
            var statement = "TRUNCATE TABLE game";
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error: failed to remove games", e);
        }
    }

    public ArrayList<GameData> getAllGameData() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var preparedStatement = conn.prepareStatement("SELECT gameData FROM game")) {
                conn.setCatalog("chess");

                try (var rs = preparedStatement.executeQuery()) {
                    ArrayList<GameData> allGames = new ArrayList<>();
                    var serializer = new Gson();
                    while (rs.next()) {
                        var gameDataString = rs.getString("gameData");
                        allGames.add(serializer.fromJson(gameDataString, GameData.class));
                    }
                    return allGames;
                }
            }

        } catch (Exception e) {
            throw new DataAccessException("Error: failed to fetch all game data", e);
        }
   }
    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException {

        GameData oldGame = getGame(gameID);
        GameData updatedGame;
        if (teamColor == ChessGame.TeamColor.WHITE) {
            updatedGame = new GameData(gameID,
                    username, oldGame.blackUsername(), oldGame.gameName(), oldGame.game());
        } else {
            updatedGame = new GameData(gameID,
                    oldGame.whiteUsername(), username, oldGame.gameName(), oldGame.game());
        }
        removeGameData(gameID);
        addGameData(updatedGame);
    }
}
