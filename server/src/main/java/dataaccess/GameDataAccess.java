package dataaccess;

import chess.ChessGame;
import model.GameData;
import java.util.ArrayList;


public interface GameDataAccess {
    public void addGameData (GameData newGame) throws DataAccessException;
    public GameData getGame(int gameID) throws DataAccessException;
    public void removeGameData(int gameID) throws DataAccessException;
    public void removeAllGameData() throws DataAccessException;
    public ArrayList<GameData> getAllGameData() throws DataAccessException;
    public void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException;
}
