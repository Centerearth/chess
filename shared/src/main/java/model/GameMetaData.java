package model;

import chess.ChessGame;

public record GameMetaData(int gameID, String whiteUsername,
                       String blackUsername, String gameName) {
}