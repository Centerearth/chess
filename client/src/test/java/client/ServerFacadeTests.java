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
        serverFacade.resetAuth();
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
    public void registerAndLoginTest() throws Exception {
        Assertions.assertEquals("User was registered successfully. User was logged in successfully.", serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com"));
        Assertions.assertNotNull(serverFacade.getAuth());
    }

    @Test
    public void registerLogoutLogin() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        serverFacade.logoutUser();
        Assertions.assertEquals("User was logged in successfully.", serverFacade.loginUser("user1", "pswd"));
    }
    //have a register, logout, then login test

    @Test
    public void loginFail() throws Exception {
        Assertions.assertNull(serverFacade.getAuth());
        Assertions.assertNotEquals("User was logged in successfully.", serverFacade.loginUser("user1", "pswd"));
        //finish this
    }

    @Test
    public void logoutUser() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        Assertions.assertEquals("User was logged out successfully.", serverFacade.logoutUser());
    }

    @Test
    public void logoutFail() throws Exception {
        Assertions.assertEquals("User is already logged out.", serverFacade.logoutUser());
    }

    @Test
    public void createGame() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        Assertions.assertEquals("Game was created successfully.", serverFacade.createGame("game1"));
    }

    @Test
    public void createGameFail() throws Exception {
        Assertions.assertEquals("User is not logged in.", serverFacade.createGame("game1"));
    }

    @Test
    public void listGames() throws Exception {
        serverFacade.registerUser("user1", "pswd", "abcd@yahoo.com");
        serverFacade.createGame("game1");
        serverFacade.listGames();
    }

}
