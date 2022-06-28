package massim.javaagents.agents.g2pathcalc;

public enum Direction {

	NORTH(0, -1, "n"), EAST(1, 0, "e"), SOUTH(0, 1, "s"), WEST(-1, 0, "w");

	private final int dx;
	private final int dy;
	private final String abbreviation;

	Direction(int dx, int dy, String abbreviation) {
		this.dx = dx;
		this.dy = dy;
		this.abbreviation = abbreviation;
	}

	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

	@Override
	public String toString() {
		return abbreviation;
	}

	public static Direction getDirectionFromString(String dir) {
		switch (dir) {
		case ("n"): {
			return NORTH;
		}
		case ("e"): {
			return EAST;
		}
		case ("s"): {
			return SOUTH;
		}
		case ("w"): {
			return WEST;
		}
		default: {
			return null;
		}
		}

	}

	public static Direction getDirectionFromInts(int x, int y) {

		if (x == 0 && y == -1) {
			return NORTH;
		}
		if (x == 1 && y == 0) {
			return EAST;
		}
		if (x == 0 && y == 1) {
			return SOUTH;
		}
		if (x == -1 && y == 0) {
			return WEST;
		} else {
			return null;
		}
	}

}
