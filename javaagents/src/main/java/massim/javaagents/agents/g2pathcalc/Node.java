package massim.javaagents.agents.g2pathcalc;

import java.util.*;

public class Node {

    final int x;
    final int y;
    final List<Direction> initialDirs;
    int stepNum;

    public Node(int x, int y, int stepNum) {
        this.x = x;
        this.y = y;
        this.initialDirs = null;
        this.stepNum = stepNum;
    }

    public Node(int x, int y, List<Direction> initialDirs) {
        this.x = x;
        this.y = y;
        this.initialDirs = initialDirs;
    }
}
