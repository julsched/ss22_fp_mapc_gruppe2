package massim.javaagents.agents.g2pathcalc;

public class Node {

    final int x;
    final int y;
    final Direction initialDir;
    int stepNum = 0;

    public Node(int x, int y, Direction initialDir, int stepNum) {
        this.x = x;
        this.y = y;
        this.initialDir = initialDir;
        this.stepNum = stepNum;
    }

    public Node(int x, int y, Direction initialDir) {
        this.x = x;
        this.y = y;
        this.initialDir = initialDir;
    }
}
