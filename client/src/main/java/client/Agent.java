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
        ChessMove bestMove = (ChessMove) recursiveAlphaBeta(game, 4, -10000, 10000, true).get(0); //depth 4 to 5 is a big jump
        // if (Math.random() < 0.05) {
        //     ArrayList<ChessMove> moves = getAllValidMoves(game);
        //     return moves.get((int)(Math.random() * moves.size())); //has a 5 percent chance to make a random move instead of the best move
        // } don't really need the random anymore since we have the pst's
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


        ArrayList<ChessMove> captures = getAllValidCaptures(game);
        ArrayList<ChessMove> quietMoves = new ArrayList<>(validMoves);
        quietMoves.removeAll(captures);
        captures.addAll(quietMoves); // reordering so we compute captures first, then quiet moves
        validMoves = captures;

        for (ChessMove move : validMoves) {
            ChessGame newGame = (ChessGame) game.clone();
            newGame.makeMove(move);

            int expectedUtility = (depth == 1)
                ? quiescence(newGame, alpha, beta, !maximizingPlayer, 5)
                : (int) recursiveAlphaBeta(newGame, depth-1, alpha, beta, !maximizingPlayer).get(1);

            if (depth == 4) {
                //boolean isCapture = game.getBoard().getPiece(move.getEndPosition()) != null;
                //System.out.println(move + " -> " + expectedUtility + (isCapture ? " [CAPTURE]" : ""));
            }

            if (maximizingPlayer) {
                if (expectedUtility > bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                }
                // } else if (expectedUtility == bestUtility) {
                //     Randomly choose between moves of equal utility
                //     if (Math.random() < 0.5) { causing problems
                //         bestMove = move;
                //     }
                // }
                alpha = Math.max(alpha, bestUtility);
            } else {
                if (expectedUtility < bestUtility) {
                    bestUtility = expectedUtility;
                    bestMove = move;
                }
                // } else if (expectedUtility == bestUtility) {
                //     Randomly choose between moves of equal utility
                //     if (Math.random() < 0.5) { taking this out since it was causing problems
                //         bestMove = move;
                //     }
                // }
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

    private ArrayList<ChessMove> getAllValidCaptures(ChessGame game) { //change this to only reflect captures
        ArrayList<ChessMove> validMoves = getAllValidMoves(game);

        ArrayList<ChessMove> validCaptures = new ArrayList<>();
        ChessBoard board = game.getBoard();

        for (ChessMove move : validMoves) {
            ChessPosition endPosition = move.getEndPosition();
            if (board.getPiece(endPosition) != null) {
                validCaptures.add(move);
            }
        }
        return validCaptures;
    }

    private int quiescence(ChessGame game, int alpha, int beta, boolean maximizingPlayer, int depth) throws InvalidMoveException, CloneNotSupportedException {
        if (depth == 0) {
            return calculateUtility(game);
        }
        
        int doNothing = calculateUtility(game);

        if (maximizingPlayer) {
            if (doNothing >= beta) {
                return beta;   // this position wouldn't be reached, can be pruned
            }     
            alpha = Math.max(alpha, doNothing); //update alpha if necessary
        } else {
            if (doNothing <= alpha) {
                return alpha;   // this position wouldn't be reached, can be pruned
            }     
            beta = Math.min(beta, doNothing); //update beta if necessary
        }

        ArrayList<ChessMove> validCaptures = getAllValidCaptures(game);

        for (ChessMove capture : validCaptures) {
                ChessGame newGame = (ChessGame) game.clone();
                newGame.makeMove(capture);
                int futureUtility = quiescence(newGame, alpha, beta, !maximizingPlayer, depth-1);
                if (maximizingPlayer) {
                    if (futureUtility >= beta) {
                        return beta;
                    }
                    alpha = Math.max(alpha, futureUtility);
                } else {
                    if (futureUtility <= alpha) {
                        return alpha;
                    }
                    beta = Math.min(beta, futureUtility);
                }
        }

        return (maximizingPlayer) ? alpha : beta;
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
                    boolean endgame = isEndgame(game);
                        totalUtility += factor * getValue(piece, endgame);
                        totalUtility += factor * pstCalculation(piece, position, endgame);
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

        if (piece.getTeamColor() == TeamColor.WHITE) {
            row = 7 - row;
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