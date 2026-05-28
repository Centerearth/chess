package serverfacade;

import com.google.gson.Gson;
import model.AuthData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class ServerFacadeMain {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final int REQUEST_TIMEOUT_MS = 5000;
    private final String serverUrl;
    private AuthData authData;
    private HashMap<String, Integer> gameNameToId = new HashMap<>();

    public ServerFacadeMain(String url) {
        this.serverUrl = url;
    }

    public AuthData getAuth() {
        return authData;
    }

    public void setAuth(String username, String token) {
        authData = new AuthData(token, username);
    }

    public Integer getGameId(String gameName) {
        return gameNameToId.get(gameName);
    }

    public boolean isGameCurrent(int id) {
        return gameNameToId.containsValue(id);
    }

    public void clearEverything() throws IOException, InterruptedException {
        buildAndReceiveRequest("DELETE", "/db", null, null);
    }

    public String logoutUser() throws IOException, InterruptedException {
        if (authData == null) {
            return "User is already logged out.";
        }
        HttpResponse<String> httpResponse = buildAndReceiveRequest("DELETE", "/session", null, makeAuthHeaders());
        return responseHandler("User was logged out successfully.", httpResponse);
    }

    public String loginUser(String username, String password) throws IOException, InterruptedException {
        HashMap<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/session", GSON.toJson(body), null);

        if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
            var parsed = GSON.fromJson(httpResponse.body(), HashMap.class);
            setAuth(parsed.get("username").toString(), parsed.get("authToken").toString());
        }
        return responseHandler("User was logged in successfully.", httpResponse);
    }

    public String registerUser(String username, String password, String email) throws IOException, InterruptedException {
        HashMap<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", email);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/user", GSON.toJson(body), null);
        String loginResult = loginUser(username, password);
        return responseHandler("User was registered successfully. " + loginResult, httpResponse);
    }

    public String createGame(String gameName) throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("gameName", gameName);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/game", GSON.toJson(body), makeAuthHeaders());
        return responseHandler("Game was created successfully.", httpResponse);
    }

    public String listGames() throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        HttpResponse<String> httpResponse = buildAndReceiveRequest("GET", "/game", null, makeAuthHeaders());
        ListGameResult allGames = GSON.fromJson(httpResponse.body(), ListGameResult.class);

        if (allGames.games().isEmpty()) {
            return "No games to display.";
        }

        gameNameToId = new HashMap<>();
        StringBuilder gameList = new StringBuilder();
        for (int i = 0; i < allGames.games().size(); i++) {
            var game = allGames.games().get(i);
            gameNameToId.put(game.gameName(), game.gameID());
            gameList.append("Game Name: ").append(game.gameName());
            gameList.append(", White Player: ").append(game.whiteUsername() != null ? game.whiteUsername() : "none");
            gameList.append(", Black Player: ").append(game.blackUsername() != null ? game.blackUsername() : "none");
            gameList.append("\n");
        }
        return responseHandler(gameList.toString(), httpResponse);
    }

    public String playGame(int gameIndex, String color) throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        if (!isGameCurrent(gameIndex)) {
            return "Game does not exist.";
        }
        HashMap<String, Object> body = new HashMap<>();
        body.put("gameID", gameIndex);
        body.put("playerColor", color);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("PUT", "/game", GSON.toJson(body), makeAuthHeaders());
        return responseHandler("User joined successfully.", httpResponse);
    }

    public String observeGame(int gameIndex) {
        if (authData == null) {
            return "User is not logged in.";
        }
        if (!isGameCurrent(gameIndex)) {
            return "Game does not exist.";
        }
        return "Game is being observed.";
    }

    private HashMap<String, String> makeAuthHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", authData.authToken());
        return headers;
    }

    private HttpResponse<String> buildAndReceiveRequest(String method, String path, String body,
                                                        HashMap<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .timeout(java.time.Duration.ofMillis(REQUEST_TIMEOUT_MS))
                .method(method, makeRequestBody(body));
        if (body != null) {
            requestBuilder.setHeader("Content-Type", "application/json");
        }
        if (headers != null) {
            for (String key : headers.keySet()) {
                requestBuilder.setHeader(key, headers.get(key));
            }
        }
        return HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.BodyPublisher makeRequestBody(String body) {
        if (body != null) {
            return HttpRequest.BodyPublishers.ofString(body);
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    private String responseHandler(String defaultMessage, HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() >= HttpURLConnection.HTTP_OK && httpResponse.statusCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
            return defaultMessage;
        } else if (httpResponse.statusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            return "Request was malformed.";
        } else if (httpResponse.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return "User is not authorized.";
        } else if (httpResponse.statusCode() == HttpURLConnection.HTTP_FORBIDDEN) {
            return "That option is already taken";
        } else {
            return "An unknown error occurred.";
        }
    }
}
