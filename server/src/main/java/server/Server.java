package server;

import com.google.gson.Gson;
import io.javalin.*;
import io.javalin.http.Context;
import recordandrequest.*;
import service.UserService;

import java.util.HashMap;

public class Server {

    private final Javalin javalin;
    private final UserService userService = new UserService();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"))
                .delete("/db", this::clearApplication)
                .delete("/session", this::logoutUser)
                .post("/user", this::createNewUser)
                .start();

        // Register your endpoints and exception handlers here.

    }

    private void clearApplication(Context context) {
            userService.clearAllData();
    }

    private void createNewUser(Context context) {
        HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
        String username = bodyObject.get("username");
        String password = bodyObject.get("password");
        String email = bodyObject.get("email");
        RegisterResult registerResult = userService.register(new RegisterRequest(
                username, password, email));

        String json = new Gson().toJson(registerResult);
        context.json(json);

    }

    private void loginUser(Context context) {
        try {
            HashMap<String, String> bodyObject = getBodyObject(context, HashMap.class);
            String username = bodyObject.get("username");
            String password = bodyObject.get("password");
            LoginResult loginResult = userService.login(new LoginRequest(
                    username, password));

            String json = new Gson().toJson(loginResult);
            context.json(json);
        } catch (Exception e) { // change this later
            System.out.println("Hi2");
        }
    }

    private void logoutUser(Context context) {
        try {
            String authToken = getAuthToken(context);
            userService.logout(new LogoutRequest(authToken));
        } catch (Exception e) { // change all this
            System.out.println("Hi");
        }
    }

    private String getAuthToken (Context context) {
        return context.header("authorization");
    }

    private static <T> T getBodyObject(Context context, Class<T> classType) {
        var bodyObject = new Gson().fromJson(context.body(), classType);

        if (bodyObject == null) {
            throw new RuntimeException("missing required body");
        }

        return bodyObject;
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
