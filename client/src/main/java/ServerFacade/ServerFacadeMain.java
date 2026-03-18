package ServerFacade;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class ServerFacadeMain {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String serverUrl;

    public ServerFacadeMain(String url) {
        this.serverUrl = url;
    }

    public String loginUser(String username, String password) throws IOException, InterruptedException {
        HashMap<String, String> bodyObject = new HashMap<>();
        bodyObject.put("username", username);
        bodyObject.put("password", password);
        String jsonBody = new Gson().toJson(bodyObject);
        System.out.println(jsonBody);
        HttpResponse<String> httpResponse = buildAndReceiveRequest("POST", "/session", jsonBody);
        System.out.println(httpResponse);
        return responseHandler("User was logged in successfully.", httpResponse);

    }

    private HttpResponse<String> buildAndReceiveRequest(String method, String path, Object body) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .timeout(java.time.Duration.ofMillis(5000))
                .method(method, makeRequestBody(body));
        if (body != null) {
            requestBuilder.setHeader("Content-Type", "application/json");
            //probably need to set other headers as well
        }
        HttpRequest finishedRequest = requestBuilder.build();
        System.out.println(finishedRequest);
        return httpClient.send(finishedRequest, HttpResponse.BodyHandlers.ofString()); //Should it always be a string?

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
        } else {
            System.out.println("Error: received status code " + httpResponse.statusCode());
            return "Error"; // change
        }
    }
    //for now have each functionality that interacts with the server its own thing. then start to group functionality
    //have a seperate error checker that has a message passed in based ont the method but ovverides if there is an error
}
