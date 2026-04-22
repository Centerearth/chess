Plan for phase 6:


Fix rest of issues that are relevant
go file by file and do clean up
join with gameName instead of ID
castling and en passant
stress test it
What would upgrading to java 25 look like?

After:
Write a website that provides a GUI for the chess game
Redeploy


---

## Code Review (2026-04-01)

### General Architecture
- 3-module Maven project: shared (chess engine + WebSocket protocol), server (Javalin HTTP + WebSocket + MySQL), client (CLI REPL)
- REST for auth/game management, WebSocket for real-time gameplay
- Java 21, Javalin 6, Gson, BCrypt, Jakarta WebSocket, MySQL


### Code Quality Critique (2026-04-01)

**Consistency**
- Null checks have no coherent policy — some places guard, others don't.

**Duplication**
- `tryConnection()` (ChessClient.java:194) has two nearly identical WHITE/BLACK branches — should be one path with a parameter.
- SQL data access classes repeat the same connection boilerplate in every method across three files — needs a shared helper.
- `join()` checks `gamesOver` twice (before and after connecting); one check is always redundant.

**Naming**
- `number` (field and local in join()) — should be `currentGameNumber` or `selectedGameIndex`.
- `gamesOver` — reads as a noun, but is a state map. Should be `endedGameIds` after the bug fix.
- `tryConnection()` — doesn't describe purpose, describes implementation. Should be something like `joinGame()`.
- `HelpCalculator` — vague; it's actually a move bounds validator.

**Method Length and Responsibility**
- `eval()` encodes which commands exist per state directly in its body — adding a new state requires editing it. A map of `String → Command` per state would be more extensible.
- `displayGameMechanics()` mixes board orientation logic with rendering — two separate concerns.

**Dead / Leftover Code**
- Comment at ChessGame.java:260: `// why did IntelliJ say to put this in?` — unresolved, should be removed.

**Error Handling Style**
- Broad `catch (Exception e)` returning fixed strings like `"Failed to join."` hides the actual error. At minimum, surface `e.getMessage()`.

**Also discussed: client-side vs. server-side validation**
- Both are appropriate, but for different reasons: server validation is mandatory (security), client validation is for UX (fast feedback, fewer round trips).
- Don't maintain two separate implementations of the same logic — share via the `shared` module.
- The client-side move check in `makeMove()` is fine as a UX shortcut, but the server check is the one that actually matters.






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