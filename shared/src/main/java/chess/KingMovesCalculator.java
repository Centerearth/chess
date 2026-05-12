package chess;

import static chess.ChessGame.TeamColor.WHITE;
import static chess.ChessPiece.PieceType.ROOK;

import java.util.ArrayList;
import java.util.Collection;

public class KingMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        ChessPiece currentKing = board.getPiece(myPosition);

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

        if (currentKing.getTotalMoves() == 0) {
            if (currentKing.getTeamColor() == WHITE) {
                ChessPiece leftPiece = board.getPiece(new ChessPosition(1,1));
                if (leftPiece != null && leftPiece.getPieceType() == ROOK && leftPiece.getTotalMoves() == 0 &&
                rangeEmpty(row, 2, 4, board)) {
                    ChessMove castlingLeft = new ChessMove(myPosition, new ChessPosition(1, 3), null);
                    castlingLeft.setCastling(true);
                    validMoves.add(castlingLeft);
                }
                ChessPiece rightPiece = board.getPiece(new ChessPosition(1,8));
                if (rightPiece != null && rightPiece.getPieceType() == ROOK && rightPiece.getTotalMoves() == 0 &&
                rangeEmpty(row, 6, 7, board)) {
                    ChessMove castlingRight= new ChessMove(myPosition, new ChessPosition(1, 7), null);
                    castlingRight.setCastling(true);
                    validMoves.add(castlingRight);
                }
            } else {
                ChessPiece leftPiece = board.getPiece(new ChessPosition(8,1));
                if (leftPiece != null && leftPiece.getPieceType() == ROOK && leftPiece.getTotalMoves() == 0 &&
                rangeEmpty(row, 2, 4, board)) {
                    ChessMove castlingLeft = new ChessMove(myPosition, new ChessPosition(8, 3), null);
                    castlingLeft.setCastling(true);
                    validMoves.add(castlingLeft);
                }
                ChessPiece rightPiece = board.getPiece(new ChessPosition(8,8));
                if (rightPiece != null && rightPiece.getPieceType() == ROOK && rightPiece.getTotalMoves() == 0 &&
                rangeEmpty(row, 6, 7, board)) {
                    ChessMove castlingRight= new ChessMove(myPosition, new ChessPosition(8, 7), null);
                    castlingRight.setCastling(true);
                    validMoves.add(castlingRight);
                }
            }
        }
        return validMoves;
    }

    public boolean rangeEmpty(int row, int startingCol, int finalCol, ChessBoard board) {
        for (int i = startingCol; i <= finalCol; i++) {
            if (board.getPiece(new ChessPosition(row, i)) != null) {
                return false;
            }
        }
        return true;
    }

    public boolean inBoundsAndValid (int i, int j, int newRow, int newCol) {
        return (!(0 == i && 0 == j) && ((ChessBoard.BOARD_MIN <= newRow) && (ChessBoard.BOARD_MAX >= newRow) && (ChessBoard.BOARD_MIN <= newCol) && (ChessBoard.BOARD_MAX >= newCol)));
    }
}
