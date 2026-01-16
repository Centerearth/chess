package chess;

import java.util.ArrayList;

public class HelpCalculator {

    // Note that this mutates validMoves as well as returns a bool
    public static boolean helpCalculator(int newRow, int newCol, ChessBoard board, ChessPosition myPosition, ArrayList<ChessMove> validMoves) {
        if ((1 <= newRow) && (8 >= newRow) && (1 <= newCol) && (8 >= newCol)) {

            ChessPosition adjacentPosition = new ChessPosition(newRow, newCol);
            ChessPiece adjacentPiece = board.getPiece(adjacentPosition);

            ChessGame.TeamColor currentColor = board.getPiece(myPosition).getTeamColor();

            if (adjacentPiece != null && adjacentPiece.getTeamColor() != currentColor) {
                validMoves.add(new ChessMove(myPosition, adjacentPosition, null));
                return true;
            } else if (adjacentPiece != null && adjacentPiece.getTeamColor() == currentColor) {
                return true;
            } else {
                validMoves.add(new ChessMove(myPosition, adjacentPosition, null));
                return false;
            }
        }
        return true;
    }
}
