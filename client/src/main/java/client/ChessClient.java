package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import serverfacade.ServerFacadeMain;
import serverfacade.WebsocketFacade;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static ui.EscapeSequences.*;

public class ChessClient implements ServerMessageObserver {
    private final ServerFacadeMain server;
    private State state = State.LOGGEDOUT;
    private GameplayState gameplayState = GameplayState.NOGAMEPLAY;
    private final WebsocketFacade websocketFacade;
    private ChessGame.TeamColor teamColor = ChessGame.TeamColor.WHITE;

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
                if (gameplayState == GameplayState.NOGAMEPLAY) {
                return switch (cmd) {
                    case "create" -> create(params);
                    case "list" -> list();
                    case "join" -> join(params);
                    case "observe" -> observe(params);
                    case "logout" -> logout();
                    case "quit" -> "quit";
                    default -> help();
                };
                } else {
                    return switch (cmd) {
                        case "filler" -> create(params);
                        case "filler2" -> list();
                        case "logout" -> logout();
                        case "quit" -> "quit";
                        default -> help();
                    };
                }
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
        //should initiate the new UI
        try {
            if (params.length == 2) {
                int number = Integer.parseInt(params[0]);
                String color = params[1];
                if (Objects.equals(color, "WHITE") || Objects.equals(color, "white")) {
                    String response = server.playGame(number, "WHITE");
                    if (Objects.equals(response, "User joined successfully.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username());
                        gameplayState = GameplayState.INGAMEPLAY;
                    }
                    return response;
                } else if (Objects.equals(color, "BLACK") || Objects.equals(color, "black")) {
                    String response = server.playGame(number, "BLACK");
                    if (Objects.equals(response, "User joined successfully.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username());
                        gameplayState = GameplayState.INGAMEPLAY;
                        teamColor = ChessGame.TeamColor.BLACK;
                    }
                    return response;
                } else {
                    return "Request is malformed";
                }
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
                        //displayBoard("WHITE");
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
                    - leave
                    - resign
                    - help - will list all available commands.
                    """;
        }
    }

    @Override
    public void notify(NotificationMessage notificationMessage) {
        System.out.println();
        System.out.println(notificationMessage.getNotification());
        printPrompt();
    }

    @Override
    public void notifyError(ErrorMessage errorMessage) {
        System.out.println();
        System.out.println(errorMessage.getMessage());
        printPrompt();
    }

    @Override
    public void notifyDefault(ServerMessage serverMessage) {
        System.out.println();
        System.out.println("Something weird happened");
        printPrompt();
    }

    @Override
    public void displayGame (LoadGameMessage loadGameMessage) {
        ChessGame game = loadGameMessage.getGame();
        ChessBoard gameBoard = game.getBoard();

        String[] rowLabels;
        String[] columnLabels;

        if (teamColor == ChessGame.TeamColor.WHITE) {
            rowLabels = new String[]{"a", "b", "c", "d", "e", "f", "g", "h"};
            columnLabels = new String[]{"8", "7", "6", "5", "4", "3", "2", "1"};
        } else {
            rowLabels = new String[]{"h", "g", "f", "e", "d", "c", "b", "a"};
            columnLabels = new String[]{"1", "2", "3", "4", "5", "6", "7", "8"};
        }

        System.out.println();
        System.out.print(SET_BG_COLOR_LIGHT_GREY);
        System.out.print(SET_TEXT_COLOR_BLACK);

        System.out.print("   ");
        for (String letter : rowLabels) {
            System.out.printf(" %s ", letter);
        }
        System.out.print("   ");
        System.out.println("\u001b[39;49m");

        for (int i = 1; i <= 8; i++) {
            System.out.print(SET_BG_COLOR_LIGHT_GREY);
            System.out.print(SET_TEXT_COLOR_BLACK);
            System.out.printf(" %s ", columnLabels[i-1]);

            for (int j = 1; j <= 8; j++) {
                if (teamColor == ChessGame.TeamColor.WHITE) {
                    printSquare(gameBoard, 9-i, j);
                } else {
                    printSquare(gameBoard, i, 9-j);
                }
            }
            System.out.print(SET_BG_COLOR_LIGHT_GREY);
            System.out.print(SET_TEXT_COLOR_BLACK);
            System.out.printf(" %s ", columnLabels[i-1]);

            System.out.println("\u001b[39;49m");
        }

        System.out.print(SET_BG_COLOR_LIGHT_GREY);
        System.out.print(SET_TEXT_COLOR_BLACK);
        System.out.print("   ");
        for (String letter : rowLabels) {
            System.out.printf(" %s ", letter);
        }
        System.out.print("   ");
        System.out.println("\u001b[39;49m");

        printPrompt();
    }

    private void printSquare(ChessBoard gameBoard, int i, int j) {
        if (((i+j) % 2) != 0 ) {
            System.out.print(SET_BG_COLOR_WHITE);
        } else {
            System.out.print(SET_BG_COLOR_BLACK);
        }

        ChessPiece chessPiece = gameBoard.getPiece(new ChessPosition(i, j));

        ChessPiece.PieceType chessPieceType = null;
        if (chessPiece != null) {
            chessPieceType = chessPiece.getPieceType();
            if (chessPiece.getTeamColor() == ChessGame.TeamColor.WHITE) {
                System.out.print(SET_TEXT_COLOR_BLUE);
            } else {
                System.out.print(SET_TEXT_COLOR_GREEN);
            }
        }

        switch (chessPieceType) {
            case null -> System.out.print("   ");
            case ROOK -> System.out.print(" R ");
            case KNIGHT -> System.out.print(" N ");
            case BISHOP -> System.out.print(" B ");
            case QUEEN -> System.out.print(" Q ");
            case KING -> System.out.print(" K ");
            case PAWN -> System.out.print(" P ");
        }
    }
}
