package chess;

import java.util.ArrayList;
import java.util.Collection;

public class BishopMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        for (int i = 1, j = 1; i <= 7 && j <= 7; i++, j++) {
            int newRow = row + i;
            int newCol = col - j;
            if (!helperAdd(newRow, newCol, board, myPosition, validMoves)) {
                break;
            }
        }
        return validMoves;
    }

    public boolean helperAdd(int newRow, int newCol, ChessBoard board, ChessPosition myPosition, ArrayList<ChessMove> validMoves) {
        if ((1 <= newRow) && (8 >= newRow) && (1 <= newCol) && (8 >= newCol)) {
            ChessPosition adjacentPosition = new ChessPosition(newRow, newCol);
            ChessPiece adjacentPiece = board.getPiece(adjacentPosition);
            ChessGame.TeamColor currentColor = board.getPiece(myPosition).getTeamColor();

            if (adjacentPiece != null && adjacentPiece.getTeamColor() != currentColor) {
                validMoves.add(new ChessMove(myPosition, adjacentPosition, null));
                return false;
            } else if (adjacentPiece != null && adjacentPiece.getTeamColor() == currentColor) {
                return false;
            } else {
                validMoves.add(new ChessMove(myPosition, adjacentPosition, null));
                return true;
            }
        }
        return false;
    }
}
