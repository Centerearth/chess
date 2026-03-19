package serverfacade;

import com.google.gson.Gson;
import model.AuthData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class ServerFacadeMain {
    private static final HttpClient HTTPCLIENT = HttpClient.newHttpClient();
    private final String serverUrl;
    private static AuthData authData;
    private static HashMap<Integer, Integer> idToNumber;
    //add logging

    public ServerFacadeMain(String url) {
        this.serverUrl = url;
    }

    public AuthData getAuth() {
        return authData;
    }

    public void setAuth(String username, String token) {
        authData = new AuthData(token, username);
    }

    public void resetAuth() {
        //for testing purposes
        authData = null;
    }

    public void resetIds() {
        //for testing purposes
        idToNumber = null;
    }

    public void clearEverything() throws IOException, InterruptedException {
        //for testing purposes
        buildAndReceiveRequest("DELETE", "/db", null, null);
    }

    public String logoutUser() throws IOException, InterruptedException {
        if (authData == null) {
            return "User is already logged out.";
        }
        String authToken = authData.authToken();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", authToken);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("DELETE", "/session", null, headers);
        return responseHandler("User was logged out successfully.", httpResponse);
    }

    public String loginUser(String username, String password) throws IOException, InterruptedException {
        HashMap<String, String> bodyObject = new HashMap<>();
        bodyObject.put("username", username);
        bodyObject.put("password", password);
        String jsonBody = new Gson().toJson(bodyObject);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/session", jsonBody, null);

        if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
            var body = new Gson().fromJson(httpResponse.body(), HashMap.class);
            setAuth(body.get("username").toString(), body.get("authToken").toString());
        }
        return responseHandler("User was logged in successfully.", httpResponse);

    }

    public String playGame(int gameIndex, String color) throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        String authToken = authData.authToken();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", authToken);

        if (idToNumber == null || !idToNumber.containsKey(gameIndex)) {
            return "Game does not exist.";
        }
        //need to make it so that the checks happen on the other end and that the right format is passed in
        HashMap<String, Object> bodyObject = new HashMap<>();
        bodyObject.put("gameID", idToNumber.get(gameIndex));
        bodyObject.put("playerColor", color);
        String jsonBody = new Gson().toJson(bodyObject);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("PUT", "/game", jsonBody, headers);
        return responseHandler("User joined successfully.", httpResponse);
    }
    //need to change from string?

    public String observeGame(int gameIndex) {
        if (authData == null) {
            return "User is not logged in.";
        }
        //this will be implemented later
        if (idToNumber == null || !idToNumber.containsKey(gameIndex)) {
            return "Game does not exist.";
        }
        return "Game is being observed.";
    }

    public String registerUser(String username, String password, String email) throws IOException, InterruptedException {
        HashMap<String, String> bodyObject = new HashMap<>();
        bodyObject.put("username", username);
        bodyObject.put("password", password);
        bodyObject.put("email", email);
        String jsonBody = new Gson().toJson(bodyObject);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/user", jsonBody, null);
        String loginResult = loginUser(username, password);
        return responseHandler("User was registered successfully. " + loginResult, httpResponse);

    }

    public String createGame(String gameName) throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        String authToken = authData.authToken();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", authToken);

        HashMap<String, String> bodyObject = new HashMap<>();
        bodyObject.put("gameName", gameName);
        String jsonBody = new Gson().toJson(bodyObject);

        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/game", jsonBody, headers);
        return responseHandler("Game was created successfully.", httpResponse);
    }

    public String listGames() throws IOException, InterruptedException {
        if (authData == null) {
            return "User is not logged in.";
        }
        String authToken = authData.authToken();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", authToken);

        HttpResponse<String> httpResponse = buildAndReceiveRequest("GET", "/game", null, headers);
        System.out.println(httpResponse.body());
        ListGameResult allGames = new Gson().fromJson(httpResponse.body(), ListGameResult.class);
        System.out.println("allGames: " + allGames);

        StringBuilder gameList = new StringBuilder();
        idToNumber = new HashMap<>();

        if (allGames.games().isEmpty()) {
            return "No games to display.";
        }

        for (int i = 0; i < allGames.games().size(); i++) {
            idToNumber.put(i+1, allGames.games().get(i).gameID());

            gameList.append("Game: ");
            gameList.append(i+1);
            gameList.append(", Game Name: ");
            gameList.append(allGames.games().get(i).gameName());
            gameList.append(", White Player: ");
            if (allGames.games().get(i).whiteUsername() != null) {
                gameList.append(allGames.games().get(i).whiteUsername());
            } else {
                gameList.append("none");
            }
            gameList.append(", Black Player: ");
            if (allGames.games().get(i).blackUsername() != null) {
                gameList.append(allGames.games().get(i).blackUsername());
            } else {
                gameList.append("none");
            }
            gameList.append("\n");
        }

        System.out.println(idToNumber);
        //need a way to format the games and then store them in a static variable where the order matches to the id
        return responseHandler(gameList.toString(), httpResponse);
    }

    private HttpResponse<String> buildAndReceiveRequest(String method, String path, Object body,
                                                        HashMap<String, String> header) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .timeout(java.time.Duration.ofMillis(5000))
                .method(method, makeRequestBody(body));
        if (body != null) {
            requestBuilder.setHeader("Content-Type", "application/json");
        }
        if (header != null) {
            for (String key : header.keySet()) {
                System.out.println(key);
                System.out.println(header.get(key));
                requestBuilder.setHeader(key, header.get(key));
            }
        }
        HttpRequest finishedRequest = requestBuilder.build();
        System.out.println(finishedRequest);
        System.out.println(finishedRequest.headers());
        return HTTPCLIENT.send(finishedRequest, HttpResponse.BodyHandlers.ofString());

    }


    private HttpRequest.BodyPublisher makeRequestBody(Object request) {
        if (request != null) {
            return HttpRequest.BodyPublishers.ofString((String) request); //seems like a bandaid
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    private String responseHandler(String defaultMessage, HttpResponse httpResponse) {
        if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
            System.out.println(httpResponse.body());
            return defaultMessage;
        } else if (httpResponse.statusCode() == 400) {
            return "Request was malformed.";
        } else if (httpResponse.statusCode() == 401) {
            return "User is not authorized.";
        } else if (httpResponse.statusCode() == 403) {
            return "That option is already taken";
        } else {
            System.out.println("Error: received status code " + httpResponse.statusCode()); //delete
            return "An unknown error occurred.";
        }
    }
    //for now have each functionality that interacts with the server its own thing. then start to group functionality
}
