package massim.javaagents.agents.g2pathcalc;

public class Node {

    final int x;
    final int y;
    final Direction initialDir;

    public Node(int x, int y, Direction initialDir) {
        this.x = x;
        this.y = y;
        this.initialDir = initialDir;
    }
}
