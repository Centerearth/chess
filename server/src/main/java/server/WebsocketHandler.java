package server;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.FenSerializer;
import chess.InvalidMoveException;
import chess.SanGenerator;


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

import agent.Agent;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static websocket.messages.ServerMessage.ServerMessageType.*;


public class WebsocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketHandler.class);
    private static final String WHITE_WINS = "1-0";
    private static final String BLACK_WINS = "0-1";
    private static final String DRAW = "1/2-1/2";
    private static final long AI_VS_AI_MOVE_DELAY_MS = 800;

    private static final ThreadFactory DAEMON_FACTORY = runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    };

    private final ConnectionManager allConnections = new ConnectionManager();
    private final GameService gameService;
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool(DAEMON_FACTORY);
    private final ScheduledExecutorService clockSweeper = Executors.newSingleThreadScheduledExecutor(DAEMON_FACTORY);
    private final ConcurrentHashMap<Integer, AtomicBoolean> aiBusy = new ConcurrentHashMap<>();

    public WebsocketHandler(GameService gameService) throws DataAccessException {
        this.gameService = gameService;
        clockSweeper.scheduleAtFixedRate(this::sweepClocks, 1, 1, TimeUnit.SECONDS);
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
                LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData, System.currentTimeMillis());
                allConnections.broadcastOne(session, loadGameMessage, gameData.gameID());

                String notification = "This game has already ended";
                NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);
                allConnections.broadcastOne(session, notificationMessage, userGameCommand.getGameID());
                allConnections.removeSession(userGameCommand.getGameID(), session);
                return;
            }
        allConnections.add(userGameCommand.getGameID(), session);

        GameData gameData = gameService.getGame(userGameCommand.getGameID());
        LoadGameMessage loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData, System.currentTimeMillis());

        String notification = String.format("%s has joined the game as %s", userGameCommand.getUsername(), userGameCommand.getColor());

        NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

        allConnections.broadcastOne(session, loadGameMessage, userGameCommand.getGameID());
        allConnections.broadcastSome(session, notificationMessage, userGameCommand.getGameID());

        scheduleAiMoveIfNeeded(userGameCommand.getGameID());

        } catch (Exception e) {
            logger.error("Failed to connect to game", e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR,"Error: failed to connect"));
        }

    }

    private void makeMove(MakeMoveCommand makeMoveCommand, Session session) throws IOException {
        int gameID = makeMoveCommand.getGameID();
        try {
            String username;
            ChessGame.TeamColor playerColor;
            synchronized (gameService.getLock(gameID)) {
                if (gameService.isGameWon(gameID)) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "This game has already ended."));
                    return;
                }

                username = gameService.getAuthData(makeMoveCommand.getAuthToken()).username();
                ChessGame game = gameService.getGame(gameID).game();
                ChessMove move = makeMoveCommand.getMove();

                playerColor = gameService.getColor(username, gameID);
                if (playerColor == null) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "ERROR: Observers cannot make moves"));
                    return;
                }

                ChessPiece startingPiece = game.getBoard().getPiece(move.getStartPosition());
                if (startingPiece != null && startingPiece.getTeamColor() != playerColor) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "ERROR: You cannot move your opponent's pieces"));
                    return;
                }
            }

            applyMove(gameID, makeMoveCommand.getMove(), username, playerColor, session);

        } catch (Exception e) {
            logger.error("Failed to make move in game {}", gameID, e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: failed to make move"));
        }
    }

    /**
     * Applies a move for a player or an AI: deducts clock time, records SAN history,
     * detects game-ending conditions, persists, and broadcasts.
     *
     * @param moverSession the mover's websocket session, or null when the AI moves
     * @return true if the move was applied and the game continues
     */
    private boolean applyMove(int gameID, ChessMove move, String moverName,
                              ChessGame.TeamColor moverColor, Session moverSession) throws Exception {
        LoadGameMessage loadGameMessage;
        NotificationMessage moveNotification;
        NotificationMessage endNotification;
        boolean gameWon;

        synchronized (gameService.getLock(gameID)) {
            if (gameService.isGameWon(gameID)) {
                errorOrLog(moverSession, gameID, "This game has already ended.");
                return false;
            }
            GameData gameData = gameService.getGame(gameID);
            if (gameData == null) {
                return false;
            }
            ChessGame game = gameData.game();
            if (game.getTeamTurn() != moverColor) {
                errorOrLog(moverSession, gameID, "ERROR: It is not your turn");
                return false;
            }

            long now = System.currentTimeMillis();
            Long whiteTime = gameData.whiteTimeMs();
            Long blackTime = gameData.blackTimeMs();
            if (gameData.isTimed() && gameData.turnStartedAt() != null) {
                long remaining = (moverColor == ChessGame.TeamColor.WHITE ? whiteTime : blackTime)
                        - (now - gameData.turnStartedAt());
                if (remaining <= 0) {
                    endGameOnTime(gameData, moverColor);
                    return false;
                }
                if (moverColor == ChessGame.TeamColor.WHITE) {
                    whiteTime = remaining;
                } else {
                    blackTime = remaining;
                }
            }

            String san = SanGenerator.toSan(game, move);
            try {
                game.makeMove(move);
            } catch (InvalidMoveException e) {
                errorOrLog(moverSession, gameID, String.format("ERROR: Invalid move, %s", e.getMessage()));
                return false;
            }

            Long newTurnStart = gameData.isTimed() ? now : null;
            gameData = gameData.withMoveApplied(game, san, move, FenSerializer.toFen(game),
                    whiteTime, blackTime, newTurnStart);

            EndState endState = evaluateEndState(gameData, moverName);
            if (endState != null && endState.result() != null) {
                gameData = gameData.withGameOver(endState.result());
            }
            gameService.putGame(gameData);

            loadGameMessage = new LoadGameMessage(LOAD_GAME, gameData, now);
            moveNotification = new NotificationMessage(NOTIFICATION,
                    String.format("%s has made the move %s", moverName, san));
            endNotification = endState == null ? null : new NotificationMessage(NOTIFICATION, endState.notification());
            gameWon = Boolean.TRUE.equals(gameData.gameOver());
        }

        allConnections.broadcastAll(loadGameMessage, gameID);
        if (moverSession != null) {
            allConnections.broadcastSome(moverSession, moveNotification, gameID);
        } else {
            allConnections.broadcastAll(moveNotification, gameID);
        }
        if (endNotification != null) {
            allConnections.broadcastAll(endNotification, gameID);
        }
        if (gameWon) {
            allConnections.remove(gameID);
            return false;
        }
        scheduleAiMoveIfNeeded(gameID);
        return true;
    }

    private void errorOrLog(Session session, int gameID, String message) throws IOException {
        if (session != null) {
            allConnections.broadcastError(session, new ErrorMessage(ERROR, message));
        } else {
            logger.error("AI move rejected in game {}: {}", gameID, message);
        }
    }

    private record EndState(String notification, String result) {}

    /** Checkmate, check, and every draw condition. Returns null if the game simply continues. */
    private EndState evaluateEndState(GameData gameData, String moverName) {
        ChessGame game = gameData.game();
        if (game.isInCheckmate(ChessGame.TeamColor.WHITE)) {
            return new EndState(String.format("%s has checkmated white", moverName), BLACK_WINS);
        }
        if (game.isInCheckmate(ChessGame.TeamColor.BLACK)) {
            return new EndState(String.format("%s has checkmated black", moverName), WHITE_WINS);
        }
        if (game.isInStalemate(ChessGame.TeamColor.WHITE) || game.isInStalemate(ChessGame.TeamColor.BLACK)) {
            return new EndState("Draw by stalemate", DRAW);
        }
        if (game.isFiftyMoveDraw()) {
            return new EndState("Draw by the fifty-move rule", DRAW);
        }
        if (game.isInsufficientMaterial()) {
            return new EndState("Draw by insufficient material", DRAW);
        }
        var positions = gameData.positionHistorySafe();
        if (!positions.isEmpty()) {
            String current = positions.get(positions.size() - 1);
            long occurrences = positions.stream().filter(current::equals).count();
            if (occurrences >= 3) {
                return new EndState("Draw by threefold repetition", DRAW);
            }
        }
        if (game.isInCheck(ChessGame.TeamColor.WHITE)) {
            return new EndState(String.format("%s has put white in check", moverName), null);
        }
        if (game.isInCheck(ChessGame.TeamColor.BLACK)) {
            return new EndState(String.format("%s has put black in check", moverName), null);
        }
        return null;
    }

    /** Must be called while holding the game lock. */
    private void endGameOnTime(GameData gameData, ChessGame.TeamColor flagged) throws Exception {
        int gameID = gameData.gameID();
        Long whiteTime = flagged == ChessGame.TeamColor.WHITE ? 0L : gameData.whiteTimeMs();
        Long blackTime = flagged == ChessGame.TeamColor.BLACK ? 0L : gameData.blackTimeMs();
        String result = flagged == ChessGame.TeamColor.WHITE ? BLACK_WINS : WHITE_WINS;
        GameData ended = gameData.withClocks(whiteTime, blackTime, gameData.turnStartedAt()).withGameOver(result);
        gameService.putGame(ended);

        allConnections.broadcastAll(new LoadGameMessage(LOAD_GAME, ended, System.currentTimeMillis()), gameID);
        allConnections.broadcastAll(new NotificationMessage(NOTIFICATION,
                String.format("%s ran out of time", flagged == ChessGame.TeamColor.WHITE ? "White" : "Black")), gameID);
        allConnections.remove(gameID);
    }

    /** Periodically ends games whose side to move has run out of time. */
    private void sweepClocks() {
        for (Integer gameID : allConnections.activeGameIDs()) {
            try {
                GameData gameData = gameService.getGame(gameID);
                if (gameData == null || Boolean.TRUE.equals(gameData.gameOver())
                        || !gameData.isTimed() || gameData.turnStartedAt() == null) {
                    continue;
                }
                ChessGame.TeamColor sideToMove = gameData.game().getTeamTurn();
                long base = sideToMove == ChessGame.TeamColor.WHITE ? gameData.whiteTimeMs() : gameData.blackTimeMs();
                if (base - (System.currentTimeMillis() - gameData.turnStartedAt()) > 0) {
                    continue;
                }
                synchronized (gameService.getLock(gameID)) {
                    gameData = gameService.getGame(gameID);
                    if (gameData == null || Boolean.TRUE.equals(gameData.gameOver())
                            || !gameData.isTimed() || gameData.turnStartedAt() == null) {
                        continue;
                    }
                    sideToMove = gameData.game().getTeamTurn();
                    base = sideToMove == ChessGame.TeamColor.WHITE ? gameData.whiteTimeMs() : gameData.blackTimeMs();
                    if (base - (System.currentTimeMillis() - gameData.turnStartedAt()) <= 0) {
                        endGameOnTime(gameData, sideToMove);
                    }
                }
            } catch (Exception e) {
                logger.error("Clock sweep failed for game {}", gameID, e);
            }
        }
    }

    public void scheduleAiMoveIfNeeded(int gameID) {
        AtomicBoolean busy = aiBusy.computeIfAbsent(gameID, id -> new AtomicBoolean(false));
        if (!busy.compareAndSet(false, true)) {
            return;
        }
        aiExecutor.submit(() -> {
            try {
                runAiTurns(gameID);
            } catch (Throwable e) {
                logger.error("AI move failed in game {}", gameID, e);
            } finally {
                busy.set(false);
            }
        });
    }

    private static String aiDisplayName(String aiUsername) {
        return GameService.AI_ML_USERNAME.equals(aiUsername) ? "The AI (neural net)" : "The AI (alpha-beta)";
    }

    private void runAiTurns(int gameID) throws Exception {
        while (true) {
            // pause AI play when nobody is watching; resumes on the next connect
            if (!allConnections.hasOpenConnections(gameID)) {
                return;
            }

            ChessGame snapshot;
            ChessGame.TeamColor turn;
            String aiUsername;
            String opponentUsername;
            int difficulty;

            synchronized (gameService.getLock(gameID)) {
                if (gameService.isGameWon(gameID)) {
                    return;
                }
                GameData gameData = gameService.getGame(gameID);
                if (gameData == null) {
                    return;
                }
                turn = gameData.game().getTeamTurn();
                aiUsername = (turn == ChessGame.TeamColor.WHITE)
                        ? gameData.whiteUsername() : gameData.blackUsername();
                opponentUsername = (turn == ChessGame.TeamColor.WHITE)
                        ? gameData.blackUsername() : gameData.whiteUsername();
                if (!GameService.isAiUsername(aiUsername)) {
                    return;
                }
                difficulty = gameData.aiDifficulty() == null ? Agent.DIFFICULTY_HARD : gameData.aiDifficulty();
                snapshot = (ChessGame) gameData.game().clone();
            }

            boolean mlMode = GameService.AI_ML_USERNAME.equals(aiUsername);
            ChessMove move = new Agent(turn, difficulty).getBestMove(snapshot, mlMode);
            if (move == null) {
                logger.error("AI could not find a move in game {}", gameID);
                return;
            }

            boolean gameContinues = applyMove(gameID, move, aiDisplayName(aiUsername), turn, null);
            if (!gameContinues) {
                return;
            }
            if (GameService.isAiUsername(opponentUsername)) {
                Thread.sleep(AI_VS_AI_MOVE_DELAY_MS); // watchable pace for AI-vs-AI games
            }
        }
    }
    private void leave(UserGameCommand userGameCommand, Session session) throws IOException {
        try {
            String notification = String.format("%s has left the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            NotificationMessage notificationMessage = new NotificationMessage(NOTIFICATION, notification);

            String username = gameService.getAuthData(userGameCommand.getAuthToken()).username();
            ChessGame.TeamColor teamColor = gameService.getColor(username, userGameCommand.getGameID());

            // only free the seat while the game is still in progress; finished games keep their players
            if (teamColor != null && !gameService.isGameWon(userGameCommand.getGameID())) {
                gameService.updateGame(teamColor, userGameCommand.getGameID(), null);
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
            ChessGame.TeamColor resignerColor = gameService.getColor(username, gameID);
            if (username == null || resignerColor == null) {
                allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: cannot resign as observer"));
                return;
            }

            synchronized (gameService.getLock(gameID)) {
                if (gameService.isGameWon(gameID)) {
                    allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: game is already finished."));
                    return;
                }
                String result = resignerColor == ChessGame.TeamColor.WHITE ? BLACK_WINS : WHITE_WINS;
                gameService.updateGameWin(gameID, result);
            }

            String notification = String.format("%s has resigned from the game (%s)", userGameCommand.getUsername(), userGameCommand.getColor());
            allConnections.broadcastAll(new NotificationMessage(NOTIFICATION, notification), gameID);
            allConnections.remove(gameID);

        } catch (Exception e) {
            logger.error("Failed to resign from game {}", gameID, e);
            allConnections.broadcastError(session, new ErrorMessage(ERROR, "Error: failed to resign"));
        }
    }
}
