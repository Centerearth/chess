package client;

public class ClientMain {
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";

    public static void main(String[] args) {
        System.out.println("♕ 240 Chess Client");

        String serverUrl = DEFAULT_SERVER_URL;
        if (args.length == 1) {
            serverUrl = args[0];
        }
        try {
            new ChessClient(serverUrl).run();
        } catch (Exception ex) {
            System.out.printf("Unable to start client: %s%n", ex.getMessage());
        }
    }
}
