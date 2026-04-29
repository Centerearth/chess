package server;


public class ServerMain {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("♕ 240 Chess Server");

        Server server = new Server();
        server.run(DEFAULT_PORT);

        System.out.println("♕ 240 Chess Server");
    }
}
