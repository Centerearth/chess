package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KingMovesCalculator implements PieceMovesCalculator {
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ArrayList<ChessMove> validMoves = new ArrayList<>();
        validMoves.add(new ChessMove(new ChessPosition(1,1), new ChessPosition(1,2), null));
        return validMoves;
        // return List.of(new ChessMove(new ChessPosition(1,1), new ChessPosition(1,2), null));
    }
}
