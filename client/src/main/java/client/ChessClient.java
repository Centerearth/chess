package client;

import ServerFacade.ServerFacadeMain;

import java.util.Arrays;
import java.util.Objects;
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
            return ex.getMessage(); //change later
        }
    }

    private String register(String... params) {
        try {
            if (params.length == 3) {
                String response = server.registerUser(params[0], params[1], params[2]);
                if (Objects.equals(response, "User was registered successfully.")) {
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
        return "create";
    }

    private String list() {
        return "list";
    }

    private String join(String... params) {
        return "join";
    }

    private String observe(String... params) {
        return "observe";
    }

    private String logout() {
        return "logout";
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
