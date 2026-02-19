package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static dataaccess.DatabaseManager.*;

public class SQLGameDataAccess implements GameDataAccess{
    public SQLGameDataAccess() {
        try {createDatabase();} catch (DataAccessException e) {
            System.out.println(e);
        }
    }
    public void addGameData (GameData newGame) {
        try (var conn = DatabaseManager.getConnection()) {
            var serializer = new Gson();
            var gameJSON = serializer.toJson(newGame);
            System.out.println("JSON: " + gameJSON);
            try (var preparedStatement = conn.prepareStatement("INSERT INTO game (gameID, gameData) VALUES(?, ?)")) {
                conn.setCatalog("chess");
                preparedStatement.setInt(1, newGame.gameID());
                preparedStatement.setString(2, gameJSON);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    public void addGameData(GameData newGame) throws DataAccessException, SQLException {
//        try (var conn = DatabaseManager.getConnection()) {
//
//        }
//    }

    public GameData getGame(int gameID) {
        return null;
    }
    public void removeGameData(int gameID) {
    }

    public void removeAllGameData() {
        try (var conn = DatabaseManager.getConnection(); ) {

            conn.setCatalog("chess");
            var statement = "TRUNCATE TABLE game";
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public ArrayList<GameData> getAllGameData() {
        return null;
   }
    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) {
    }

//    private static final HashMap<Integer, GameData> ALLGAMEDATA = new HashMap<>();
//
//    public void addGameData(GameData newGame) {
//        ALLGAMEDATA.put(newGame.gameID(), newGame);
//    }
//    public GameData getGame(int gameID) {
//        return ALLGAMEDATA.getOrDefault(gameID, null);
//    }
//
//    public void removeGameData(int gameID) {
//        ALLGAMEDATA.remove(gameID);
//    }
//
//    public void removeAllGameData() {
//        ALLGAMEDATA.clear();
//    }
//
//    public ArrayList<GameData> getAllGameData() {
//        return new ArrayList<>(ALLGAMEDATA.values());
//    }
//    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) {
//        GameData oldGame = getGame(gameID);
//        GameData updatedGame;
//        if (teamColor == ChessGame.TeamColor.WHITE) {
//            updatedGame = new GameData(gameID,
//                    username, oldGame.blackUsername(), oldGame.gameName(), oldGame.game());
//        } else {
//            updatedGame = new GameData(gameID,
//                    oldGame.whiteUsername(), username, oldGame.gameName(), oldGame.game());
//        }
//        removeGameData(gameID);
//        addGameData(updatedGame);
//    }
}
