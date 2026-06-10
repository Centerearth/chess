package client;

import chess.*;
import java.util.ArrayList;
import chess.ChessGame.TeamColor;
import java.util.Arrays;

public class Agent {
    private TeamColor color;

    public Agent(TeamColor color) {
        this.color = color;
    }

    public ChessMove minimaxMove(ChessGame game) throws CloneNotSupportedException, InvalidMoveException {
        return (ChessMove) recursiveMinimax(game, 3, true).get(0);

    }

    private ArrayList<Object> recursiveMinimax(ChessGame game, int depth, boolean maximizingPlayer) throws CloneNotSupportedException, InvalidMoveException {
        //what to do if no validMoves?
        ArrayList<ChessMove> validMoves = getAllValidMoves(game);
        double bestUtility = (maximizingPlayer) ? -10000.0 : 10000.0;
        ChessMove bestMove = null;


        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            double expectedUtility = (depth == 2)
                ? calculateUtility(newGame)
                : (double) recursiveMinimax(newGame, depth-1, !maximizingPlayer).get(1);

            if (maximizingPlayer) {
                if (expectedUtility > bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                }
            } else {
                if (expectedUtility < bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                }
            }
        }

        return new ArrayList<>(Arrays.asList(bestMove, bestUtility));

    }

    private ArrayList<ChessMove> getAllValidMoves(ChessGame game) {
        ArrayList<ChessMove> validMoves = new ArrayList<>();
        ChessBoard board = game.getBoard();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == game.getTeamTurn()) {
                    validMoves.addAll(piece.pieceMoves(board, position));
                }
            }
        }
        return validMoves;
    }

    private double calculateUtility(ChessGame game) {
        double totalUtility = 0.0;
        ChessBoard board = game.getBoard();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                if (board.getPiece(position) != null) {
                    totalUtility += getValue(board.getPiece(position));
                }
            }
        }
        return totalUtility;
    }

    private double getValue(ChessPiece piece) {
        switch (piece.getPieceType()) {
            case PAWN:
                return 1.0;
            case KNIGHT:
                return 3.0;
            case BISHOP:
                return 3.0;
            case ROOK:
                return 5.0;
            case QUEEN:
                return 9.0;
            default:
                return 0.0;
        }
    }
}
