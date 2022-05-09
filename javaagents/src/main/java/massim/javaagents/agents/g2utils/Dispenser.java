package massim.javaagents.agents.g2utils;

import java.util.*;

public class Dispenser {

    private final RelativeCoordinate relativeCoordinate;
    private final String type;

    public static Dispenser getClosestDispenser(List<Dispenser> dispensers) {
        Dispenser closestDispenser = null;
        for (Dispenser dispenser : dispensers) {
            if (closestDispenser == null) {
                closestDispenser = dispenser;
                continue;
            }
            if (dispenser.isCloserThan(closestDispenser)) {
                closestDispenser = dispenser;
            }
        }
        return closestDispenser;
    }

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

    public boolean isCloserThan(Dispenser dispenser) {
        if (this.relativeCoordinate.isCloserThan(dispenser.getRelativeCoordinate())) {
            return true;
        } else {
            return false;
        }
    }
}
