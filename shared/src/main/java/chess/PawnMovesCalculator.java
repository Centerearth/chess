package chess;

import chess.ChessPiece.PieceType;

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
                    this.promotion(validMoves, myPosition, nextSquare);
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
                        this.promotion(validMoves, myPosition, firstDiagonal);
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
                        this.promotion(validMoves, myPosition, secondDiagonal);
                    } else {
                        validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                    }
                }
            }
        } else {
            ChessPosition nextSquare = new ChessPosition(row-1, col);
            if (board.getPiece(nextSquare) == null) {
                if (row == 2) {
                    this.promotion(validMoves, myPosition, nextSquare);
                } else {
                    validMoves.add(new ChessMove(myPosition, nextSquare, null));
                    if (row == 7) {
                        ChessPosition twoSquare = new ChessPosition(row - 2, col);
                        if (board.getPiece(twoSquare) == null) {
                            validMoves.add(new ChessMove(myPosition, twoSquare, null));
                        }
                    }
                }
            }
            if (col != 1) {
                ChessPosition firstDiagonal = new ChessPosition(row - 1, col - 1);
                ChessPiece diagonalPiece = board.getPiece(firstDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    if (row == 2) {
                        this.promotion(validMoves, myPosition, firstDiagonal);
                    } else {
                        validMoves.add(new ChessMove(myPosition, firstDiagonal, null));
                    }
                }
            }
            if (col != 8) {
                ChessPosition secondDiagonal = new ChessPosition(row - 1, col + 1);
                ChessPiece diagonalPiece = board.getPiece(secondDiagonal);
                if (diagonalPiece != null && diagonalPiece.getTeamColor() != currentColor) {
                    if (row == 2) {
                        this.promotion(validMoves, myPosition, secondDiagonal);
                    } else {
                        validMoves.add(new ChessMove(myPosition, secondDiagonal, null));
                    }
                }
            }
        }
        return validMoves;
    }

    public void promotion(ArrayList<ChessMove> validMoves, ChessPosition myPosition, ChessPosition nextSquare) {
        for (PieceType piece : PieceType.values()) {
            if (piece != PieceType.KING && piece != PieceType.PAWN) {
                validMoves.add(new ChessMove(myPosition, nextSquare, piece));
            }
        }
    }
}
