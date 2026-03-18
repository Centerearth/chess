package ServerFacade;

import java.net.http.HttpClient;

public class ServerFacadeMain {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    //for now have each functionality that interacts with the server its own thing
    //have a seperate error checker that has a message passed in based ont the method but ovverides if there is an error
}
