package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {

    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;
    private boolean enPassant;
    private ChessPosition enPassantAdjacent;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
        this.enPassant = false;
        this.enPassantAdjacent = null;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return this.startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return this.endPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return this.promotionPiece;
    }

    public boolean getEnPassant() {return this.enPassant;}

    public void setEnPassant(boolean enPassant) {
        this.enPassant = enPassant;
    }

    public ChessPosition getEnPassantAdjacent() {return this.enPassantAdjacent;}

    public void setEnPassantAdjacent(ChessPosition enPassantAdjacent) {
        this.enPassantAdjacent = enPassantAdjacent;
    }

    @Override
    public String toString() {
        return "" + startPosition +
                " -> " + endPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessMove chessMove = (ChessMove) o;
        if (Objects.equals(getStartPosition(), chessMove.getStartPosition())) {
            if (Objects.equals(getEndPosition(), chessMove.getEndPosition())) {
                return getPromotionPiece() == chessMove.getPromotionPiece();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartPosition(), getEndPosition(), getPromotionPiece());
    }
}
