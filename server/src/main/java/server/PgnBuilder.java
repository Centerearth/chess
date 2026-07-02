package server;

import model.GameData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PgnBuilder {

    public static String build(GameData gameData) {
        String result = gameData.result() == null ? "*" : gameData.result();

        StringBuilder pgn = new StringBuilder();
        pgn.append("[Event \"").append(escape(gameData.gameName())).append("\"]\n");
        pgn.append("[Site \"Chess Web\"]\n");
        pgn.append("[Date \"").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))).append("\"]\n");
        pgn.append("[White \"").append(escape(playerName(gameData.whiteUsername()))).append("\"]\n");
        pgn.append("[Black \"").append(escape(playerName(gameData.blackUsername()))).append("\"]\n");
        pgn.append("[Result \"").append(result).append("\"]\n\n");

        List<String> moves = gameData.moveHistorySafe();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) {
                pgn.append(i / 2 + 1).append(". ");
            }
            pgn.append(moves.get(i)).append(" ");
        }
        pgn.append(result).append("\n");
        return pgn.toString();
    }

    private static String playerName(String username) {
        if (username == null) {
            return "?";
        }
        return switch (username) {
            case "ai" -> "AI (alpha-beta)";
            case "ml" -> "AI (neural net)";
            default -> username;
        };
    }

    private static String escape(String value) {
        return value == null ? "?" : value.replace("\"", "'");
    }
}
