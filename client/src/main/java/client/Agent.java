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
        ChessMove bestMove = (ChessMove) recursiveAlphaBeta(game, 4, -10000, 10000, true).get(0);
        if (Math.random() < 0.05) {
            ArrayList<ChessMove> moves = getAllValidMoves(game);
            return moves.get((int)(Math.random() * moves.size())); //has a 5 percent chance to make a random move instead of the best move
        }
        return bestMove;
    }

    private ArrayList<Object> recursiveAlphaBeta(ChessGame game, int depth, int alpha, int beta, boolean maximizingPlayer) throws CloneNotSupportedException, InvalidMoveException {
        ArrayList<ChessMove> validMoves = getAllValidMoves(game);
        int bestUtility = (maximizingPlayer) ? -10000 : 10000;
        ChessMove bestMove = null;

        if (validMoves.isEmpty()) {
            return new ArrayList<>(Arrays.asList(null, 
                game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE) ? 0 : bestUtility));
        }
        //the checkmate utility is baked in because the starting utilities are 10000 and -10000
        bestMove = validMoves.get(0); //best moves can't be null for checkmate reasons


        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            int expectedUtility = (depth == 1)
                ? calculateUtility(newGame)
                : (int) recursiveAlphaBeta(newGame, depth-1, alpha, beta, !maximizingPlayer).get(1);

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
        int bestUtility = (maximizingPlayer) ? -10000 : 10000;
        ChessMove bestMove = null;

        if (validMoves.isEmpty()) {
            return new ArrayList<>(Arrays.asList(null, 
                game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE) ? 0 : bestUtility));
        }

        bestMove = validMoves.get(0); //best moves can't be null for checkmate reasons


        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            int expectedUtility = (depth == 1)
                ? calculateUtility(newGame)
                : (int) recursiveMinimax(newGame, depth-1, !maximizingPlayer).get(1);

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

    private int calculateUtility(ChessGame game) {
        int totalUtility = 0;
        ChessBoard board = game.getBoard();

        if (game.isInCheckmate(this.color)) {
            return -10000; 
        } else if (game.isInCheckmate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE)) {
            return 10000; 
        } else if (game.isInStalemate(this.color) || game.isInStalemate((this.color == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE)) {
            return 0; 
        }



        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null) {
                    int factor = (piece.getTeamColor() == this.color) ? 1 : -1;
                        totalUtility += factor * getValue(piece, isEndgame(game));
                        totalUtility += factor * pstCalculation(piece, position, isEndgame(game));
                }
                



            }
        }
        return totalUtility;
    }

    private int getValue(ChessPiece piece, boolean isEndgame) {
        if (isEndgame) {
            switch (piece.getPieceType()) {
                case PAWN:
                    return 94;
                case KNIGHT:
                    return 281;
                case BISHOP:
                    return 297;
                case ROOK:
                    return 512;
                case QUEEN:
                    return 936;
                default:
                    return 0;
            }
        } else {
            switch (piece.getPieceType()) {
                case PAWN:
                    return 82;
                case KNIGHT:
                    return 337;
                case BISHOP:
                    return 365;
                case ROOK:
                    return 477;
                case QUEEN:
                    return 1025;
                default:
                    return 0;
            }
        }
    }

    private boolean isEndgame(ChessGame game) {
        ChessBoard board = game.getBoard();
        int totalPieces = 0;
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                if (board.getPiece(position) != null) {
                    totalPieces++;
                }
            }
        }
        return totalPieces <= 12; 
    }

    private int pstCalculation(ChessPiece piece, ChessPosition position, boolean isEndgame) {
        int row = position.getRow() - 1;
        int col = position.getColumn() - 1;

        if (piece.getTeamColor() == TeamColor.BLACK) {
            row = 7 - row;
            col = 7 - col;
        }

        if (!isEndgame) {
            switch (piece.getPieceType()) {
                case PAWN:
                    return midgamePST.PAWN_PST[row][col];
                case KNIGHT:
                    return midgamePST.KNIGHT_PST[row][col];
                case BISHOP:
                    return midgamePST.BISHOP_PST[row][col];
                case ROOK:
                    return midgamePST.ROOK_PST[row][col];
                case QUEEN:
                    return midgamePST.QUEEN_PST[row][col];
                case KING:
                    return midgamePST.KING_PST[row][col];
                default:
                    return 0;
            }
        } else {
            switch (piece.getPieceType()) {
                case PAWN:
                    return endgamePST.PAWN_PST[row][col];
                case KNIGHT:
                    return endgamePST.KNIGHT_PST[row][col];
                case BISHOP:
                    return endgamePST.BISHOP_PST[row][col];
                case ROOK:
                    return endgamePST.ROOK_PST[row][col];
                case QUEEN:
                    return endgamePST.QUEEN_PST[row][col];
                case KING:
                    return endgamePST.KING_PST[row][col];
                default:
                    return 0;
            }
        }
    }
}