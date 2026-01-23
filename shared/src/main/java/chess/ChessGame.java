package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame implements Cloneable {
        private ChessBoard board;
        private TeamColor teams_turn;

    public ChessGame() {
        board = new ChessBoard();
        this.board.resetBoard();
        teams_turn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.teams_turn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teams_turn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece startPiece = board.getPiece(startPosition);
        if (startPiece == null) {
            return null;
        }

        Collection<ChessMove> initialMoves = startPiece.pieceMoves(board, startPosition);
        ArrayList<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : initialMoves) {
            try {
                ChessGame tempGame = (ChessGame) this.clone();
                tempGame.makeMoveTesting(move);
                if (!tempGame.isInCheck(startPiece.getTeamColor())) {
                    validMoves.add(move);
                }
            } catch (CloneNotSupportedException | InvalidMoveException e) {
                throw new RuntimeException(e);
            }
        }
        return validMoves;

    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece.PieceType promotionPiece = move.getPromotionPiece();
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            throw new InvalidMoveException("There is no piece there");
        }

        TeamColor color = piece.getTeamColor();
        if (color != getTeamTurn()) {
            throw new InvalidMoveException("It is not your turn");
        }

        Collection<ChessMove> validMoves = this.validMoves(startPosition);
        if (validMoves.contains(move)) {
            board.addPiece(startPosition, null);
            if (promotionPiece == null) {
                board.addPiece(endPosition, piece);
            } else {
                ChessPiece promotedPiece = new ChessPiece(color, promotionPiece);
                board.addPiece(endPosition, promotedPiece);
            }
        } else {
            throw new InvalidMoveException("This is an invalid move");
        }

        if (piece.getTeamColor() == TeamColor.WHITE) {
            this.setTeamTurn(TeamColor.BLACK);
        } else {
            this.setTeamTurn(TeamColor.WHITE);
        }

    }

    public void makeMoveTesting(ChessMove move) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece.PieceType promotionPiece = move.getPromotionPiece();
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            throw new InvalidMoveException("There is no piece there");
        }
        TeamColor color = piece.getTeamColor();

        Collection<ChessMove> validMoves = piece.pieceMoves(board, startPosition);
        if (validMoves.contains(move)) {
            board.addPiece(startPosition, null);
            if (promotionPiece == null) {
                board.addPiece(endPosition, piece);
            } else {
                ChessPiece promotedPiece = new ChessPiece(color, promotionPiece);
                board.addPiece(endPosition, promotedPiece);
            }
        } else {
            throw new InvalidMoveException("This is an invalid move");
        }
    }


    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKingPosition(teamColor);
        for (int i = 1; i <= 8; i++ ) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() != teamColor) {
                    Collection<ChessMove> validMoves = piece.pieceMoves(board, position);
                    for (ChessMove move: validMoves) {
                        if (move.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public ChessPosition findKingPosition(TeamColor teamColor) {
        for (int i = 1; i <= 8; i++ ) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition kingPosition = new ChessPosition(i, j);
                ChessPiece king = board.getPiece(kingPosition);
                if (king != null) {
                    if (king.getPieceType() == ChessPiece.PieceType.KING && king.getTeamColor() == teamColor) {
                        return kingPosition;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (this.isInCheck(teamColor)) {
            return noValidMoves(teamColor);
        }
        return false;
    }

    public boolean noValidMoves(TeamColor teamColor) {
        for (int i = 1; i <= 8; i++ ) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i,j);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor) {
                    ArrayList<ChessMove> validMoves = (ArrayList<ChessMove>) this.validMoves(position);
                    if (validMoves != null && !validMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (this.isInCheck(teamColor)) {
            return false;
        }
        return noValidMoves(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return this.board;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(getBoard(), chessGame.getBoard()) && teams_turn == chessGame.teams_turn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBoard(), teams_turn);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Object o = super.clone(); // why did IntelliJ say to put this in ?
        var clone = new ChessGame();
        clone.teams_turn = this.teams_turn;

        for (int i = 1; i <= 8; i++ ) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i,j);
                clone.board.addPiece(position, this.board.getPiece(position));
            }
        }

        return clone;
    }
}
