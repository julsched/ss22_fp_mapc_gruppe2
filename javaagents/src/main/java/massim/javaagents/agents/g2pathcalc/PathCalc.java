package massim.javaagents.agents.g2pathcalc;

import massim.javaagents.agents.g2utils.*;
import java.util.*;

public class PathCalc {

    public static Direction calculateShortestPathMap(HashMap<RelativeCoordinate, List<Cell>> map, RelativeCoordinate currentPos, Set<RelativeCoordinate> destinations, List<RelativeCoordinate> attachedBlocks, HashMap<String, RelativeCoordinate> mapDimensions) {
        if (destinations == null || destinations.size() == 0) {
            return null;
        }
        
        // Position of agent inside the map
        int xA = currentPos.getX();
        int yA = currentPos.getY();

        // Set for keeping track of the fields that have been already analyzed
        Set<RelativeCoordinate> discovered = new HashSet<>();
        discovered.add(currentPos);


        // Start of algorithm
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(new Node(xA, yA, null));
        while (!queue.isEmpty()) {
            Node node = queue.poll();

            // TODO: take into account that agent can rotate in order to fit on a path

            for (Direction dir : Direction.values()) {
                int newX = node.x + dir.getDx();
                int newY = node.y + dir.getDy();
                if (newX < 0) {
                    int x = mapDimensions.get("west").getX();
                    if (newX < (x - 5)) {
                        continue;
                    }
                } else {
                    int x = mapDimensions.get("east").getX();
                    if (newX > (x + 5)) {
                        continue;
                    }
                }
                if (newY < 0) {
                    int y = mapDimensions.get("north").getY();
                    if (newY < (y - 5)) {
                        continue;
                    }
                } else {
                    int y = mapDimensions.get("south").getY();
                    if (newY > (y + 5)) {
                        continue;
                    }
                }

                // Check if the cell is occupied
                boolean occupied = false;
                List<Cell> cells = map.get(new RelativeCoordinate(newX, newY));
                if (cells != null && !cells.isEmpty()) {
                    for (Cell cell : cells) {
                        // TODO: add '|| cell instanceof Entity' (once Entity implements Cell)
                        if (cell instanceof Obstacle || cell instanceof Block) {
                            occupied = true;
                            break;
                        }
                    }              
                }

                // Check if attachedBlocks of agents fit into the cell's surrounding cells
                if (!occupied) {
                    for (RelativeCoordinate attachedBlock : attachedBlocks) {
                        RelativeCoordinate absolutePosition = new RelativeCoordinate(newX + attachedBlock.getX(), newY + attachedBlock.getY());
                        List<Cell> cells2 = map.get(absolutePosition);
                        if (cells2 != null && !cells2.isEmpty()) {
                            for (Cell cell : cells2) {
                                // TODO: add '|| cell instanceof Entity' (once Entity implements Cell)
                                if (cell instanceof Obstacle || cell instanceof Block) {
                                    occupied = true;
                                    break;
                                }
                            }
                        }
                        if (occupied) {
                            break;
                        }
                    }
                }
                
                // Is there a path in the direction and has that field not yet been analyzed?
                if (!occupied && !discovered.contains(new RelativeCoordinate(newX, newY))) {
                    Direction newDir = node.initialDir == null ? dir : node.initialDir;
                    for (RelativeCoordinate destination : destinations) {
                        int xG = destination.getX();
                        int yG = destination.getY();
                        // Destination reached?
                        if (newX == xG && newY == yG) {
                            return newDir;
                        }
                    }
                    // Mark field as 'discovered' and add it to the queue
                    discovered.add(new RelativeCoordinate(newX, newY));
                    queue.add(new Node(newX, newY, newDir));
                }
            }
        }
        return null;
    }
}
