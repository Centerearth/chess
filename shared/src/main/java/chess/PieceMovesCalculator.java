package chess;

import java.util.Collection;
import java.util.List;

/**
 * Calculates the possible moves for a given piece
 */

public interface PieceMovesCalculator {
    Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition);
}
