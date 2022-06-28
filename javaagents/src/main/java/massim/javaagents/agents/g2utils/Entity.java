package massim.javaagents.agents.g2utils;

import java.util.*;

public class Entity extends Cell {

    private final String teamName;

    public Entity(RelativeCoordinate relativeCoordinate, String teamName, int lastSeen) {
        super(relativeCoordinate, lastSeen);
        this.teamName = teamName;
    }

    public String getTeamName() {
        return this.teamName;
    }
}
