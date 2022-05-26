package massim.javaagents.agents.g2pathcalc;

import massim.javaagents.agents.g2utils.*;
import java.util.*;

public class PathCalc {

    public static Direction calculateShortestPath(int vision, List<RelativeCoordinate> occupiedFields, List<RelativeCoordinate> attachedBlocks, List<RelativeCoordinate> destinations) {
        // Boolean array representing a map (true means field is occupied)
        boolean[][] map = new boolean[2 * vision + 3][2 * vision + 3];
        // Position of agent inside the map (center)
        int xA = vision + 1;
        int yA = vision + 1;

        // Fill fields surrounding the vision zone with 'true'
        map[0][vision + 1] = true;
        //System.out.println("map[" + 0 + "][" + (vision + 1) + "] = true");
        map[2 * vision + 2][vision + 1] = true;
        //System.out.println("map[" + (2 * vision + 2) + "][" + (vision + 1) + "] = true");
        map[vision + 1][0] = true;
        //System.out.println("map[" + (vision + 1) + "][" + 0 + "] = true");
        map[vision + 1][2 * vision + 2] = true;
        //System.out.println("map[" + (vision + 1) + "][" + (2 * vision + 2) + "] = true");
        for (int n = 0; n < (2 * vision + 3); n++) {
            if (n != 0 && n != (vision + 1) && n != (2 * vision + 2)) {
                int range = Math.abs((vision + 1) - n);
                for (int x = 1; x <= range; x++) {
                    map[x][n] = true;
                    //System.out.println("map[" + x + "][" + n + "] = true");
                }
                for (int x = (2 * vision + 1); x > (2 * vision + 1 - range); x--) {
                    map[x][n] = true;
                    //System.out.println("map[" + x + "][" + n + "] = true");
                }
            }
        }

        //Fill occupied fields inside the vision zone with 'true'
        //System.out.println("Occupied fields:");
        for (RelativeCoordinate field : occupiedFields) {
            if (!attachedBlocks.contains(field)) {
                int x = field.getX();
                int y = field.getY();
                map[x + vision + 1][y + vision + 1] = true;
                //System.out.println("map[" + (x + vision + 1) + "][" + (y + vision + 1) + "] = true");
            } 
        }

        // Boolean array for keeping track of the fields that have been already analyzed
        boolean[][] discovered = new boolean[2 * vision + 3][2 * vision + 3];
        discovered[xA][yA] = true;


        // Start of algorithm
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(new Node(xA, yA, null));
        while (!queue.isEmpty()) {
            Node node = queue.poll();

            // TODO: check if agent can walk in certain direction when it has block(s) attached in this direction

            for (Direction dir : Direction.values()) {
                int newX = node.x + dir.getDx();
                int newY = node.y + dir.getDy();
                Direction newDir = node.initialDir == null ? dir : node.initialDir;
                for (RelativeCoordinate destination : destinations) {
                    int xG = destination.getX() + vision + 1;
                    int yG = destination.getY() + vision + 1;
                    // Destination reached?
                    if (newX == xG && newY == yG) {
                        return newDir;
                    }
                }

                // Is there a path in the direction and has that field not yet been analyzed?
                if (!map[newX][newY] && !discovered[newX][newY]) {
                    // Mark field as 'discovered' and add it to the queue
                    discovered[newX][newY] = true;
                    queue.add(new Node(newX, newY, newDir));
                }
            }
        }
        return null;
    }
}
