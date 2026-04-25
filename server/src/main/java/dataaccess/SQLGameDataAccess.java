package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import java.util.ArrayList;
import java.util.Objects;

import static dataaccess.DatabaseManager.*;

public class SQLGameDataAccess implements GameDataAccess{
    public SQLGameDataAccess() throws DataAccessException {
        createDatabase();
    }

    public void addGameData (GameData newGame) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {

            var serializer = new Gson();
            var gameJSON = serializer.toJson(newGame);

            try (var preparedStatement = conn.prepareStatement(
                    "INSERT INTO game (gameID, gameData) VALUES(?, ?)")) {
                conn.setCatalog(databaseName);
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
                conn.setCatalog(databaseName);

                try (var rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        var gameDataString = rs.getString("gameData");
                        return new Gson().fromJson(gameDataString, GameData.class);
                    }
                    return null;
                }
            }

        } catch (DataAccessException e) {
            throw new DataAccessException("Error: game could not be retrieved");
        } catch (Exception e) {
            return null;
        }
    }



    public void removeGameData(int gameID) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            conn.setCatalog(databaseName);
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

            conn.setCatalog(databaseName);
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
                conn.setCatalog(databaseName);

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
    private void executeUpdate(int gameID, GameData newGame) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            conn.setCatalog(databaseName);
            try (var preparedStatement = conn.prepareStatement("UPDATE game SET gameData = ? WHERE gameID = ?")) {
                preparedStatement.setString(1, new Gson().toJson(newGame));
                preparedStatement.setInt(2, gameID);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error: failed to update game", e);
        }
    }

    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException {
        GameData oldGame = getGame(gameID);
        GameData newGame;
        if (teamColor == ChessGame.TeamColor.BLACK) {
            newGame = new GameData(gameID,
                    oldGame.whiteUsername(), username, oldGame.gameName(), oldGame.game(),
                    oldGame.gameOver());
        } else {
            newGame = new GameData(gameID,
                    username, oldGame.blackUsername(), oldGame.gameName(), oldGame.game(),
                    oldGame.gameOver());
        }
        executeUpdate(gameID, newGame);
    }

    public void updateBoard(int gameID, ChessGame game) throws DataAccessException {
        GameData oldGame = getGame(gameID);
        GameData newGame = new GameData(gameID, oldGame.whiteUsername(), oldGame.blackUsername(), oldGame.gameName(),
                game, oldGame.gameOver());
        executeUpdate(gameID, newGame);
    }


    public void updateGameWin(int gameID) throws DataAccessException {
        GameData oldGame = getGame(gameID);
        GameData newGame = new GameData(gameID, oldGame.whiteUsername(), oldGame.blackUsername(), oldGame.gameName(),
                oldGame.game(), true);
        executeUpdate(gameID, newGame);
    }

    public boolean isGameWon(int gameID) throws DataAccessException {
        return getGame(gameID).gameOver();
    }

    public String giveColorGivenUsername(String username, int gameID) throws DataAccessException {
        GameData gameData = getGame(gameID);
        if (Objects.equals(gameData.whiteUsername(), username)) {
            return "WHITE";
        } else if (Objects.equals(gameData.blackUsername(), username)) {
            return "BLACK";
        }
        return null;
    }
}
