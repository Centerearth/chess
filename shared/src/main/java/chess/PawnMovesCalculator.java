package chess;

import chess.ChessPiece.PieceType;

import java.util.ArrayList;
import java.util.Collection;

public class PawnMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ChessGame.TeamColor currentColor = board.getPiece(myPosition).getTeamColor();
        ArrayList<ChessMove> validMoves = new ArrayList<>();

        if (currentColor == ChessGame.TeamColor.WHITE) {
            pawnMoves(currentColor, validMoves, board, myPosition, 2, 7, 1);
        } else {
            pawnMoves(currentColor, validMoves, board, myPosition, 7, 2, -1);
        }
        return validMoves;
    }

    public void pawnMoves(ChessGame.TeamColor currentColor, ArrayList<ChessMove> validMoves, ChessBoard board, ChessPosition myPosition, int secondRow, int secondToLastRow, int factor) {

        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        ChessPosition nextSquare = new ChessPosition(row+factor, col);
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
        if (col != 1) {
            ChessPosition firstDiagonal = new ChessPosition(row + factor, col - 1);
            ChessPiece diagonalPiece = board.getPiece(firstDiagonal);
            if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                if (row == secondToLastRow) {
                    this.promotion(validMoves, myPosition, firstDiagonal);
                } else {
                    validMoves.add(new ChessMove(myPosition, firstDiagonal, null));
                }
            }
        }
        if (col != 8) {
            ChessPosition secondDiagonal = new ChessPosition(row + factor, col + 1);
            ChessPiece diagonalPiece = board.getPiece(secondDiagonal);
            if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                if (row == secondToLastRow) {
                    this.promotion(validMoves, myPosition, secondDiagonal);
                } else {
                    validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                }
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
