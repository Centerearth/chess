package client;

import chess.*;
import java.util.ArrayList;
import chess.ChessGame.TeamColor;
import java.util.Arrays;

public class Agent {
    private TeamColor color;

    public Agent(ChessGame.TeamColor color) {
        this.color = color;
    }

    public ChessMove getBestMove(ChessGame game) {
        try {
            return alphaBetaMove(game);
        } catch (CloneNotSupportedException | InvalidMoveException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ChessMove alphaBetaMove(ChessGame game) throws CloneNotSupportedException, InvalidMoveException {
        return (ChessMove) recursiveAlphaBeta(game, 3, -10000.0, 10000.0, true).get(0);
    }

    private ArrayList<Object> recursiveAlphaBeta(ChessGame game, int depth, double alpha, double beta, boolean maximizingPlayer) throws CloneNotSupportedException, InvalidMoveException {
        ArrayList<ChessMove> validMoves = getAllValidMoves(game);
        double bestUtility = (maximizingPlayer) ? -10000.0 : 10000.0;
        ChessMove bestMove = null;

        if (validMoves.isEmpty()) {
            return new ArrayList<>(Arrays.asList(null, 
                game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE) ? 0.0 : bestUtility));
        }
        //the checkmate utility is baked in because the starting utilities are 10000 and -10000

        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            double expectedUtility = (depth == 1)
                ? calculateUtility(newGame)
                : (double) recursiveAlphaBeta(newGame, depth-1, alpha, beta, !maximizingPlayer).get(1);

            if (maximizingPlayer) {
                if (expectedUtility > bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                } else if (expectedUtility == bestUtility) {
                    // Randomly choose between moves of equal utility
                    if (Math.random() < 0.5) {
                        bestMove = move;
                    }
                }
                alpha = Math.max(alpha, bestUtility);
            } else {
                if (expectedUtility < bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                } else if (expectedUtility == bestUtility) {
                    // Randomly choose between moves of equal utility
                    if (Math.random() < 0.5) {
                        bestMove = move;
                    }
                }
                beta = Math.min(beta, bestUtility);
            }

            if (beta <= alpha) {
                break; 
            }
        }

        return new ArrayList<>(Arrays.asList(bestMove, bestUtility));
    }

    private ChessMove minimaxMove(ChessGame game) throws CloneNotSupportedException, InvalidMoveException {
        return (ChessMove) recursiveMinimax(game, 3, true).get(0);

    }

    private ArrayList<Object> recursiveMinimax(ChessGame game, int depth, boolean maximizingPlayer) throws CloneNotSupportedException, InvalidMoveException {
        //what to do if no validMoves?
        ArrayList<ChessMove> validMoves = getAllValidMoves(game);
        double bestUtility = (maximizingPlayer) ? -10000.0 : 10000.0;
        ChessMove bestMove = null;

        if (validMoves.isEmpty()) {
            return new ArrayList<>(Arrays.asList(null, 
                game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE) ? 0.0 : bestUtility));
        }
        
        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            double expectedUtility = (depth == 1)
                ? calculateUtility(newGame)
                : (double) recursiveMinimax(newGame, depth-1, !maximizingPlayer).get(1);

            if (maximizingPlayer) {
                if (expectedUtility > bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                } else if (expectedUtility == bestUtility) {
                    // Randomly choose between moves of equal utility
                    if (Math.random() < 0.5) {
                        bestMove = move;
                    }
                }
            } else {
                if (expectedUtility < bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                } else if (expectedUtility == bestUtility) {
                    // Randomly choose between moves of equal utility
                    if (Math.random() < 0.5) {
                        bestMove = move;
                    }
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
                    validMoves.addAll(game.validMoves(position));
                }
            }
        }
        return validMoves;
    }

    private double calculateUtility(ChessGame game) {
        double totalUtility = 0.0;
        ChessBoard board = game.getBoard();

        if (game.isInCheckmate(this.color)) {
            return -10000.0; 
        } else if (game.isInCheckmate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE)) {
            return 10000.0; 
        } else if (game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE)) {
            return 0.0; 
        }



        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null) {
                double factor = (piece.getTeamColor() == this.color) ? 1.0 : -1.0;
                    totalUtility += factor * getValue(piece);
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
