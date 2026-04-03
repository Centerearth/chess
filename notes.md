Plan for phase 6:

go through "problems"
joining after game has ended doesn't always work
After:
implement castling and en passant
Protect against races
deploy to AWS
Take down
Write a website that provides a GUI for the chess game
Redeploy


---

## Code Review (2026-04-01)

### General Architecture
- 3-module Maven project: shared (chess engine + WebSocket protocol), server (Javalin HTTP + WebSocket + MySQL), client (CLI REPL)
- REST for auth/game management, WebSocket for real-time gameplay
- Java 21, Javalin 6, Gson, BCrypt, Jakarta WebSocket, MySQL

---

### Critical Issues

1. **Board initialization bug** (ChessBoard.java:60-74)
   Comments say "adding bishops" but code places KNIGHTs, and vice versa. Every game starts with illegal piece positions.

2. **Weak game ID generation** (GameService.java:25-28)
   Uses `Random.nextInt(500)` — collisions after ~30 games. Should use UUID or DB auto-increment.

3. **Race condition in client state** (ChessClient.java:15-24)
   Fields like `whoseTurn`, `teamColor`, `gameplayState`, `gamesOver` are mutated by the WebSocket thread and read by the REPL thread with no synchronization. Need `volatile` or `synchronized`.

4. **SQL result set not checked** (all SQL*DataAccess files)
   `rs.next()` is never checked before reading — silent `SQLException` if no row found. Should return `Optional` or check the result.

5. **Inefficient game updates** (SQLGameDataAccess.java:102-141)
   Every move does read → deserialize → delete → re-insert (3 DB round trips). Should use SQL UPDATE instead.

6. **Duplicate GameService instances** (WebsocketHandler.java:32)
   WebsocketHandler creates its own `new GameService(...)` separate from the one Server.java uses. Server should construct one and inject it into both.

---

### Major Issues

7. **`gamesOver` keyed by list position, not game ID** (ChessClient.java:23, 180, 230, 299-303) ← **ROOT CAUSE OF "game already ended" BUG**
   `gamesOver` maps list position → true when a game ends. List positions are not stable across `list` calls. A new game appearing at the same list slot incorrectly shows as "already ended."
   Fix: key by actual game ID (`server.getGameID(this.number)`) on both the write side (displayGame) and read side (join/observe).

8. **Unvalidated user input crashes client** (ChessClient.java:392-393)
   `substring(0,1)` and `substring(1,2)` with no length check — typing a short string throws `StringIndexOutOfBoundsException`.

9. **Pawn promotion not validated** (WebsocketHandler.java:160)
   `move.getPromotionPiece()` passed through unchecked — client could send KING or PAWN as promotion piece.

10. **`printStackTrace()` swallows errors** (WebsocketHandler.java:115, ConnectionManager.java:71, others)
    No error is sent back to the client; silent failures. Should send `ErrorMessage` over WebSocket.

11. **`ServerFacadeMain` uses static mutable state** (ServerFacadeMain.java:16-17)
    `static AuthData authData` and `static HashMap<Integer, Integer> idToNumber` — instance fields are the right tool.

12. **Redundant/broken move validation** (WebsocketHandler.java:144-152)
    Checks `piece.getTeamColor() != teamColor` twice. One is dead code. Also `gameService.getColor()` can return null with no null check.

---

### Minor Issues

13. Debug `System.out.println` in production (SQLGameDataAccess.java:161,164; WebsocketHandler.java:39,64) — should use SLF4J (already a dependency).
14. Magic numbers throughout move calculators and board scanning — define `BOARD_MIN = 1`, `BOARD_MAX = 8` constants.
15. `(int)(double)` cast hack for Gson JSON numbers (ServerFacadeMain.java:148) — use `((Number) obj).intValue()`.
16. Test helper methods `resetAuth()` / `resetIds()` exposed in public API (ServerFacadeMain.java:36-42).
17. Stray question comment in production code (ChessGame.java:260): `// why did IntelliJ say to put this in ?`
18. `idToNumber` mapping is fragile — list positions shift when games are added; downstream code that relies on stable positions will misbehave.

---

### Code Quality Critique (2026-04-01)

**Consistency**
- String comparison style varies throughout: `.equals()`, `Objects.equals()`, and `==` used interchangeably. Pick `.equals()` as the default; only use `Objects.equals()` when the left side may be null.
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
- `notes.md` contains old `displayBoard()` / `displayLine()` code that was replaced and never removed.
- `resetAuth()` and `resetIds()` in ServerFacadeMain are labeled "for testing" but ship in the production class.
- Comment at ChessGame.java:260: `// why did IntelliJ say to put this in?` — unresolved, should be removed.

**Error Handling Style**
- Broad `catch (Exception e)` returning fixed strings like `"Failed to join."` hides the actual error. At minimum, surface `e.getMessage()`.

**Also discussed: client-side vs. server-side validation**
- Both are appropriate, but for different reasons: server validation is mandatory (security), client validation is for UX (fast feedback, fewer round trips).
- Don't maintain two separate implementations of the same logic — share via the `shared` module.
- The client-side move check in `makeMove()` is fine as a UX shortcut, but the server check is the one that actually matters.

---

### Additional Bugs Found (2026-04-01)

1. **Check notifications backwards** (WebsocketHandler.java:177-182)
   `isInCheck(WHITE)` means WHITE's king is threatened, but the notification says "put black in check." Both branches have the color flipped. Simple string fix.

2. **Stalemate doesn't end the game** (WebsocketHandler.java:183-188)
   Checkmate calls `gameService.updateGameWin(gameID)`. Stalemate sends the notification but never calls it — the game stays open and players can keep making moves after stalemate.

3. **NPE when moving from empty square** (WebsocketHandler.java:137-140)
   `game.validMoves()` returns `null` when there's no piece at the start position (ChessGame.java:56-58). The server then iterates over `null` → NullPointerException, swallowed by the catch block into a generic error.

---

### Prioritized Fix List
1. Fix board init bug (ChessBoard.java:60-74)
2. Fix game ID generation (GameService.java:25-28)
3. Fix stalemate not ending game (WebsocketHandler.java:183-188) — add `updateGameWin` call
4. Fix check notification strings being backwards (WebsocketHandler.java:177-182)
5. Fix `gamesOver` keying bug (ChessClient.java) — use actual game ID as key
6. Guard against null from `validMoves()` before iterating (WebsocketHandler.java:137)
7. Add `rs.next()` checks in all SQL getX() methods
8. Synchronize shared client state
9. Consolidate GameService to one shared instance
10. Validate user input before substring() calls
11. Clean up println/printStackTrace, magic numbers, dead code




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