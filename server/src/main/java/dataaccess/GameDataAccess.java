package dataaccess;

import chess.ChessGame;
import model.GameData;
import java.util.ArrayList;


public interface GameDataAccess {
    void addGameData (GameData newGame) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    void removeGameData(int gameID) throws DataAccessException;
    void removeAllGameData() throws DataAccessException;
    ArrayList<GameData> getAllGameData() throws DataAccessException;
    void updateGame(ChessGame.TeamColor teamColor, int gameID, String username) throws DataAccessException;
}
