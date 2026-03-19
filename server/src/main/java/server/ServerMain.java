package server;

import chess.*;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("♕ 240 Chess Server");

        Server server = new Server();
        server.run(8080);

        System.out.println("♕ 240 Chess Server");
    }
}
