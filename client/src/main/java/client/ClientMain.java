package client;

import chess.*;

public class ClientMain {
    public static void main(String[] args) {
        System.out.println("♕ 240 Chess Client");

        String serverUrl = "http://localhost:8080";
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
