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
                .start();
                //.post("/user", this::createNewUser)

        // Register your endpoints and exception handlers here.

    }

    private void clearApplication(Context context) {
            userService.clearAllData();
    }

//    private void createNewUser(Context context) {
//        HashMap bodyObject = getBodyObject(context, HashMap.class);
//        RegisterResult registerResult = userService.register(new RegisterRequest(
//                bodyObject.get("username")));
//
//        String json = new Gson().toJson(bodyObject);
//        context.json(json);
//
//    }

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
