package massim.javaagents.agents.g2utils;

import java.util.*;

public class Block {

    private final RelativeCoordinate relativeCoordinate;
    private final String type;

    public Block(RelativeCoordinate relativeCoordinate, String type) {
        this.relativeCoordinate = relativeCoordinate;
        this.type = type;
    }

    public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
    }

    public String getType() {
        return this.type;
    }

    public boolean sameTypeAs(Block block) {
        if (this.type.equals(block.getType())) {
            return true;
        } else {
            return false;
        }
    }
}
