package client;

import chess.*;
import serverfacade.ServerFacadeMain;
import serverfacade.WebsocketFacade;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.*;

import static ui.EscapeSequences.*;

public class ChessClient implements ServerMessageObserver {
    private final ServerFacadeMain server;
    private State state = State.LOGGEDOUT;
    private GameplayState gameplayState = GameplayState.NOGAMEPLAY;
    private ObservingState observingState = ObservingState.NOTOBSERVING;
    private final WebsocketFacade websocketFacade;
    private ChessGame.TeamColor teamColor = ChessGame.TeamColor.WHITE;
    private ChessGame game;
    private int number;

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
                } else if (observingState == ObservingState.OBSERVING ){
                    return switch (cmd) {
                        case "legalmoves" -> legalMoves(params);
                        case "redraw" -> {displayGameMechanics(game.getBoard(), null); yield "Here is the redrawn board";}
                        case "leave" -> leave();
                        case "logout" -> logout(); //change these two here
                        default -> help();
                    };
                } else {
                    return switch (cmd) {
                        case "legalmoves" -> legalMoves(params);
                        case "redraw" -> {displayGameMechanics(game.getBoard(), null); yield "Here is the redrawn board";}
                        case "leave" -> leave();
                        case "move" -> makeMove(params);
                        case "logout" -> logout();
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
        try {
            if (params.length == 2) {
                int number = Integer.parseInt(params[0]);
                this.number = number;
                String color = params[1];
                if (Objects.equals(color, "WHITE") || Objects.equals(color, "white")) {
                    String response = server.playGame(number, "WHITE");
                    if (Objects.equals(response, "User joined successfully.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username(), color);
                        gameplayState = GameplayState.INGAMEPLAY;
                    }
                    return response;
                } else if (Objects.equals(color, "BLACK") || Objects.equals(color, "black")) {
                    String response = server.playGame(number, "BLACK");
                    if (Objects.equals(response, "User joined successfully.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username(), color);
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
            return "Failed to join.";
        }
    }

    private String observe(String... params) {
        try {
            if (params.length == 1) {
                try {
                    int number = Integer.parseInt(params[0]);
                    this.number = number;
                    String response = server.observeGame(number);

                    if (Objects.equals(response, "Game is being observed.")) {
                        websocketFacade.connect(server.getAuth().authToken(), server.getGameID(number), server.getAuth().username(), "observer");
                        gameplayState = GameplayState.INGAMEPLAY;
                        observingState = ObservingState.OBSERVING;
                        teamColor = ChessGame.TeamColor.WHITE;
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
        this.game = game;

        displayGameMechanics(gameBoard, null);
        printPrompt();
    }

    public void displayGameMechanics (ChessBoard gameBoard, ArrayList<ChessPosition> validPositions) {
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
                    printSquare(gameBoard, 9-i, j, validPositions);
                } else {
                    printSquare(gameBoard, i, 9-j, validPositions);
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

    private void printSquare(ChessBoard gameBoard, int i, int j, ArrayList<ChessPosition> validPositions) {
        if (validPositions != null && validPositions.contains(new ChessPosition(i, j))) {
            System.out.print(SET_BG_COLOR_YELLOW);
        } else if (((i+j) % 2) != 0 ) {
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

    public String legalMoves (String... params) {
        try {
            if (params.length == 1) {
                try {
                    int column = letterToNumber(params[0].substring(0,1));
                    int row = Integer.parseInt(params[0].substring(1,2));
                    System.out.println("" + row + column);
                    ArrayList<ChessPosition> allEndPositions = new ArrayList<>();
                    ArrayList<ChessMove> validMoves = (ArrayList<ChessMove>) game.validMoves(new ChessPosition(row, column));
                    for (ChessMove move : validMoves) {
                        allEndPositions.add(move.getEndPosition());
                    }

                    displayGameMechanics(game.getBoard(), allEndPositions);

                    return "Here are the valid moves";

                } catch (Exception e) {
                    return "Request is malformed";
                }
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to highlight legal moves";
        }
    }

    private int letterToNumber(String letter) throws Exception {
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

    private ChessPiece.PieceType letterToPromotion(String letter) throws Exception {
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

    public String leave() {
        try {
            if (observingState == ObservingState.OBSERVING) {
                websocketFacade.leave(server.getAuth().authToken(), server.getGameID(this.number), server.getAuth().username(), "observer");
                observingState = ObservingState.NOTOBSERVING;
                gameplayState = GameplayState.NOGAMEPLAY;
                return "User stopped observing the game.";
            } else {
                websocketFacade.leave(server.getAuth().authToken(), server.getGameID(this.number), server.getAuth().username(), teamColor.toString().toLowerCase());
                gameplayState = GameplayState.NOGAMEPLAY;
                return "User has left the game";
            }
        } catch (Exception e) {
            return "Failed to leave";
        }
    }

    public String makeMove (String... params) {
        try {
            if (params.length == 2 || params.length == 3) {
                try {
                    int column1 = letterToNumber(params[0].substring(0,1));
                    int row1 = Integer.parseInt(params[0].substring(1,2));

                    int column2 = letterToNumber(params[1].substring(0,1));
                    int row2 = Integer.parseInt(params[1].substring(1,2));

                    ChessPiece.PieceType promotionPiece = null;
                    if (params.length == 3) {
                        promotionPiece = letterToPromotion(params[2].substring(0,1));
                    }

                    ChessPosition startPosition = new ChessPosition(row1, column1);
                    ChessPosition endPosition = new ChessPosition(row2, column2);

                    ArrayList<ChessMove> validMoves = (ArrayList<ChessMove>) game.validMoves(startPosition);
                    ArrayList<ChessPosition> allEndPositions = new ArrayList<>();
                    for (ChessMove move : validMoves) {
                        allEndPositions.add(move.getEndPosition());
                    }

                    if (!allEndPositions.contains(endPosition)) {
                        return "Not a valid move";
                    } else {
                        ChessMove chessMove = new ChessMove(startPosition, endPosition, promotionPiece);
                        websocketFacade.makeMove(server.getAuth().authToken(), server.getGameID(this.number),
                                server.getAuth().username(), teamColor.toString().toLowerCase(), chessMove);
                        return "Move successful";
                    }

                } catch (Exception e) {
                    return "Request is malformed";
                }
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to make move";
        }
    }
}
