package chess;

import java.util.Collection;

/**
 * Calculates the possible moves for a given piece
 */

public interface PieceMovesCalculator {
    Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition);
}
