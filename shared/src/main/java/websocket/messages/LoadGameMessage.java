package websocket.messages;

import chess.ChessGame;

import java.util.Objects;

public class LoadGameMessage extends ServerMessage {
    private final ChessGame game;
    private final Boolean gameOver;

    public LoadGameMessage(ServerMessageType type, ChessGame game,
                           Boolean gameOver) {
        super(type);
        this.game = game;
        this.gameOver = gameOver;
    }


    public ChessGame getGame() {
        return game;
    }

    public Boolean getGameOver() {return gameOver;}

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
