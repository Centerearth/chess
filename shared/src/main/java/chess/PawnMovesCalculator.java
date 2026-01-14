package chess;

import java.util.ArrayList;
import java.util.Collection;

public class PawnMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        ChessGame.TeamColor currentColor = board.getPiece(myPosition).getTeamColor();
        ArrayList<ChessMove> validMoves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        if (currentColor == ChessGame.TeamColor.WHITE) {
            ChessPosition nextSquare = new ChessPosition(row+1, col);
            if (board.getPiece(nextSquare) == null) {
                if (row == 7) {
                    for (ChessPiece.PieceType piece : ChessPiece.PieceType.values()) {
                        validMoves.add(new ChessMove(myPosition, nextSquare, piece));
                    }
                } else {
                    validMoves.add(new ChessMove(myPosition, nextSquare, null));
                    if (row == 2) {
                        ChessPosition twoSquare = new ChessPosition(row + 2, col);
                        if (board.getPiece(twoSquare) == null) {
                            validMoves.add(new ChessMove(myPosition, twoSquare, null));
                        }
                    }
                }
            }
            if (col != 1) {
                ChessPosition firstDiagonal = new ChessPosition(row + 1, col - 1);
                ChessPiece diagonalPiece = board.getPiece(firstDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    if (row == 7) {
                        for (ChessPiece.PieceType piece : ChessPiece.PieceType.values()) {
                            validMoves.add(new ChessMove(myPosition, firstDiagonal, piece));
                        }
                    } else {
                        validMoves.add(new ChessMove(myPosition, firstDiagonal, null));
                    }
                }
            }
            if (col != 8) {
                ChessPosition secondDiagonal = new ChessPosition(row + 1, col + 1);
                ChessPiece diagonalPiece = board.getPiece(secondDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    if (row == 7) {
                        for (ChessPiece.PieceType piece : ChessPiece.PieceType.values()) {
                            validMoves.add(new ChessMove(myPosition, secondDiagonal, piece));
                        }
                    } else {
                        validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                    }
                }
            }
        } else {
            ChessPosition nextSquare = new ChessPosition(row-1, col);
            if (board.getPiece(nextSquare) == null) {
                validMoves.add(new ChessMove(myPosition, nextSquare, null));
                if (row == 7) {
                    ChessPosition twoSquare = new ChessPosition(row-2, col);
                    if (board.getPiece(twoSquare) == null) {
                        validMoves.add(new ChessMove(myPosition, twoSquare, null));
                    }
                }
            }
            if (col != 1) {
                ChessPosition firstDiagonal = new ChessPosition(row - 1, col - 1);
                ChessPiece diagonalPiece = board.getPiece(firstDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    validMoves.add(new ChessMove(myPosition, firstDiagonal, null));
                }
            }
            if (col != 8) {
                ChessPosition secondDiagonal = new ChessPosition(row - 1, col + 1);
                ChessPiece diagonalPiece = board.getPiece(secondDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                }
            }
        }
        return validMoves;
    }
}
