package recordandrequest;

//[{"gameID": 1234, "whiteUsername":"", "blackUsername":"", "gameName:""} ]
public record GameRepresentation(int gameID, String whiteUsername, String blackUsername, String gameName) {
}
