package chess;

import java.util.ArrayList;
import java.util.Collection;

public class KingMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newRow = row + i;
                int newCol = col + j;
                if (this.inBoundsAndValid(i, j, newRow, newCol)) {
                    ChessPosition adjacentPosition = new ChessPosition(newRow, newCol );
                    ChessPiece adjacentPiece = board.getPiece(adjacentPosition);
                    if (adjacentPiece == null || adjacentPiece.getTeamColor() != board.getPiece(myPosition).getTeamColor()) {
                        validMoves.add(new ChessMove(myPosition, adjacentPosition, null));
                    }
                }
            }
        }
        return validMoves;
    }
    public boolean inBoundsAndValid (int i, int j, int newRow, int newCol) {
        return (!(0 == i && 0 == j) && ((1 <= newRow) && (8 >= newRow) && (1 <= newCol) && (8 >= newCol)));
    }
}
