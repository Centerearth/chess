package chess;

import java.util.ArrayList;
import java.util.Collection;

import static chess.HelpCalculator.helpCalculator;

public class BishopMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int i = 1, j = 1; i <= 7 && j <= 7; i++, j++) {
            int newRow = row + i;
            int newCol = col - j;
            if (helpCalculator(newRow, newCol, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1, j = 1; i <= 7 && j <= 7; i++, j++) {
            int newRow = row + i;
            int newCol = col + j;
            if (helpCalculator(newRow, newCol, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1, j = 1; i <= 7 && j <= 7; i++, j++) {
            int newRow = row - i;
            int newCol = col - j;
            if (helpCalculator(newRow, newCol, board, myPosition, validMoves)) {
                break;
            }
        }

        for (int i = 1, j = 1; i <= 7 && j <= 7; i++, j++) {
            int newRow = row - i;
            int newCol = col + j;
            if (helpCalculator(newRow, newCol, board, myPosition, validMoves)) {
                break;
            }
        }
        return validMoves;
    }
}
