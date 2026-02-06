package dataaccess;

import chess.ChessGame;
import model.GameData;

import java.util.ArrayList;
import java.util.HashMap;

public class MemoryGameDataAccess implements GameDataAccess{
    private static final HashMap<Integer, GameData> allGameData = new HashMap<>();

    public void addGameData(GameData newGame) {
        allGameData.put(newGame.gameID(), newGame);
    }
    public GameData getGame(int gameID) {
        return allGameData.getOrDefault(gameID, null);
    }

    public void removeGameData(int gameID) {
        allGameData.remove(gameID);
    }

    public void removeAllGameData() {
        allGameData.clear();
    }

    public ArrayList<GameData> getAllGameData() {
        return new ArrayList<>(allGameData.values());
    }
    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) {
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
