package chess;

import java.util.ArrayList;
import java.util.Collection;

public class QueenMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        RookMovesCalculator rookCalculator = new RookMovesCalculator();
        ArrayList<ChessMove> rookMoves = (ArrayList<ChessMove>) rookCalculator.pieceMoves(board, myPosition);
        BishopMovesCalculator bishopCalculator = new BishopMovesCalculator();
        ArrayList<ChessMove> bishopMoves = (ArrayList<ChessMove>) bishopCalculator.pieceMoves(board, myPosition);
        ArrayList<ChessMove> validMoves = new ArrayList<>();
        validMoves.addAll(rookMoves);
        validMoves.addAll(bishopMoves);

        return validMoves;
    }
}
