package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.InvalidMoveException;


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
import websocket.commands.*;
import websocket.messages.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static websocket.messages.ServerMessage.ServerMessageType.*;


public class WebsocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandler.class);

    private final ConnectionManager allConnections = new ConnectionManager();
    private final GameService gameService;

    public WebsocketHandler(GameService gameService) throws DataAccessException {
        this.gameService = gameService;
    }

    @Override
    public void handleConnect(WsConnectContext ctx) {
        logger.info("WebSocket connected");
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

        } catch (IOException ex) {
            logger.error("WebSocket message handling failed", ex);
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        logger.info("WebSocket closed");
    }

    private void connect(UserGameCommand userGameCommand, Session session) throws IOException {
        try {

            if (!gameService.authDataExists(userGameCommand.getAuthToken())) {
                allConnections.add(userGameCommand.getGameID(), session);
                String message = "ERROR: User is not authorized";
                ErrorMessage errorMessage = new ErrorMessage(ERROR, message);
                allConnections.broadcastError(session, errorMessage);
                allConnections.removeSession(userGameCommand.getGameID(), session);
                return;
            }
            if (gameService.getGame(userGameCommand.getGameID()) == null) {
                allConnections.add(userGameCommand.getGameID(), session);
                String message = "ERROR: Game ID is invalid";
                ErrorMessage errorMessage = new ErrorMessage(ERROR, message);
                allConnections.broadcastError(session, errorMessage);
                allConnections.removeSession(userGameCommand.getGameID(), session);
                return;
            }
            if (gameService.isGameWon(userGameCommand.getGameID())) {
                allConnections.add(userGameCommand.getGameID(), session);

                GameData gameData = gameService.getGame(userGameCommand.getGameID());
                LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData.game(),
                        gameData.gameOver());
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
                gameData.gameOver());

        String notification = String.format("%s has joined the game as %s", userGameCommand.getUsername(), userGameCommand.getColor());

        NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

        allConnections.broadcastOne(session, loadGameMessage, userGameCommand.getGameID());
        allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

        } catch (Exception e) {
            logger.error("Failed to connect to game", e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to connect"));
        }

    }

    private void makeMove(MakeMoveCommand makeMoveCommand, Session session) throws IOException {
        int gameID = makeMoveCommand.getGameID();
        try {
            NotificationMessage notificationMessage2;
            LoadGameMessage loadGameMessage;
            NotificationMessage notificationMessage;
            boolean gameWon;

            synchronized (gameService.getLock(gameID)) {
                if (gameService.isGameWon(gameID)) {
                    allConnections.broadcastOne(session, new ErrorMessage(ERROR, "This game has already ended."), gameID);
                    return;
                }

                String username = gameService.getAuthData(makeMoveCommand.getAuthToken()).username();
                ChessGame game = gameService.getGame(gameID).game();
                ChessMove move = makeMoveCommand.getMove();

                GameData gameData = gameService.getGame(gameID);
                ChessGame.TeamColor playerColor = gameService.getColor(username, gameID);
                if (playerColor == null) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "ERROR: Observers cannot make moves"));
                    return;
                }

                ChessPiece startingPiece = game.getBoard().getPiece(move.getStartPosition());
                if (startingPiece != null && startingPiece.getTeamColor() != playerColor) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "ERROR: You cannot move your opponent's pieces"));
                    return;
                }

                try {
                    game.makeMove(move);
                } catch (InvalidMoveException e) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, String.format("ERROR: Invalid move, %s", e.getMessage())));
                    return;
                }
                gameService.updateBoard(gameID, game);

                notificationMessage2 = null;
                if (game.isInCheckmate(ChessGame.TeamColor.WHITE)) {
                    notificationMessage2 = new NotificationMessage(NOTIFICATION, String.format("%s has checkmated white", username));
                    gameService.updateGameWin(gameID);
                } else if (game.isInCheckmate(ChessGame.TeamColor.BLACK)) {
                    notificationMessage2 = new NotificationMessage(NOTIFICATION, String.format("%s has checkmated black", username));
                    gameService.updateGameWin(gameID);
                } else if (game.isInCheck(ChessGame.TeamColor.WHITE)) {
                    notificationMessage2 = new NotificationMessage(NOTIFICATION, String.format("%s has put white in check", username));
                } else if (game.isInCheck(ChessGame.TeamColor.BLACK)) {
                    notificationMessage2 = new NotificationMessage(NOTIFICATION, String.format("%s has put black in check", username));
                } else if (game.isInStalemate(ChessGame.TeamColor.WHITE) || game.isInStalemate(ChessGame.TeamColor.BLACK)) {
                    notificationMessage2 = new NotificationMessage(NOTIFICATION, String.format("%s has put the game in stalemate", username));
                    gameService.updateGameWin(gameID);
                }

                gameData = gameService.getGame(gameID);

                loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData.game(), gameData.gameOver());
                notificationMessage = new NotificationMessage(NOTIFICATION,
                        String.format("%s has made the move %s", username, makeMoveCommand.getMove().toString()));
                gameWon = gameService.isGameWon(gameID);
            }

            allConnections.broadcastAll(loadGameMessage, gameID);
            allConnections.broadcastSome(session, notificationMessage, gameID);
            if (notificationMessage2 != null) {
                allConnections.broadcastAll(notificationMessage2, gameID);
            }
            if (gameWon) {
                allConnections.remove(gameID);
            }

        } catch (Exception e) {
            logger.error("Failed to make move in game {}", gameID, e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: failed to make move"));
        }
    }
    private void leave(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
            String notification = String.format("%s has left the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

            //allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            String username = gameService.getAuthData(userGameCommand.getAuthToken()).username();
            ChessGame.TeamColor teamColor = gameService.getColor(username, userGameCommand.getGameID());

            if (Objects.equals(teamColor, ChessGame.TeamColor.WHITE)) {
                gameService.updateGame(ChessGame.TeamColor.WHITE, userGameCommand.getGameID(), null);
            } else if (Objects.equals(teamColor, ChessGame.TeamColor.BLACK)) {
                gameService.updateGame(ChessGame.TeamColor.BLACK, userGameCommand.getGameID(), null);
            }

            allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            allConnections.removeSession(userGameCommand.getGameID(), session);

        } catch (Exception e) {
            logger.error("Failed to leave game", e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to leave"));
        }
    }

    private void resign(UserGameCommand userGameCommand, Session session) throws IOException {
        int gameID = userGameCommand.getGameID();
        try {
            String username = gameService.getAuthData(userGameCommand.getAuthToken()).username();
            if (username == null || gameService.getColor(username, gameID) == null) {
                allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: cannot resign as observer"));
                return;
            }

            synchronized (gameService.getLock(gameID)) {
                if (gameService.isGameWon(gameID)) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: game is already finished."));
                    return;
                }
                gameService.updateGameWin(gameID);
            }

            String notification = String.format("%s has resigned from the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            allConnections.broadcastAll(new NotificationMessage(NOTIFICATION, notification), gameID);
            allConnections.removeSession(gameID, session);

        } catch (Exception e) {
            logger.error("Failed to resign from game {}", gameID, e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: failed to resign"));
        }
    }
}