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
import websocket.commands.*;
import websocket.messages.*;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.ArrayList;
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
        System.out.println("Websocket closed");
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
            System.out.println(username);

            ChessGame game = gameService.getGame(gameID).game();

            ChessMove move = makeMoveCommand.getMove();
            ChessPosition startPosition = move.getStartPosition();
            ChessPosition endPosition = move.getEndPosition();
            ChessPiece startingPiece = game.getBoard().getPiece(startPosition);

            //validation check
            ArrayList<ChessMove> validMoves = (ArrayList<ChessMove>) game.validMoves(startPosition);
            ArrayList<ChessPosition> allEndPositions = new ArrayList<>();
            for (ChessMove eachMove : validMoves) {
                allEndPositions.add(eachMove.getEndPosition());
            }

            GameData gameData = gameService.getGame(gameID);
            ChessGame.TeamColor teamColor = game.getBoard().getPiece(startPosition).getTeamColor();
            if (teamColor != gameData.whoseTurn()) {
                String message = "ERROR: Invalid";
                ErrorMessage errorMessage = new ErrorMessage(ERROR, message);
                allConnections.broadcastError(session, errorMessage);
                return;
            }
            System.out.println(makeMoveCommand.getColor());
            System.out.println(startPosition);
            System.out.println(endPosition);
            System.out.println(teamColor);
            System.out.println(gameService.getColor(username, gameID));

            if (!allEndPositions.contains(endPosition) ||
                    game.getBoard().getPiece(startPosition).getTeamColor() != teamColor
            || game.getBoard().getPiece(startPosition).getTeamColor() != gameService.getColor(username, gameID)) {
                String message = "ERROR: Not a valid move";
                ErrorMessage errorMessage = new ErrorMessage(ERROR, message);
                allConnections.broadcastError(session, errorMessage);
                return;
            }

            if (move.getPromotionPiece() != null) {
                game.getBoard().addPiece(endPosition,
                        new ChessPiece(game.getBoard().getPiece(startPosition).getTeamColor(), move.getPromotionPiece()));
            } else {
                game.getBoard().addPiece(endPosition, startingPiece);
            }
            game.getBoard().addPiece(startPosition, null);

            gameService.updateBoard(gameID, game);

            //check and checkmate and stalemate
            NotificationMessage notificationMessage2 = null;

            if (game.isInCheckmate(ChessGame.TeamColor.WHITE)) {
                String notification = String.format("%s has checkmated white", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
                gameService.updateGameWin(gameID);
            } else if (game.isInCheckmate(ChessGame.TeamColor.BLACK)) {
                String notification = String.format("%s has checkmated black", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
                gameService.updateGameWin(gameID);
            } else if (game.isInCheck(ChessGame.TeamColor.WHITE)) {
                String notification = String.format("%s has put black in check", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
            } else if (game.isInCheck(ChessGame.TeamColor.BLACK)) {
                String notification = String.format("%s has put white in check", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
            } else if (game.isInStalemate(ChessGame.TeamColor.WHITE)) {
                String notification = String.format("%s has put the game in stalemate", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
            } else if (game.isInStalemate(ChessGame.TeamColor.BLACK)) {
                String notification = String.format("%s has put the game in stalemate", username);
                notificationMessage2 = new NotificationMessage(NOTIFICATION, notification);
            }



            gameData = gameService.getGame(gameID);

            ChessGame.TeamColor nextTurn = ChessGame.TeamColor.WHITE;
            if (gameData.whoseTurn() == ChessGame.TeamColor.WHITE) {
                nextTurn = ChessGame.TeamColor.BLACK;
            }

            gameService.updateTurn(gameID, nextTurn);
            gameData = gameService.getGame(gameID);

            LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME,
                    gameData.game(), nextTurn, gameData.gameOver());
            allConnections.broadcastAll(loadGameMessage, gameID);

            String notification = String.format("%s has made the move %s", username, makeMoveCommand.getMove().toString());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);
            allConnections.broadcastSome(session, notificationMessage, gameID);

            if (notificationMessage2 != null) {
                allConnections.broadcastAll(notificationMessage2, gameID);
            }

            if (gameService.isGameWon(gameID)) {
                allConnections.remove(gameID);
            }

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

            allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

            GameData gameData = gameService.getGame(userGameCommand.getGameID());
            LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME,
                    gameData.game(), gameData.whoseTurn(), gameData.gameOver());
            allConnections.broadcastAll(loadGameMessage, userGameCommand.getGameID());



            allConnections.removeSession(userGameCommand.getGameID(), session);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to resign"));
        }
    }
}