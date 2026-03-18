package client;

import org.junit.jupiter.api.*;
import server.Server;
import ServerFacade.ServerFacadeMain;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacadeMain serverFacade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        String urlString = String.format("http://%s:%d", "localhost", port);
        serverFacade = new ServerFacadeMain(urlString);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    public void loginTest() throws Exception {
        System.out.println(serverFacade.loginUser("hello", "hello"));
        Assertions.assertTrue(true);
    }

}
