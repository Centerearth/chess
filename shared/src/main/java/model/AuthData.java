package model;

public record AuthData(String authToken, String username, Long createdAt) {
    public AuthData(String authToken, String username) {
        this(authToken, username, System.currentTimeMillis());
    }
}
