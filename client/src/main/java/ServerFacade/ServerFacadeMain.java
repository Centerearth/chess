package ServerFacade;

import com.google.gson.Gson;
import model.AuthData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class ServerFacadeMain {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String serverUrl;
    private static AuthData authData;
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

    public void clearEverything() throws IOException, InterruptedException {
        //for testing purposes
        HttpResponse<String> httpResponse = buildAndReceiveRequest("DELETE", "/db", null, null);
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



    private HttpResponse<String> buildAndReceiveRequest(String method, String path, Object body, HashMap<String, String> header) throws IOException, InterruptedException {
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
        return httpClient.send(finishedRequest, HttpResponse.BodyHandlers.ofString());

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
        } else if (httpResponse.statusCode() == 403) {
            return "User is already registered.";
        } else if (httpResponse.statusCode() == 400) {
            return "Request was malformed.";
        } else {
            System.out.println("Error: received status code " + httpResponse.statusCode());
            return "Error"; // change
        }
    }
    //for now have each functionality that interacts with the server its own thing. then start to group functionality
}
