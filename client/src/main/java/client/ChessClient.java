package client;

import chess.*;
import serverfacade.ServerFacadeMain;
import serverfacade.WebsocketFacade;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import static ui.EscapeSequences.*;

public class ChessClient implements ServerMessageObserver {
    private final ServerFacadeMain server;
    private volatile State state = State.LOGGEDOUT;
    private volatile GameplayState gameplayState = GameplayState.NOGAMEPLAY;
    private volatile ObservingState observingState = ObservingState.NOTOBSERVING;
    private final WebsocketFacade websocketFacade;
    private volatile ChessGame.TeamColor teamColor = ChessGame.TeamColor.WHITE;
    private volatile ChessGame game;
    private volatile int currentId;
    private volatile ChessGame.TeamColor whoseTurn;
    private final HashMap<Integer, Boolean> gamesOver = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private volatile boolean suppressNextMainPrompt = false;

    public ChessClient(String serverUrl) throws Exception {
        server = new ServerFacadeMain(serverUrl);
        websocketFacade = new WebsocketFacade(serverUrl, this);
    }

    public void run() {
        System.out.println("Welcome to the chess application. Sign in to start.");
        System.out.println("Note - application is not case sensitive.");
        System.out.print(help());

        var result = "";
        while (!result.equals("quit")) {
            printMainLoopPrompt();
            String line = scanner.nextLine();

            try {
                result = eval(line);
                System.out.print(result);
            } catch (Throwable e) {
                var msg = e.toString();
                System.out.print(msg);
            }
        }
        scanner.close();
        System.out.println();
    }

    private void printMainLoopPrompt() {
        if (suppressNextMainPrompt) {
            suppressNextMainPrompt = false;
            return;
        }
        printPrompt();
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
                        case "redraw" -> reDraw();
                        case "leave" -> leave();
                        default -> help();
                    };
                } else {
                    return switch (cmd) {
                        case "legalmoves" -> legalMoves(params);
                        case "redraw" -> reDraw();
                        case "leave" -> leave();
                        case "move" -> makeMove(params);
                        case "resign" -> resignHelper();
                        default -> help();
                    };
                }
            }
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String resignHelper() {
        System.out.print("Are you sure you want to resign? Type y for yes: ");
        String confirm = scanner.nextLine();
        if (confirm.equals("y") || confirm.equals("yes")) {
            System.out.println();
            return resign();
        } else {
            return "The user did not resign";
        }
    }

    private String reDraw() {
        displayGameMechanics(game.getBoard(), null);
        return "Here is the redrawn board";
    }

    private String register(String... params) {
        try {
            if (params.length == 3) {
                String response = server.registerUser(params[0], params[1], params[2]);
                if (response.equals("User was registered successfully. User was logged in successfully.")) {
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
                if (response.equals("User was logged in successfully.")) {
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
                String gameName = params[0];
                Integer gameId = server.getGameId(gameName);
                if (gameId == null) {
                    return "Game does not exist.";
                }
                if (gamesOver.get(gameId) != null && gamesOver.get(gameId)) {
                    return "Game has already ended";
                }
                this.currentId = gameId;
                String color = params[1];
                return tryConnection(color);
            } else {
                return "Request is malformed";
            }
        } catch (Exception e) {
            return "Failed to join.";
        }
    }

    private String tryConnection(String color) throws Exception {
        if (color.equals("WHITE") || color.equals("white")) {
            String response = server.playGame(currentId, "WHITE");
            if (response.equals("User joined successfully.")) {
                websocketFacade.connect(server.getAuth().authToken(), currentId,
                        server.getAuth().username(), color);

                suppressNextMainPrompt = true;
                if (gamesOver.get(currentId) != null && gamesOver.get(currentId)) {
                    return "Game has already ended";
                }
                gameplayState = GameplayState.INGAMEPLAY;
                teamColor = ChessGame.TeamColor.WHITE;
                return response;
            }
            return response;
        } else if (color.equals("BLACK") || color.equals("black")) {
            String response = server.playGame(currentId, "BLACK");
            if (response.equals("User joined successfully.")) {
                websocketFacade.connect(server.getAuth().authToken(), currentId,
                        server.getAuth().username(), color);

                suppressNextMainPrompt = true;
                if (gamesOver.get(currentId) != null && gamesOver.get(currentId)) {
                    return "Game has already ended";
                }
                gameplayState = GameplayState.INGAMEPLAY;
                teamColor = ChessGame.TeamColor.BLACK;
                suppressNextMainPrompt = true;
                return response;
            }
            return response;
        } else {
            return "Request is malformed";
        }
    }

    private String observe(String... params) {
        try {
            if (params.length == 1) {
                String gameName = params[0];
                Integer gameId = server.getGameId(gameName);
                if (gameId == null) {
                    return "Game does not exist.";
                }
                if (gamesOver.get(gameId) != null && gamesOver.get(gameId)) {
                    return "Game has already ended";
                }
                this.currentId = gameId;
                String response = server.observeGame(currentId);

                if (response.equals("Game is being observed.")) {
                    websocketFacade.connect(server.getAuth().authToken(), currentId, server.getAuth().username(), "observer");

                    if (gamesOver.get(currentId) != null && gamesOver.get(currentId)) {
                        return "Game has already ended";
                    }
                    gameplayState = GameplayState.INGAMEPLAY;
                    observingState = ObservingState.OBSERVING;
                    teamColor = ChessGame.TeamColor.WHITE;
                    suppressNextMainPrompt = true;
                    return "Observing game...";
                } else {
                    return response;
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
        return HelperFunctions.help(state, observingState, gameplayState);
    }

    @Override
    public void notify(NotificationMessage notificationMessage) {
        System.out.println();
        System.out.println(notificationMessage.getMessage());
        printPrompt();
    }

    @Override
    public void notifyError(ErrorMessage errorMessage) {
        System.out.println();
        System.out.println(errorMessage.getErrorMessage());
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
        this.whoseTurn = game.getTeamTurn();
        displayGameMechanics(gameBoard, null);
        if (loadGameMessage.getGameOver()) {
            this.gamesOver.put(currentId, true);
            observingState = ObservingState.NOTOBSERVING;
            gameplayState = GameplayState.NOGAMEPLAY;
        }
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
        return HelperFunctions.letterToNumber(letter);
    }
    private ChessPiece.PieceType letterToPromotion(String letter) throws Exception {
        return HelperFunctions.letterToPromotion(letter);
    }

    public String leave() {
        try {
            if (observingState == ObservingState.OBSERVING) {
                websocketFacade.leave(server.getAuth().authToken(), this.currentId, server.getAuth().username(), "observer");
                observingState = ObservingState.NOTOBSERVING;
                gameplayState = GameplayState.NOGAMEPLAY;
                return "User stopped observing the game.";
            } else {
                websocketFacade.leave(server.getAuth().authToken(), this.currentId,
                        server.getAuth().username(), teamColor.toString().toLowerCase());
                gameplayState = GameplayState.NOGAMEPLAY;
                return "User has left the game";
            }
        } catch (Exception e) {
            return "Failed to leave";
        }
    }
    public String makeMove (String... params) {
        try {
            if (this.teamColor != this.whoseTurn) {
                return "ERROR: Not your turn";
            }
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

                    if (!allEndPositions.contains(endPosition) ||
                            game.getBoard().getPiece(startPosition).getTeamColor() != teamColor) {
                        return "ERROR: Not a valid move";
                    } else {
                        ChessMove chessMove = new ChessMove(startPosition, endPosition, promotionPiece);
                        websocketFacade.makeMove(server.getAuth().authToken(), this.currentId,
                                server.getAuth().username(), teamColor.toString().toLowerCase(), chessMove);

                        suppressNextMainPrompt = true;
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
    public String resign() {
        try {
                websocketFacade.resign(server.getAuth().authToken(), this.currentId,
                        server.getAuth().username(), teamColor.toString().toLowerCase());
                gameplayState = GameplayState.NOGAMEPLAY;
                return "User has resigned from the game";
        } catch (Exception e) {
            return "Failed to resign";
        }
    }
}
