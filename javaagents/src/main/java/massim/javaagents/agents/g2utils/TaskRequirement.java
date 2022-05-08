package massim.javaagents.agents.g2utils;

import java.util.*;

public class TaskRequirement {

    private final RelativeCoordinate relativeCoordinate;
    private final String blockType;

    public TaskRequirement(RelativeCoordinate relativeCoordinate, String blockType) {
        this.relativeCoordinate = relativeCoordinate;
        this.blockType = blockType;
    }

    public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
    }

    public String getBlockType() {
        return this.blockType;
    }
}
