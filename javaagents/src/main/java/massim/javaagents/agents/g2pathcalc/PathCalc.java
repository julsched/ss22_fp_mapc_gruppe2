package massim.javaagents.agents.g2pathcalc;

import massim.javaagents.agents.g2utils.*;
import java.util.*;

public class PathCalc {

    private MapManagement mapManager;
    private List<Block> attachedBlocks; // relative coordinates

    public PathCalc(MapManagement mapManager, List<Block> attachedBlocks) {
        this.mapManager = mapManager;
        this.attachedBlocks = attachedBlocks;
    }

    public String calculateShortestPathVision(int vision, List<RelativeCoordinate> occupiedFields, Set<RelativeCoordinate> destinations) {
        List<RelativeCoordinate> attachedBlockCoordinates = getRelativeCoordinates(attachedBlocks);
        
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
            if (!attachedBlockCoordinates.contains(field)) {
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
                        return newDir.toString();
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

    public String calculateShortestPathMap(Set<RelativeCoordinate> destinations) {
        if (destinations == null || destinations.size() == 0) {
            return null;
        }
        
        List<RelativeCoordinate> attachedBlocksRelative = getRelativeCoordinates(attachedBlocks);
        RelativeCoordinate currentPos = mapManager.getCurrentPosition();
        HashMap<String, RelativeCoordinate> mapDimensions = mapManager.analyzeMapDimensions();
        HashMap<RelativeCoordinate, Block> blockLayer = mapManager.getBlockLayer();
        HashMap<RelativeCoordinate, Obstacle> obstacleLayer = mapManager.getObstacleLayer();
        HashMap<RelativeCoordinate, Entity> entityLayer = mapManager.getEntityLayer();

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
            // TODO: save number of steps required (not just direction)
            // TODO: Enable agent to walk two steps
            // TODO: get to closest dispenser of specific type (return the Dispenser of destinations which equals :))

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
                boolean occupied = checkIfOccupied(new RelativeCoordinate(newX, newY));

                // Check if attachedBlocks of agent fit into the cell's surrounding cells
                if (!occupied) {
                    for (RelativeCoordinate attachedBlock : attachedBlocksRelative) {
                        RelativeCoordinate absolutePosition = new RelativeCoordinate(newX + attachedBlock.getX(), newY + attachedBlock.getY());
                        occupied = checkIfOccupied(absolutePosition);
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
                            System.out.println("Destination: (" + newX + "|" + newY + ")");
                            return newDir.toString();
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

    public boolean checkIfOccupied(RelativeCoordinate coordinate) {
        int x = coordinate.getX();
        int y = coordinate.getY();
        HashMap<RelativeCoordinate, Obstacle> obstacleLayer = mapManager.getObstacleLayer();
        HashMap<RelativeCoordinate, Block> blockLayer = mapManager.getBlockLayer();
        HashMap<RelativeCoordinate, Entity> entityLayer = mapManager.getEntityLayer();
        List<RelativeCoordinate> attachedBlocksAbsolute = getAbsoluteCoordinates(attachedBlocks);
        boolean occupied = false;

        // Check if cell is occupied by an obstacle
        Obstacle obstacle = obstacleLayer.get(new RelativeCoordinate(x, y));
        if (obstacle != null) {
            occupied = true;
        }

        // Check if cell is occupied by an entity
        Entity entity = entityLayer.get(new RelativeCoordinate(x, y));
        if (entity != null) {
            occupied = true;
        }

        // Check if cell is occupied by a (non-attached) block
        if (!occupied) {
            Block block = blockLayer.get(new RelativeCoordinate(x, y));
            if (block != null && !attachedBlocksAbsolute.contains(new RelativeCoordinate(x, y))) {
                occupied = true;
            }
        }
        return occupied;
    }

    public List<RelativeCoordinate> getRelativeCoordinates(List<Block> blocks) {
        List<RelativeCoordinate> coordinates = new ArrayList<>();
        if (blocks != null && !blocks.isEmpty()) {
			for (Block block : blocks) {
				coordinates.add(block.getRelativeCoordinate());
			}	
		}
		return coordinates;
    }

    public List<RelativeCoordinate> getAbsoluteCoordinates(List<Block> blocks) {
        List<RelativeCoordinate> coordinatesRelative = getRelativeCoordinates(blocks);
        List<RelativeCoordinate> coordinatesAbsolute = new ArrayList<>();
        if (coordinatesRelative != null && !coordinatesRelative.isEmpty()) {
			for (RelativeCoordinate relativeCoordinate : coordinatesRelative) {
                coordinatesAbsolute.add(new RelativeCoordinate(mapManager.getCurrentPosition().getX() + relativeCoordinate.getX(), 
                    mapManager.getCurrentPosition().getY() + relativeCoordinate.getY()));
            }
		}
		return coordinatesAbsolute;
    }

    /**
	 * Determines non-occupied goal zone cells which have enough space around them for the task to be submitted
	 * 
	 * @param currentTask The task to be submitted
	 * 
	 * @return The absolute coordinates of the identified goal zone cells
	 */
	public Set<RelativeCoordinate> determineGoalZoneFieldCandidates(Task currentTask) {
		// First check which goal zone cells are free (no obstacle/block/entity)
		List<RelativeCoordinate> goalZoneFieldsFree = new ArrayList<>();
		HashMap<RelativeCoordinate, Goalzone> goalzoneLayer = mapManager.getGoalzoneLayer();
		for (Map.Entry<RelativeCoordinate, Goalzone> entry : goalzoneLayer.entrySet()) {
			if (entry.getValue() != null) {
				boolean occupied = checkIfOccupied(entry.getKey());
				if (!occupied) {
					goalZoneFieldsFree.add(entry.getKey());
				}
			}
		}

		// Check which ones of the free goal zone fields have enough space around
		// them to submit the current task (surroundings fields do not have to be goal zone fields)
		Set<RelativeCoordinate> goalZoneFieldCandidates = new HashSet<>();
		for (RelativeCoordinate goalZoneField : goalZoneFieldsFree) {
			// TODO: Adjust for multi-block tasks
			RelativeCoordinate requirement = currentTask.getRequirements().get(0).getRelativeCoordinate();
			RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(goalZoneField.getX() + requirement.getX(),
					goalZoneField.getY() + requirement.getY());
			if (!checkIfOccupied(fieldToBeChecked)) {
				goalZoneFieldCandidates.add(goalZoneField);
			}
		}
		return goalZoneFieldCandidates;
	}

    /**
	 * Determines dispensers of the required type
	 * 
	 * @param dispenserType The required dispenser type
	 * 
	 * @return The absolute coordinates of the identified dispensers
	 */
	public Set<RelativeCoordinate> determineDispenserCandidates(String dispenserType) {
        // Atm not checking if dispenser is occupied
        Set<RelativeCoordinate> dispenserCandidates = new HashSet<>();
		HashMap<RelativeCoordinate, Dispenser> dispenserLayer = mapManager.getDispenserLayer();
		for (Map.Entry<RelativeCoordinate, Dispenser> entry : dispenserLayer.entrySet()) {
			if (entry.getValue() != null) {
                if (entry.getValue().getType().equals(dispenserType)) {
                    dispenserCandidates.add(entry.getKey());
                }
			}
		}
        return dispenserCandidates;
    }

    public int calcStepsToNextDispenser(String dispenserType) {
        // TODO
        return 0;
    }

    public String calculateShortestPathNextObstacle() {
        Set<RelativeCoordinate> obstacles = new HashSet<>();
        HashMap<RelativeCoordinate, Obstacle> obstacleLayer = mapManager.getObstacleLayer();
		for (Map.Entry<RelativeCoordinate, Obstacle> entry : obstacleLayer.entrySet()) {
			if (entry.getValue() != null) {
                obstacles.add(entry.getKey());
            }
        }
        return calculateShortestPathMap(obstacles);
    }
}
