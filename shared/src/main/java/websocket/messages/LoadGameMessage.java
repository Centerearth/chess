package websocket.messages;

import chess.ChessGame;
import chess.ChessMove;
import model.GameData;

import java.util.ArrayList;
import java.util.Objects;

public class LoadGameMessage extends ServerMessage {
    private final ChessGame game;
    private final Boolean gameOver;
    private ArrayList<String> moveHistory;
    private ChessMove lastMove;
    private Long whiteTimeMs;
    private Long blackTimeMs;
    private Long turnStartedAt;
    private Long serverTime;
    private String result;

    public LoadGameMessage(ServerMessageType type, ChessGame game,
                           Boolean gameOver) {
        super(type);
        this.game = game;
        this.gameOver = gameOver;
    }

    /** Full snapshot including history, last move, clocks, and result. */
    public LoadGameMessage(ServerMessageType type, GameData gameData, long serverTime) {
        super(type);
        this.game = gameData.game();
        this.gameOver = gameData.gameOver();
        this.moveHistory = gameData.moveHistorySafe();
        this.lastMove = gameData.lastMove();
        this.whiteTimeMs = gameData.whiteTimeMs();
        this.blackTimeMs = gameData.blackTimeMs();
        this.turnStartedAt = gameData.turnStartedAt();
        this.serverTime = serverTime;
        this.result = gameData.result();
    }


    public ChessGame getGame() {
        return game;
    }

    public Boolean getGameOver() {return gameOver;}

    public ArrayList<String> getMoveHistory() {return moveHistory;}

    public ChessMove getLastMove() {return lastMove;}

    public String getResult() {return result;}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LoadGameMessage that = (LoadGameMessage) o;
        return Objects.equals(getGame(), that.getGame());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getGame());
    }
}
