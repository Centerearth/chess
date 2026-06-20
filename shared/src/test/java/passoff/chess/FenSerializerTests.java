package passoff.chess;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPosition;
import chess.FenSerializer;

import static chess.ChessGame.TeamColor.BLACK;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FenSerializerTests {

    @Test
    @DisplayName("Initial board")
    public void serializeInitialBoard() {
        ChessGame game = new ChessGame();
        Assertions.assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", FenSerializer.toFen(game));

    }

    @Test
    @DisplayName("Missing pieces")
    public void missingPiecesBoard() {
        ChessBoard board = new ChessBoard();
        board.resetBoard();
        board.addPiece(new ChessPosition(8, 2), null);
        board.addPiece(new ChessPosition(8, 3), null);
        ChessGame game = new ChessGame();
        game.setBoard(board);
        Assertions.assertEquals("r2qkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", FenSerializer.toFen(game));
    }

    @Test
    @DisplayName("Missing pieces on the edge")
    public void missingPiecesEdge() {
        ChessBoard board = new ChessBoard();
        board.resetBoard();
        board.addPiece(new ChessPosition(8, 7), null);
        board.addPiece(new ChessPosition(8, 8), null);
        ChessGame game = new ChessGame();
        game.setBoard(board);
        Assertions.assertEquals("rnbqkb2/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQq - 0 1", FenSerializer.toFen(game));
    }

    @Test
    @DisplayName("Initial board black turn")
    public void serializeInitialBoardButBlack() {
        ChessGame game = new ChessGame();
        game.setTeamTurn(ChessGame.TeamColor.BLACK);
        Assertions.assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1", FenSerializer.toFen(game));

    }

    @Test
    @DisplayName("en passant")
    public void enPassantWhite() {
        ChessGame game = new ChessGame();
        game.setEnPassantColumn(5);
        Assertions.assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e3 0 1", FenSerializer.toFen(game));
    }
    
    @Test
    @DisplayName("en passant but black")
    public void enPassantBlack() {
        ChessGame game = new ChessGame();
        game.setEnPassantColumn(5);
        game.setTeamTurn(BLACK);
        Assertions.assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq e6 0 1", FenSerializer.toFen(game));
    }
}
