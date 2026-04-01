package client;

import chess.ChessPiece;

public class helperFunctions {
    public static String help(State state, ObservingState observingState, GameplayState gameplayState) {
        if (state == State.LOGGEDOUT) {
            return """
                    - register <USERNAME> <PASSWORD> <EMAIL> - this will create your account.
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

    public static int letterToNumber(String letter) throws Exception {
        switch (letter) {
            case "a" -> {
                return 1;
            }
            case "b" -> {
                return 2;
            }
            case "c" -> {
                return 3;
            }
            case "d" -> {
                return 4;
            }
            case "e" -> {
                return 5;
            }
            case "f" -> {
                return 6;
            }
            case "g" -> {
                return 7;
            }
            case "h" -> {
                return 8;
            }
            default -> throw new Exception();
        }
    }

    public static ChessPiece.PieceType letterToPromotion(String letter) throws Exception {
        switch (letter) {
            case "q" -> {
                return ChessPiece.PieceType.QUEEN;
            }
            case "r" -> {
                return ChessPiece.PieceType.ROOK;
            }
            case "b" -> {
                return ChessPiece.PieceType.BISHOP;
            }
            case "n" -> {
                return ChessPiece.PieceType.KNIGHT;
            }
            default -> throw new Exception();
        }
    }
}
