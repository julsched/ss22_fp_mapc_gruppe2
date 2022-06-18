package massim.javaagents.agents.g2pathcalc;

public enum Direction {

    NORTH(0, -1, "n"),
    EAST(1, 0, "e"),
    SOUTH(0, 1, "s"),
    WEST(-1, 0, "w");

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
}
