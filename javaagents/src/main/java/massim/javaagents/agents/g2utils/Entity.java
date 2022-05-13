package massim.javaagents.agents.g2utils;

import java.util.*;

public class Entity {

    private final RelativeCoordinate relativeCoordinate;
    private final String teamName;

    public Entity(RelativeCoordinate relativeCoordinate, String teamName) {
        this.relativeCoordinate = relativeCoordinate;
        this.teamName = teamName;
    }

    public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
    }

    public String getTeamName() {
        return this.teamName;
    }
}
