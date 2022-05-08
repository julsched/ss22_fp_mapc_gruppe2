package massim.javaagents.agents.g2utils;

import java.util.*;

public class RelativeCoordinate {

    private final int x;
    private final int y;

    public RelativeCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public boolean equals(RelativeCoordinate relativeCoordinate) {
        if (this.x == relativeCoordinate.getX() && this.y == relativeCoordinate.getY()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepNorth() {
        if (this.x == 0 && this.y == -1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepEast() {
        if (this.x == 1 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepSouth() {
        if (this.x == 0 && this.y == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepWest() {
        if (this.x == -1 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }
}
