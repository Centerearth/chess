package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import model.AuthData;
import serverfacade.ServerFacadeMain;
import serverfacade.WebsocketFacade;
import websocket.messages.LoadGameMessage;
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
                        displayBoard("WHITE");
                    }
                    return response;
                } else if (Objects.equals(color, "BLACK") || Objects.equals(color, "black")) {
                    String response = server.playGame(number, "BLACK");
                    if (Objects.equals(response, "User joined successfully.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username());
                        gameplayState = GameplayState.INGAMEPLAY;
                        teamColor = ChessGame.TeamColor.BLACK;
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
        } else { //fill in later with each command
            return """ 
                    - quit
                    - help - will list all available commands.
                    """;
        }
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
        System.out.println(serverMessage);
        printPrompt();
        //maybe should change this later?
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
            case null -> {
                System.out.print("   ");
            }
            case ROOK -> {
                System.out.print(" R ");
            }
            case KNIGHT -> {
                System.out.print(" N ");
            }
            case BISHOP -> {
                System.out.print(" B ");
            }
            case QUEEN -> {
                System.out.print(" Q ");
            }
            case KING -> {
                System.out.print(" K ");
            }
            case PAWN -> {
                System.out.print(" P ");
            }
        }
    }
}
