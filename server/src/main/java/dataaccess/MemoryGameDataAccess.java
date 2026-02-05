package dataaccess;

import model.GameData;

import java.util.HashMap;

public class MemoryGameDataAccess implements GameDataAccess{
    private static final HashMap<Integer, GameData> allGameData = new HashMap<>();

    public void addGameData(GameData newGame) {
        allGameData.put(newGame.gameID(), newGame);
    }
    public GameData getGame(int gameID) {
        return allGameData.getOrDefault(gameID, null);
    }

    public void removeAllGameData() {
        allGameData.clear();
    }
}
