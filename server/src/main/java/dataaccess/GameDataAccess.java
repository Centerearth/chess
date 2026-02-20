package dataaccess;

import chess.ChessGame;
import model.GameData;
import java.util.ArrayList;


public interface GameDataAccess {
    public void addGameData (GameData newGame);
    public GameData getGame(int gameID);
    public void removeGameData(int gameID);
    public void removeAllGameData();
    public ArrayList<GameData> getAllGameData();
    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username);
}
