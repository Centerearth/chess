package chess;

import static chess.ChessPiece.PieceType.KING;
import static chess.ChessPiece.PieceType.ROOK;

import chess.ChessGame.TeamColor;

public class FenSerializer {
    public static String toFen(ChessGame game) {
        StringBuilder sb = new StringBuilder();
        ChessBoard board = game.getBoard();
        for (int row = 8; row >= 1; row--) {
            int emptySquares = 0;

            for (int col = 1; col <= 8; col++) {
                ChessPosition currentPosition = new ChessPosition(row, col);
                ChessPiece currentPiece = board.getPiece(currentPosition);
                if (currentPiece == null) {
                    emptySquares += 1;
                } else {
                    if (emptySquares != 0) {
                        sb.append(emptySquares);
                        emptySquares = 0;
                    }
                    sb.append(pieceToString(currentPiece));
                }
            }
            if (emptySquares != 0) {
                sb.append(emptySquares);
                emptySquares = 0; //this is a duplicate reset I believe
            }
            if (row != 1) {
                sb.append("/");
            }
        } 

        if (game.getTeamTurn() == ChessGame.TeamColor.WHITE) {
            sb.append(" w"); 
        } else { 
            sb.append(" b");
        }

        sb.append(" ");
        
        boolean hasAnythingChanged = false;
        ChessPiece expectedWhiteKing = board.getPiece(new ChessPosition(1, 5));
        if (expectedWhiteKing != null && expectedWhiteKing.getPieceType() == KING && expectedWhiteKing.getTotalMoves() == 0) {
            ChessPiece expectedKingRook = board.getPiece(new ChessPosition(1, 8));
            if (expectedKingRook != null && expectedKingRook.getPieceType() == ROOK && expectedKingRook.getTotalMoves() == 0) {
                hasAnythingChanged = true;
                sb.append("K");
            }
            ChessPiece expectedQueenRook = board.getPiece(new ChessPosition(1, 1));
            if (expectedQueenRook != null && expectedQueenRook.getPieceType() == ROOK && expectedQueenRook.getTotalMoves() == 0) {
                hasAnythingChanged = true;
                sb.append("Q");
            }
        }
        ChessPiece expectedBlackKing = board.getPiece(new ChessPosition(8, 5));
        if (expectedBlackKing != null && expectedBlackKing.getPieceType() == KING && expectedBlackKing.getTotalMoves() == 0) {
            ChessPiece expectedKingRook = board.getPiece(new ChessPosition(8, 8));
            if (expectedKingRook != null && expectedKingRook.getPieceType() == ROOK && expectedKingRook.getTotalMoves() == 0) {
                hasAnythingChanged = true;
                sb.append("k");
            }
            ChessPiece expectedQueenRook = board.getPiece(new ChessPosition(8, 1));
            if (expectedQueenRook != null && expectedQueenRook.getPieceType() == ROOK && expectedQueenRook.getTotalMoves() == 0) {
                hasAnythingChanged = true;
                sb.append("q");
            }
        }

        if (!hasAnythingChanged) {
            sb.append("-");
        }

        sb.append(" ");

        if (game.getEnPassantColumn() > 0) {
            int epRow = (game.getTeamTurn() == TeamColor.WHITE) ? 3 : 6;
            sb.append(colToString(game.getEnPassantColumn()));
            sb.append(epRow);
        } else {
            sb.append("-");
        }

        sb.append(" 0 1");
        //rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1




        return sb.toString();
    }

    private static String colToString(int col) {
        switch(col) {
            case 1:
                return "a";
            case 2:
                return "b";
            case 3:
                return "c";
            case 4:
                return "d";
            case 5:
                return "e";
            case 6:
                return "f";
            case 7:
                return "g";
            case 8: 
                return "h";
            default:
                return "a";
        }
    }

    private static String pieceToString(ChessPiece piece) {
        if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
            switch (piece.getPieceType()) {
                case PAWN:
                    return "P";
                case KNIGHT:
                    return "N";
                case BISHOP:
                    return "B";
                case ROOK:
                    return "R";
                case QUEEN:
                    return "Q";
                default:
                    return "K";
            }
        } else {
            switch (piece.getPieceType()) {
                case PAWN:
                    return "p";
                case KNIGHT:
                    return "n";
                case BISHOP:
                    return "b";
                case ROOK:
                    return "r";
                case QUEEN:
                    return "q";
                default:
                    return "k";
            }
        }
    }
}
