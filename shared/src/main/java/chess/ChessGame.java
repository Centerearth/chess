package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import chess.ChessPiece.PieceType;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame implements Cloneable {
        private ChessBoard board;
        private TeamColor teamsTurn;
        private int enPassantColumn;

    public ChessGame() {
        board = new ChessBoard();
        this.board.resetBoard();
        teamsTurn = TeamColor.WHITE;
        enPassantColumn = -1;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.teamsTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamsTurn = team;
    }

    public void updateTeamTurn() {
        if (teamsTurn == TeamColor.WHITE) {
            teamsTurn = TeamColor.BLACK;
        } else {
            teamsTurn = TeamColor.WHITE;
        }
    }

    public int getEnPassantColumn() {
        return enPassantColumn;
    }

    public void setEnPassantColumn(int col) {
        this.enPassantColumn = col;
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
                ChessGame tempGameCastling = (ChessGame) this.clone();
                tempGame.makeMoveTesting(move);
                if (!tempGame.isInCheck(startPiece.getTeamColor())) {
                    ChessMove tempKingMove;
                    if (move.getCastling()) {
                        if (this.isInCheck(startPiece.getTeamColor())) {
                            // cannot castle while in check
                        } else {
                            if (move.getEndPosition().getColumn() == 7) {
                                tempKingMove = new ChessMove(move.getStartPosition(), new ChessPosition(move.getStartPosition().getRow(), 6), null);
                            } else {
                                tempKingMove = new ChessMove(move.getStartPosition(), new ChessPosition(move.getStartPosition().getRow(), 4), null);
                            }

                            tempGameCastling.makeMoveTesting(tempKingMove);
                            if (!tempGameCastling.isInCheck(startPiece.getTeamColor())) {
                                validMoves.add(move);
                            }
                        }
                    } else if (!move.getEnPassant()) {
                        validMoves.add(move);
                    } else if (move.getEnPassant() && move.getEnPassantAdjacent().getColumn() == enPassantColumn) {
                        validMoves.add(move);
                    }
                
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
        moveGeneralized(move, false);
    }

    public void makeMoveTesting(ChessMove move) throws InvalidMoveException {
        moveGeneralized(move, true);
    }

    public void moveGeneralized(ChessMove move, boolean testing) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece.PieceType promotionPiece = move.getPromotionPiece();
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            throw new InvalidMoveException("There is no piece there");
        }
        TeamColor color = piece.getTeamColor();

        ArrayList<ChessMove> validMoves;
        if (testing) {
            validMoves = (ArrayList<ChessMove>) piece.pieceMoves(board, startPosition);
        } else {
            if (color != getTeamTurn()) {
                throw new InvalidMoveException("It is not your turn");
            }
            validMoves = (ArrayList<ChessMove>) this.validMoves(startPosition);
        }

        if (validMoves.contains(move)) {
            int moveIndex = validMoves.indexOf(move);
            if (validMoves.get(moveIndex).getEnPassant()) { //it's because the user's move doesn't have en passant
                ChessPosition toBeErased = validMoves.get(moveIndex).getEnPassantAdjacent();
                board.addPiece(startPosition, null);
                board.addPiece(toBeErased, null);
                board.addPiece(endPosition, piece);
            } else if (validMoves.get(moveIndex).getCastling()) {
                //needs to add the king, then the rook, as well as updating the rook's number of moves. 
                board.addPiece(startPosition, null);
                board.addPiece(endPosition, piece);
                if (endPosition.getColumn() == 7) {
                    ChessPosition rookStart = new ChessPosition(startPosition.getRow(), 8);
                    ChessPosition rookEnd = new ChessPosition(startPosition.getRow(), 6);
                    ChessPiece rook = board.getPiece(rookStart);
                    board.addPiece(rookStart, null);
                    board.addPiece(rookEnd, rook);
                    if (!testing) {
                        rook.updateTotalMoves();
                    }
                } else {
                    ChessPosition rookStart = new ChessPosition(startPosition.getRow(), 1);
                    ChessPosition rookEnd = new ChessPosition(startPosition.getRow(), 4);
                    ChessPiece rook = board.getPiece(rookStart);
                    board.addPiece(rookStart, null);
                    board.addPiece(rookEnd, rook);
                    if (!testing) {
                        rook.updateTotalMoves();
                    }
                }
            } else {
                board.addPiece(startPosition, null);
                if (promotionPiece == PieceType.KING || promotionPiece == PieceType.PAWN) {
                    throw new InvalidMoveException("Invalid promotion piece");
                } else if (promotionPiece == null) {
                    board.addPiece(endPosition, piece);
                } else {
                    ChessPiece promotedPiece = new ChessPiece(color, promotionPiece);
                    board.addPiece(endPosition, promotedPiece);
                }
            }
        } else {
            throw new InvalidMoveException("This is an invalid move");
        }

        if (!testing) {
            if (piece.getPieceType() == PieceType.PAWN && ((startPosition.getRow() == 2 && endPosition.getRow() == 4)
            || (startPosition.getRow() == 7 && endPosition.getRow() == 5))) {
                enPassantColumn = endPosition.getColumn();
            } else {
                enPassantColumn = -1;
            }
            this.updateTeamTurn();
            piece.updateTotalMoves();
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
        for (int i = ChessBoard.BOARD_MIN; i <= ChessBoard.BOARD_MAX; i++ ) {
            for (int j = ChessBoard.BOARD_MIN; j <= ChessBoard.BOARD_MAX; j++) {
                if (checkHelper(teamColor, kingPosition, i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkHelper(TeamColor teamColor, ChessPosition kingPosition, int i, int j) {
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
        return false;
    }

    public ChessPosition findKingPosition(TeamColor teamColor) {
        for (int i = ChessBoard.BOARD_MIN; i <= ChessBoard.BOARD_MAX; i++ ) {
            for (int j = ChessBoard.BOARD_MIN; j <= ChessBoard.BOARD_MAX; j++) {
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
        for (int i = ChessBoard.BOARD_MIN; i <= ChessBoard.BOARD_MAX; i++ ) {
            for (int j = ChessBoard.BOARD_MIN; j <= ChessBoard.BOARD_MAX; j++) {
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
        return Objects.equals(getBoard(), chessGame.getBoard()) && teamsTurn == chessGame.teamsTurn
                && enPassantColumn == chessGame.enPassantColumn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBoard(), teamsTurn);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        var clone = new ChessGame();
        clone.teamsTurn = this.teamsTurn;
        clone.enPassantColumn = this.enPassantColumn;

        for (int i = ChessBoard.BOARD_MIN; i <= ChessBoard.BOARD_MAX; i++ ) {
            for (int j = ChessBoard.BOARD_MIN; j <= ChessBoard.BOARD_MAX; j++) {
                ChessPosition position = new ChessPosition(i,j);
                ChessPiece piece = this.board.getPiece(position);
                clone.board.addPiece(position, piece == null ? null 
                    : new ChessPiece(piece.getTeamColor(), piece.getPieceType(), piece.getTotalMoves()));
            }
        }

        return clone;
    }
}
