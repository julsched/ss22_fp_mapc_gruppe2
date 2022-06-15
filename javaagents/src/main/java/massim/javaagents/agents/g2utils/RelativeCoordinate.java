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

    public static RelativeCoordinate getRelativeCoordinate(String direction) {
        switch(direction) {
            case "n" -> {
                return new RelativeCoordinate(0, -1);
            }
            case "e" -> {
                return new RelativeCoordinate(1, 0);
            }
            case "s" -> {
                return new RelativeCoordinate(0, 1);
            }
            case "w" -> {
                return new RelativeCoordinate(-1, 0);
            }
        }
        return null;
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
    
    @Override
    public boolean equals(Object o) {
    	if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        
        RelativeCoordinate rCo = (RelativeCoordinate) o;
        
        if (this.x != rCo.x) {
        	return false;
        }
        if (this.y != rCo.y) {
        	return false;
        }
        
        return true;
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

    public boolean isNextToAgent() {
        if (isOneStepNorth() || isOneStepEast() || isOneStepSouth() || isOneStepWest()) {
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

    public boolean isStraightNorth() {
        if (this.x == 0 && this.y < 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightEast() {
        if (this.x > 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightSouth() {
        if (this.x == 0 && this.y > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightWest() {
        if (this.x < 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getDirectDirection() {
        if (this.isStraightNorth()) {
            return "n";
        }
        if (this.isStraightEast()) {
            return "e";
        }
        if (this.isStraightSouth()) {
            return "s";
        }
        if (this.isStraightWest()) {
            return "w";
        }
        return "x";
    }
    
    public String toString() {
    	return "( "+ this.x + ", "+ this.y+ " )";
    }
    
    @Override
	public int hashCode() {

		Integer xInt = this.x;
		Integer yInt = this.y;
		int result = 7*xInt.hashCode() + 11*yInt.hashCode();
		return result;
		
	}
    
}
