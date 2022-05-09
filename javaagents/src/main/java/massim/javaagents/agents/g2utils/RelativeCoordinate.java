package massim.javaagents.agents.g2utils;

import java.util.*;

public class RelativeCoordinate {

    private final int x;
    private final int y;

    public static RelativeCoordinate getClosestCoordinate(List<RelativeCoordinate> relativeCoordinates) {
        RelativeCoordinate shortestCoordinate = null;
        for (RelativeCoordinate relativeCoordinate : relativeCoordinates) {
            if (shortestCoordinate == null) {
                shortestCoordinate = relativeCoordinate;
                continue;
            }
            if (relativeCoordinate.isCloserThan(shortestCoordinate)) {
                shortestCoordinate = relativeCoordinate;
            }
        }
        return shortestCoordinate;
    }

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

    // Manhattan distance
    public int distanceFromAgent() {
        return Math.abs(this.x) + Math.abs(this.y);
    }

    public boolean isCloserThan(RelativeCoordinate relativeCoordinate) {
        if (this.distanceFromAgent() < relativeCoordinate.distanceFromAgent()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirectlyNorth() {
        if (this.x == 0 && this.y < 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirectlyEast() {
        if (this.x > 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirectlySouth() {
        if (this.x == 0 && this.y > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirectlyWest() {
        if (this.x < 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getDirectDirection() {
        if (this.isDirectlyNorth()) {
            return "n";
        }
        if (this.isDirectlyEast()) {
            return "e";
        }
        if (this.isDirectlySouth()) {
            return "s";
        }
        if (this.isDirectlyWest()) {
            return "w";
        }
        return "x";
    }
}
