package massim.javaagents.agents.g2utils;

public enum Orientation {
NORTH, EAST, SOUTH, WEST;
	
	public static Orientation changeOrientation(Orientation orient, int change) {
		if (change == 1) {
			switch (orient) {
			case NORTH:
				return EAST;
			case EAST:
				return SOUTH;
			case SOUTH:
				return WEST;
			case WEST:
				return NORTH;
			default:
				return SOUTH;
			}
		} else if (change == -1) {
			switch (orient) {
			case NORTH:
				return WEST;
			case EAST:
				return NORTH;
			case SOUTH:
				return EAST;
			case WEST:
				return SOUTH;
			default:
				return NORTH;
			}
		} else {
			return NORTH;
		}
		
	}
}
