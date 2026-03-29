package client;

import serverfacade.ServerFacadeMain;
import serverfacade.WebsocketFacade;
import websocket.messages.ServerMessage;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class ChessClient implements ServerMessageObserver {
    private final ServerFacadeMain server;
    private State state = State.LOGGEDOUT;
    private String username;
    private final WebsocketFacade websocketFacade;

    public ChessClient(String serverUrl) throws Exception {
        server = new ServerFacadeMain(serverUrl);
        websocketFacade = new WebsocketFacade(serverUrl, this);
    }

    public void run() {
        System.out.println("Welcome to the chess application. Sign in to start.");
        System.out.println("Note - application is not case sensitive.");
        System.out.print(help());

        Scanner scanner = new Scanner(System.in);
        var result = "";

        while (!result.equals("quit")) {
            printPrompt();
            String line = scanner.nextLine();

            try {
                result = eval(line);
                System.out.print(result);
            } catch (Throwable e) {
                var msg = e.toString();
                System.out.print(msg);
            }
        }
        System.out.println();
    }

    private void printPrompt() {
        if (state == State.LOGGEDOUT) {
            System.out.print("\n[LOGGED_OUT] >>> ");
        } else {
            System.out.print("\n[LOGGED_IN] >>> ");
        }
    }

    public String eval(String input) {
        try {
            String[] tokens = input.toLowerCase().split(" ");
            String cmd = (tokens.length > 0) ? tokens[0] : "help";
            String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
            if (state == State.LOGGEDOUT) {
                return switch (cmd) {
                    case "register" -> register(params);
                    case "login" -> login(params);
                    case "quit" -> "quit";
                    default -> help();
                };
            } else {
                return switch (cmd) {
                    case "create" -> create(params);
                    case "list" -> list();
                    case "join" -> join(params);
                    case "observe" -> observe(params);
                    case "logout" -> logout();
                    case "quit" -> "quit";
                    default -> help();
                };
            }
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String register(String... params) {
        try {
            if (params.length == 3) {
                String response = server.registerUser(params[0], params[1], params[2]);
                if (Objects.equals(response, "User was registered successfully. User was logged in successfully.")) {
                    state = State.LOGGEDIN;
                }
                return response;
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to register new user.";
        }
    }

    private String login(String... params) {
        try {
            if (params.length == 2) {
                String response = server.loginUser(params[0], params[1]);
                if (Objects.equals(response, "User was logged in successfully.")) {
                    state = State.LOGGEDIN;
                    username = params[0];
                }
                return response;
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to login.";
        }
    }

    private String create(String... params) {
        try {
            if (params.length == 1) {
                return server.createGame(params[0]);
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to create game.";
        }
    }

    private String list() {
        try {
            return server.listGames();
        } catch (Exception e) {
            return "Failed to list games.";
        }
    }

    private String join(String... params) {
        //should initiate a new ws and the new UI
        try {
            if (params.length == 2) {
                int id = Integer.parseInt(params[0]);
                String color = params[1];
                if (Objects.equals(color, "WHITE") || Objects.equals(color, "white")) {
                    String response = server.playGame(id, "WHITE");
                    if (Objects.equals(response, "User joined successfully.")) {
                        displayBoard("WHITE");
                    }
                    return response;
                } else if (Objects.equals(color, "BLACK") || Objects.equals(color, "black")) {
                    String response = server.playGame(id, "BLACK");
                    if (Objects.equals(response, "User joined successfully.")) {
                        displayBoard("BLACK");
                    }
                    return response;
                } else {
                    return "Request is malformed";
                }
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to join.";
        }
    }

    private String observe(String... params) {
        try {
            if (params.length == 1) {
                try {
                    int id = Integer.parseInt(params[0]);
                    String response = server.observeGame(id);
                    if (Objects.equals(response, "Game is being observed.")) {
                        displayBoard("WHITE");
                        return "Observing game...";
                    } else {
                        return response;
                    }
                } catch (Exception e) {
                    return "Request is malformed";
                }
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to observe.";
        }
    }

    private String logout() {
        try {
            String response = server.logoutUser();
            state = State.LOGGEDOUT;
            return response;
        } catch (Exception e) {
            return "Failed to logout";
        }
    }


    public String help() {
        if (state == State.LOGGEDOUT) {
            return """
                    - register <USERNAME> <PASSWORD> <EMAIL> - this will create your account.
                    - login <USERNAME> <PASSWORD>
                    - quit
                    - help - will list all available commands.
                    """;
        }
        return """
                - create <NAME> - this will start a new game.
                - list - this will list all games.
                - join <ID> [WHITE|BLACK]
                - observe <ID>
                - logout
                - quit
                - help - will list all available commands.
                """;
    }

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


    @Override
    public void notify(ServerMessage serverMessage) {

    }
}
