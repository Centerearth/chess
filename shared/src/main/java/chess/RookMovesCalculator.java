package chess;

import java.util.ArrayList;
import java.util.Collection;

import static chess.HelpCalculator.helpCalculator;

public class RookMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int i = 1; i <= 7; i++) {
            int newRow = row + i;
            if (helpCalculator(newRow, col, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1; i <= 7; i++) {
            int newCol = col + i;
            if (helpCalculator(row, newCol, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1; i <= 7; i++) {
            int newRow = row - i;
            if (helpCalculator(newRow, col, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1; i <= 7; i++) {
            int newCol = col - i;
            if (helpCalculator(row, newCol, board, myPosition, validMoves)) {
                break;
            }
        }
        return validMoves;
    }
}
