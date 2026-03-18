package ServerFacade;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ServerFacadeMain {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String serverUrl;

    public ServerFacadeMain(String url) {
        this.serverUrl = url;
    }

    private void loginUser(String host, int port, String path, String username, String password) throws Exception {
    }

    private HttpRequest buildAndReceiveRequest(String method, String path, Object body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .timeout(java.time.Duration.ofMillis(5000))
                .method(method, makeRequestBody(body));
        if (body != null) {
            request.setHeader("Content-Type", "application/json");
        }
        request = request.build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return null;
    }

    private HttpRequest.BodyPublisher makeRequestBody(Object request) {
        if (request != null) {
            return HttpRequest.BodyPublishers.ofString(new Gson().toJson(request));
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
