package massim.javaagents.agents.g2utils;

import java.util.*;

public class Dispenser extends Cell {

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

    public Dispenser(RelativeCoordinate relativeCoordinate, String type, int lastSeen) {
    	super(relativeCoordinate, lastSeen);
        this.type = type;
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
