Extra prompts - stress test
go file by file and do clean up
stress test it

After:
Write a website that provides a GUI for the chess game
deploy?


---

## Code Review (2026-04-01)

### General Architecture
- 3-module Maven project: shared (chess engine + WebSocket protocol), server (Javalin HTTP + WebSocket + MySQL), client (CLI REPL)
- REST for auth/game management, WebSocket for real-time gameplay
- Java 21, Javalin 6, Gson, BCrypt, Jakarta WebSocket, MySQL






private void displayBoard(String color) {
String[] rowLabels = {"a", "b", "c", "d", "e", "f", "g", "h"};
String[] pieces = {"R", "N", "B", "Q", "K", "B", "N", "R"};
String[] columnLabels = {" ", "1", "2", "3", "4", "5", "6", "7", "8", " "};
String opposingColor = "BLACK";

        if (Objects.equals(color, "BLACK")) {
            pieces = new String[]{"R", "N", "B", "K", "Q", "B", "N", "R"};
            opposingColor = "WHITE";
            rowLabels = new String[]{"h", "g", "f", "e", "d", "c", "b", "a"};
            //columnLabels = new String[]{" ", "8", "7", "6", "5", "4", "3", "2", "1", " "};
        }

        if (Objects.equals(color, "WHITE")) {
            columnLabels = new String[]{" ", "8", "7", "6", "5", "4", "3", "2", "1", " "};
        }

        String[] pawns = {"P","P","P","P","P","P","P","P"};
        String[] empty = {" "," "," "," "," "," "," "," ",};

        String[][] orderToPrint = new String[][]{rowLabels, pieces, pawns, empty, empty, empty, empty,
                pawns, pieces, rowLabels};
        String[] orderTeamColor = new String[]{"GRAY", opposingColor, opposingColor, "WHITE","WHITE",
                "WHITE","WHITE", color, color, "GRAY"};

        String startingColor = "GRAY";

        for (int i = 0; i < 10; i ++) {
            System.out.print("\u001b[30;100m");
            System.out.printf(" %s ", columnLabels[i]);
            if (i == 9) {
                startingColor = "GRAY";
            }
            startingColor = displayLine(orderToPrint[i], startingColor, orderTeamColor[i]);
            System.out.print("\u001b[30;100m");
            System.out.printf(" %s ", columnLabels[i]);
            System.out.println("\u001b[39;49m");
        }
        System.out.println("\u001b[39;49m");
        //depends on the team. Could make it JSON compatible
    }

    private String displayLine(String[] pieceSequence, String startingColor, String teamColor) {
        String currentBackgroundColor = startingColor;
        String nextLineStartingColor = "BLACK";
        if (Objects.equals(startingColor, "GRAY")) {
            nextLineStartingColor = "WHITE";
        } else if (Objects.equals(startingColor, "BLACK")) {
            nextLineStartingColor = "WHITE";
        }
        String displayColor;

        if (Objects.equals(teamColor, "BLACK")) {
            displayColor = "32";
        } else if (Objects.equals(teamColor, "WHITE")){
            displayColor = "34";
        } else {
            displayColor = "30";
        }
        for (int i = 0; i < 8; i++) {
            if (Objects.equals(currentBackgroundColor, "WHITE")) {
                System.out.printf("\u001b[%s;107m", displayColor);
                System.out.printf(" %s ", pieceSequence[i]);
                currentBackgroundColor = "BLACK";
            } else if (Objects.equals(currentBackgroundColor, "BLACK")) {
                System.out.printf("\u001b[%s;40m", displayColor);
                System.out.printf(" %s ", pieceSequence[i]);
                currentBackgroundColor = "WHITE";
            } else {
                System.out.printf("\u001b[%s;100m", displayColor);
                System.out.printf(" %s ", pieceSequence[i]);
            }
        }
        return nextLineStartingColor;
    }