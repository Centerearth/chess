package chess;

import java.util.Collection;
import java.util.List;

public class KingMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return List.of(new ChessMove(new ChessPosition(1,1), new ChessPosition(1,2), null));
    }
}
