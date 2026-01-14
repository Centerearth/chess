package chess;

import java.util.ArrayList;
import java.util.Collection;

import static chess.HelpCalculator.helpCalculator;

public class KnightMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        helpCalculator(row+2, col+1, board, myPosition, validMoves);
        helpCalculator(row+2, col-1, board, myPosition, validMoves);
        helpCalculator(row-2, col+1, board, myPosition, validMoves);
        helpCalculator(row-2, col-1, board, myPosition, validMoves);
        helpCalculator(row+1, col+2, board, myPosition, validMoves);
        helpCalculator(row-1, col+2, board, myPosition, validMoves);
        helpCalculator(row+1, col-2, board, myPosition, validMoves);
        helpCalculator(row-1, col-2, board, myPosition, validMoves);

        return validMoves;
    }
}
