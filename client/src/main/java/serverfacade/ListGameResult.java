package serverfacade;

import model.GameMetaData;

import java.util.ArrayList;


public record ListGameResult(ArrayList<GameMetaData> games) {
}
