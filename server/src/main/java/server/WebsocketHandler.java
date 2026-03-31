package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsMessageHandler;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Objects;

import static websocket.messages.ServerMessage.ServerMessageType.*;


public class WebsocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private final ConnectionManager allConnections = new ConnectionManager();
    private final GameService gameService = new GameService();

    public WebsocketHandler() throws DataAccessException {
    }

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("Websocket connected"); //probably should delete this but need some visual confirmation for now
        ctx.enableAutomaticPings();
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        try {
        UserGameCommand userGameCommand = new Gson().fromJson(ctx.message(), UserGameCommand.class);
            switch (userGameCommand.getCommandType()) {
                case CONNECT -> connect(userGameCommand, ctx.session);
                case MAKE_MOVE -> {
                    MakeMoveCommand makeMoveCommand = new Gson().fromJson(ctx.message(), MakeMoveCommand.class);
                    makeMove(makeMoveCommand, ctx.session);
                }
                case LEAVE -> leave(userGameCommand, ctx.session);
                case RESIGN -> resign(userGameCommand, ctx.session);
            }

        } catch (IOException ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed"); //same thing here? delete?
    }

    private void connect(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
            if (gameService.isGameWon(userGameCommand.getGameID())) {
                allConnections.add(userGameCommand.getGameID(), session);

                GameData gameData = gameService.getGame(userGameCommand.getGameID());
                LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData.game(),
                        gameData.whoseTurn(), gameData.gameOver());
                allConnections.broadcastOne(session, loadGameMessage, gameData.gameID());

                String notification = "This game has already ended";
                NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);
                allConnections.broadcastOne(session, notificationMessage, userGameCommand.getGameID());
                allConnections.removeSession(userGameCommand.getGameID(), session);
                return;
            }
        allConnections.add(userGameCommand.getGameID(), session);

        GameData gameData = gameService.getGame(userGameCommand.getGameID());
        ChessGame game = gameData.game();
        LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, game,
                gameData.whoseTurn(), gameData.gameOver());

        String notification = String.format("%s has joined the game as %s", userGameCommand.getUsername(), userGameCommand.getColor());

        NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

        allConnections.broadcastOne(session, loadGameMessage, userGameCommand.getGameID());
        allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

        } catch (Exception e) {
            System.out.println(e.getMessage());
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to connect"));
        }

    }

    private void makeMove(MakeMoveCommand makeMoveCommand, Session session) throws IOException {
        try {
            int gameID = makeMoveCommand.getGameID();

            if (gameService.isGameWon(gameID)) {
                String notification = "This game has already ended";
                NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);
                allConnections.broadcastOne(session, notificationMessage, gameID);
                return;
            }

            String username = makeMoveCommand.getUsername();

            ChessGame game = gameService.getGame(gameID).game();

            ChessMove move = makeMoveCommand.getMove();
            ChessPosition startPosition = move.getStartPosition();
            ChessPosition endPosition = move.getEndPosition();
            ChessPiece startingPiece = game.getBoard().getPiece(startPosition);

            if (move.getPromotionPiece() != null) {
                game.getBoard().addPiece(endPosition,
                        new ChessPiece(game.getBoard().getPiece(startPosition).getTeamColor(), move.getPromotionPiece()));
            } else {
                game.getBoard().addPiece(endPosition, startingPiece);
            }
            game.getBoard().addPiece(startPosition, null);

            gameService.updateBoard(gameID, game);

            GameData gameData = gameService.getGame(gameID);

            ChessGame.TeamColor nextTurn = ChessGame.TeamColor.WHITE;
            if (gameData.whoseTurn() == ChessGame.TeamColor.WHITE) {
                nextTurn = ChessGame.TeamColor.BLACK;
            }

            LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME,
                    gameData.game(), nextTurn, gameData.gameOver());
            allConnections.broadcastAll(loadGameMessage, gameID);

            String notification = String.format("%s has made the move %s", username, makeMoveCommand.getMove().toString());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);
            allConnections.broadcastSome(session, notificationMessage, gameID);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to make move"));
        }
    }
    private void leave(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
            String notification = String.format("%s has left the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

            allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            ChessGame.TeamColor teamColor = ChessGame.TeamColor.BLACK;
            if (Objects.equals(userGameCommand.getColor(), "white")) {
                teamColor = ChessGame.TeamColor.WHITE;
            }

            gameService.updateGame(teamColor, userGameCommand.getGameID(), null);
            allConnections.removeSession(userGameCommand.getGameID(), session);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to leave"));
        }
    }

    private void resign(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
            String notification = String.format("%s has resigned from the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

            allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            gameService.updateGameWin(userGameCommand.getGameID());

            GameData gameData = gameService.getGame(userGameCommand.getGameID());
            LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME,
                    gameData.game(), gameData.whoseTurn(), gameData.gameOver());
            allConnections.broadcastAll(loadGameMessage, userGameCommand.getGameID());

            allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            allConnections.removeSession(userGameCommand.getGameID(), session);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to resign"));
        }
    }
}