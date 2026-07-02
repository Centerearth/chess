package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Generates standard algebraic notation (e.g. "Nxf6+", "O-O", "e8=Q#")
 * for a move about to be played on a given game state.
 */
public class SanGenerator {

    /**
     * @param before the game state the move is being played on (not yet applied)
     * @param move   a move that is legal in {@code before}
     */
    public static String toSan(ChessGame before, ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessBoard board = before.getBoard();
        ChessPiece piece = board.getPiece(start);
        if (piece == null) {
            return move.toString();
        }

        // find the engine's version of this move so en passant / castling flags are set
        ChessMove matched = move;
        Collection<ChessMove> legalFromStart = before.validMoves(start);
        for (ChessMove candidate : legalFromStart) {
            if (candidate.equals(move)) {
                matched = candidate;
                break;
            }
        }

        StringBuilder san = new StringBuilder();
        boolean isPawn = piece.getPieceType() == ChessPiece.PieceType.PAWN;
        boolean isCapture = board.getPiece(end) != null || matched.getEnPassant();

        if (matched.getCastling()) {
            san.append(end.getColumn() == 7 ? "O-O" : "O-O-O");
        } else {
            if (isPawn) {
                if (isCapture) {
                    san.append(fileLetter(start.getColumn())).append("x");
                }
            } else {
                san.append(pieceLetter(piece.getPieceType()));
                san.append(disambiguation(before, piece, start, end));
                if (isCapture) {
                    san.append("x");
                }
            }
            san.append(fileLetter(end.getColumn())).append(end.getRow());
            if (move.getPromotionPiece() != null) {
                san.append("=").append(pieceLetter(move.getPromotionPiece()));
            }
        }

        san.append(checkSuffix(before, move, piece.getTeamColor()));
        return san.toString();
    }

    private static String checkSuffix(ChessGame before, ChessMove move, ChessGame.TeamColor mover) {
        try {
            ChessGame after = (ChessGame) before.clone();
            after.makeMove(move);
            ChessGame.TeamColor opponent = (mover == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            if (after.isInCheckmate(opponent)) {
                return "#";
            }
            if (after.isInCheck(opponent)) {
                return "+";
            }
        } catch (CloneNotSupportedException | InvalidMoveException e) {
            // notation stays suffix-free if the lookahead fails
        }
        return "";
    }

    /** File and/or rank of the start square when another identical piece could also reach the target. */
    private static String disambiguation(ChessGame game, ChessPiece piece, ChessPosition start, ChessPosition end) {
        ArrayList<ChessPosition> rivals = new ArrayList<>();
        for (int row = ChessBoard.BOARD_MIN; row <= ChessBoard.BOARD_MAX; row++) {
            for (int col = ChessBoard.BOARD_MIN; col <= ChessBoard.BOARD_MAX; col++) {
                ChessPosition position = new ChessPosition(row, col);
                if (position.equals(start)) {
                    continue;
                }
                ChessPiece other = game.getBoard().getPiece(position);
                if (other == null || other.getTeamColor() != piece.getTeamColor()
                        || other.getPieceType() != piece.getPieceType()) {
                    continue;
                }
                for (ChessMove candidate : game.validMoves(position)) {
                    if (candidate.getEndPosition().equals(end)) {
                        rivals.add(position);
                        break;
                    }
                }
            }
        }
        if (rivals.isEmpty()) {
            return "";
        }
        boolean fileUnique = rivals.stream().noneMatch(p -> p.getColumn() == start.getColumn());
        if (fileUnique) {
            return fileLetter(start.getColumn());
        }
        boolean rankUnique = rivals.stream().noneMatch(p -> p.getRow() == start.getRow());
        if (rankUnique) {
            return String.valueOf(start.getRow());
        }
        return fileLetter(start.getColumn()) + start.getRow();
    }

    private static String pieceLetter(ChessPiece.PieceType type) {
        return switch (type) {
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case ROOK -> "R";
            case QUEEN -> "Q";
            case KING -> "K";
            default -> "";
        };
    }

    private static String fileLetter(int col) {
        return String.valueOf((char) ('a' + col - 1));
    }
}
