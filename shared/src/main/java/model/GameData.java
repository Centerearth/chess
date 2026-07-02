package model;

import chess.ChessGame;
import chess.ChessMove;

import java.util.ArrayList;

public record GameData(int gameID, String whiteUsername,
                       String blackUsername, String gameName, ChessGame game,
                       Boolean gameOver,
                       ArrayList<String> moveHistory,
                       ChessMove lastMove,
                       ArrayList<String> positionHistory,
                       Integer aiDifficulty,
                       Long whiteTimeMs,
                       Long blackTimeMs,
                       Long turnStartedAt,
                       String result) {

    public GameData(int gameID, String whiteUsername, String blackUsername,
                    String gameName, ChessGame game, Boolean gameOver) {
        this(gameID, whiteUsername, blackUsername, gameName, game, gameOver,
                new ArrayList<>(), null, new ArrayList<>(), null, null, null, null, null);
    }

    // null-safe accessors for games persisted before these fields existed
    public ArrayList<String> moveHistorySafe() {
        return moveHistory == null ? new ArrayList<>() : moveHistory;
    }

    public ArrayList<String> positionHistorySafe() {
        return positionHistory == null ? new ArrayList<>() : positionHistory;
    }

    public boolean isTimed() {
        return whiteTimeMs != null && blackTimeMs != null;
    }

    public GameData withUsername(ChessGame.TeamColor color, String username) {
        String white = (color == ChessGame.TeamColor.WHITE) ? username : whiteUsername;
        String black = (color == ChessGame.TeamColor.BLACK) ? username : blackUsername;
        return new GameData(gameID, white, black, gameName, game, gameOver, moveHistory,
                lastMove, positionHistory, aiDifficulty, whiteTimeMs, blackTimeMs, turnStartedAt, result);
    }

    public GameData withBoard(ChessGame newGame) {
        return new GameData(gameID, whiteUsername, blackUsername, gameName, newGame, gameOver,
                moveHistory, lastMove, positionHistory, aiDifficulty, whiteTimeMs, blackTimeMs, turnStartedAt, result);
    }

    public GameData withGameOver(String gameResult) {
        return new GameData(gameID, whiteUsername, blackUsername, gameName, game, true,
                moveHistory, lastMove, positionHistory, aiDifficulty, whiteTimeMs, blackTimeMs, turnStartedAt, gameResult);
    }

    public GameData withAiDifficulty(Integer difficulty) {
        return new GameData(gameID, whiteUsername, blackUsername, gameName, game, gameOver,
                moveHistory, lastMove, positionHistory, difficulty, whiteTimeMs, blackTimeMs, turnStartedAt, result);
    }

    public GameData withClocks(Long white, Long black, Long turnStart) {
        return new GameData(gameID, whiteUsername, blackUsername, gameName, game, gameOver,
                moveHistory, lastMove, positionHistory, aiDifficulty, white, black, turnStart, result);
    }

    /** A new snapshot after a move: updated board, appended histories, new clocks. */
    public GameData withMoveApplied(ChessGame newGame, String san, ChessMove move, String fenKey,
                                    Long white, Long black, Long turnStart) {
        ArrayList<String> moves = moveHistorySafe();
        moves.add(san);
        ArrayList<String> positions = positionHistorySafe();
        positions.add(fenKey);
        return new GameData(gameID, whiteUsername, blackUsername, gameName, newGame, gameOver,
                moves, move, positions, aiDifficulty, white, black, turnStart, result);
    }
}
