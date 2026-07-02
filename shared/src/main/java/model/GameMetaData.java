package model;

public record GameMetaData(int gameID, String whiteUsername,
                           String blackUsername, String gameName,
                           Boolean gameOver) {
    public GameMetaData(int gameID, String whiteUsername, String blackUsername, String gameName) {
        this(gameID, whiteUsername, blackUsername, gameName, false);
    }
}
