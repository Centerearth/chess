package chess;

import chess.ChessPiece.PieceType;

import java.util.ArrayList;
import java.util.Collection;

public class PawnMovesCalculator implements PieceMovesCalculator {

    private static final int WHITE_START_ROW = 2;
    private static final int BLACK_START_ROW = 7;
    private static final int WHITE_PRE_PROMOTION_ROW = 7;
    private static final int BLACK_PRE_PROMOTION_ROW = 2;
    private static final int WHITE_EN_PASSANT_ROW = 5;
    private static final int BLACK_EN_PASSANT_ROW = 4;

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ChessGame.TeamColor currentColor = board.getPiece(myPosition).getTeamColor();
        ArrayList<ChessMove> validMoves = new ArrayList<>();

        if (currentColor == ChessGame.TeamColor.WHITE) {
            pawnMoves(currentColor, validMoves, board, myPosition, WHITE_START_ROW, WHITE_PRE_PROMOTION_ROW, 
                WHITE_EN_PASSANT_ROW, 1);
        } else {
            pawnMoves(currentColor, validMoves, board, myPosition, BLACK_START_ROW, BLACK_PRE_PROMOTION_ROW, 
                BLACK_EN_PASSANT_ROW, -1);
        }
        return validMoves;
    }

    public void pawnMoves(ChessGame.TeamColor currentColor,
                          ArrayList<ChessMove> validMoves,
                          ChessBoard board,
                          ChessPosition myPosition,
                          int secondRow,
                          int secondToLastRow,
                          int enPassantRow,
                          int factor) {

        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        ChessPosition nextSquare = new ChessPosition(row+factor, col);
        //if there is no piece in front (or two in front), add the square to validMoves
        if (board.getPiece(nextSquare) == null) {
            if (row == secondToLastRow) {
                this.promotion(validMoves, myPosition, nextSquare);
            } else {
                validMoves.add(new ChessMove(myPosition, nextSquare, null));
                if (row == secondRow) {
                    ChessPosition twoSquare = new ChessPosition(row + factor * 2, col);
                    if (board.getPiece(twoSquare) == null) {
                        validMoves.add(new ChessMove(myPosition, twoSquare, null));
                    }
                }
            }
        }
        //check the diagonal assuming the piece is not at the edge
        if (col != ChessBoard.BOARD_MIN) {
            ChessPosition firstDiagonal = new ChessPosition(row + factor, col - 1);
            ChessPiece diagonalPiece = board.getPiece(firstDiagonal);

            ChessPosition firstAdjacent = new ChessPosition(row, col - 1);
            ChessPiece firstAdjacentPiece = board.getPiece(firstAdjacent);

            if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                if (row == secondToLastRow) {
                    this.promotion(validMoves, myPosition, firstDiagonal);
                } else {
                    validMoves.add(new ChessMove(myPosition, firstDiagonal, null));
                }
            } 
            else if (row == enPassantRow && firstAdjacentPiece != null &&
                firstAdjacentPiece.getPieceType() == PieceType.PAWN //en passant
                && firstAdjacentPiece.getTeamColor() != currentColor && firstAdjacentPiece.getTotalMoves() == 1) {
                ChessMove enPassantMove = new ChessMove(myPosition, firstDiagonal, null);
                enPassantMove.setEnPassant(true);
                enPassantMove.setEnPassantAdjacent(firstAdjacent);
                validMoves.add(enPassantMove);
            }
        }

        //check the other diagonal
        if (col != ChessBoard.BOARD_MAX) {
            ChessPosition secondDiagonal = new ChessPosition(row + factor, col + 1);
            ChessPiece diagonalPiece = board.getPiece(secondDiagonal);

            ChessPosition secondAdjacent = new ChessPosition(row, col + 1);
            ChessPiece secondAdjacentPiece = board.getPiece(secondAdjacent);
            if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                if (row == secondToLastRow) {
                    this.promotion(validMoves, myPosition, secondDiagonal);
                } else {
                    validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                }
            } 
            else if (row == enPassantRow && secondAdjacentPiece != null &&
                secondAdjacentPiece.getPieceType() == PieceType.PAWN //en passant
                && secondAdjacentPiece.getTeamColor() != currentColor && secondAdjacentPiece.getTotalMoves() == 1) {
                ChessMove enPassantMove = new ChessMove(myPosition, secondDiagonal, null);
                enPassantMove.setEnPassant(true);
                enPassantMove.setEnPassantAdjacent(secondAdjacent);
                validMoves.add(enPassantMove);
            }
        }
    }

    public void promotion(ArrayList<ChessMove> validMoves, ChessPosition myPosition, ChessPosition nextSquare) {
        for (PieceType piece : PieceType.values()) {
            if (piece != PieceType.KING && piece != PieceType.PAWN) {
                validMoves.add(new ChessMove(myPosition, nextSquare, piece));
            }
        }
    }
}
