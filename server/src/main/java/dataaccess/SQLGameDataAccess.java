package dataaccess;

import chess.ChessGame;
import model.GameData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static dataaccess.DatabaseManager.*;

public class SQLGameDataAccess implements GameDataAccess{
    public SQLGameDataAccess() throws DataAccessException {
        createDatabase();
    }

    public void addGameData(GameData newGame) throws DataAccessException, SQLException {
        try (var conn = DatabaseManager.getConnection()) {
            //do something
        }
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
