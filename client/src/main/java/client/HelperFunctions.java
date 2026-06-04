package client;

import chess.ChessPiece;

public class HelperFunctions {
    public static String help(State state, ObservingState observingState, GameplayState gameplayState) {
        if (state == State.LOGGEDOUT) {
            return """
                    - register <USERNAME> <PASSWORD> - this will create your account.
                    - login <USERNAME> <PASSWORD>
                    - quit
                    - help - will list all available commands.
                    """;
        } else if (observingState == ObservingState.OBSERVING) {
            return """
                    - legalmoves <position> - highlight all legal moves
                    - leave
                    - redraw
                    - help - will list all available commands.
                    """;
        } else if (gameplayState == GameplayState.NOGAMEPLAY) {
            return """
                    - create <NAME> - this will start a new game.
                    - list - this will list all games.
                    - join <ID> [WHITE|BLACK]
                    - observe <ID>
                    - logout
                    - quit
                    - help - will list all available commands.
                    """;
        } else {
            return """
                    - move <starting position> <end position> <optional:promotion piece (q,r,n,b)
                    - legalmoves <position> - highlight all legal moves
                    - redraw
                    - leave
                    - resign
                    - help - will list all available commands.
                    """;
        }
    }

    public static int fileToColumn(String letter) throws Exception {
        return switch (letter) {
            case "a" -> 1;
            case "b" -> 2;
            case "c" -> 3;
            case "d" -> 4;
            case "e" -> 5;
            case "f" -> 6;
            case "g" -> 7;
            case "h" -> 8;
            default -> throw new Exception();
        };
    }

    public static ChessPiece.PieceType charToPromotionPiece(String letter) throws Exception {
        return switch (letter) {
            case "q" -> ChessPiece.PieceType.QUEEN;
            case "r" -> ChessPiece.PieceType.ROOK;
            case "b" -> ChessPiece.PieceType.BISHOP;
            case "n" -> ChessPiece.PieceType.KNIGHT;
            default -> throw new Exception();
        };
    }
}
