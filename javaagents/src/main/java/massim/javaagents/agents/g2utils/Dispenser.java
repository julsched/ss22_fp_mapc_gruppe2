package massim.javaagents.agents.g2utils;

import java.util.*;

public class Dispenser {

    private final RelativeCoordinate relativeCoordinate;
    private final String type;


    public Dispenser(RelativeCoordinate relativeCoordinate, String type) {
        this.relativeCoordinate = relativeCoordinate;
        this.type = type;
    }

    public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
    }

    public String getType() {
        return this.type;
    }

    public boolean sameTypeAs(Dispenser dispenser) {
        if (this.type.equals(dispenser.getType())) {
            return true;
        } else {
            return false;
        }
    }
}
