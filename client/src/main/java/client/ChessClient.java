package client;

import ServerFacade.ServerFacadeMain;

import java.util.Arrays;
import java.util.Scanner;

public class ChessClient {
    private final ServerFacadeMain server;
    private State state = State.LOGGEDOUT;

    public ChessClient(String serverUrl) {
        server = new ServerFacadeMain(serverUrl);
    }

    public void run() {
        System.out.println("Welcome to the chess application. Sign in to start.");
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

                    default -> help();
                };
            }
        } catch (Exception ex) {
            return ex.getMessage(); //change later
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
}
