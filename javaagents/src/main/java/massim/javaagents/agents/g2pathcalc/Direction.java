package massim.javaagents.agents.g2pathcalc;

public enum Direction {

    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }
<<<<<<< Updated upstream
=======

    @Override
    public String toString() {
        return abbreviation;
    }
    
    public static Direction getDirectionOfString(String dirString) {
    	switch(dirString) {
    	case ("n"):{
    		return NORTH;
    	}
    	case ("e"):{
    		return EAST;
    	}
    	case ("s"):{
    		return SOUTH;
    	}
    	case ("W"):{
    		return WEST;
    	}
    	default:{
    		return null;
    	}
    	}
    	
    }
>>>>>>> Stashed changes
}
