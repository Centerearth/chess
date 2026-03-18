package client;

import org.junit.jupiter.api.*;
import server.Server;
import ServerFacade.ServerFacadeMain;

import java.io.IOException;

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

    @BeforeEach
    public void clearDatabase() throws IOException, InterruptedException {
        serverFacade.clearEverything();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    public void registerTest() throws Exception {
        Assertions.assertEquals("User was registered successfully. User was logged in successfully.", serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com"));
    }

    @Test
    public void registerTestTwice() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        Assertions.assertEquals("User is already registered.", serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com"));
    }

    @Test
    public void loginTest() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        Assertions.assertEquals("User was logged in successfully.", serverFacade.loginUser("user1", "pswd"));

    }

}
