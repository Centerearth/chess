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
