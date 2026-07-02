package recordandrequest;

public record CreateGameRequest(String authToken, String gameName, Integer timeControlMinutes) {
    public CreateGameRequest(String authToken, String gameName) {
        this(authToken, gameName, null);
    }
}
