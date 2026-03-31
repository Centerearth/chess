package chess;

import java.util.Objects;

/**
 * Represents a single square position on a chess board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPosition {

    private final int row;
    private final int col;

    public ChessPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {
        return this.row;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left row
     */
    public int getColumn() {
        return this.col;
    }

    @Override
    public String toString() {
        return String.format("[%s%d]", numberToLetter(col), row);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPosition that = (ChessPosition) o;
        return this.getRow() == that.getRow() && this.getColumn() == that.getColumn();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRow(), col);
    }

    private String numberToLetter(int number) {
        switch (number) {
            case 1 -> {
                return "a";
            }
            case 2 -> {
                return "b";
            }
            case 3 -> {
                return "c";
            }
            case 4 -> {
                return "d";
            }
            case 5 -> {
                return "e";
            }
            case 6 -> {
                return "f";
            }
            case 7 -> {
                return "g";
            }
            case 8 -> {
                return "h";
            }
            default -> {
                return "unknown";
            }
        }
    }
}
