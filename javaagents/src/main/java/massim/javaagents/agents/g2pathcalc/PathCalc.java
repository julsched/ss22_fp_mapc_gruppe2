package massim.javaagents.agents.g2pathcalc;

import massim.javaagents.agents.g2utils.*;
import java.util.*;

public class PathCalc extends PathCalcSuper{

<<<<<<< Updated upstream
    public static Direction calculateShortestPath(int vision, List<RelativeCoordinate> occupiedFields, List<RelativeCoordinate> attachedBlocks, Set<RelativeCoordinate> destinations) {
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
=======

	public PathCalc(MapManagement mapManager, List<Block> attachedBlocks) {
		super(mapManager, attachedBlocks);
	}

	/**
	 * Determines the direction of the closest destination taking into account
	 * obstacles/entities/blocks on the way
	 * 
	 * @param destinations The possible destinations
	 * 
	 * @return The direction the agent should walk towards
	 */
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
			// TODO: Enable agent to walk two steps

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
						RelativeCoordinate absolutePosition = new RelativeCoordinate(newX + attachedBlock.getX(),
								newY + attachedBlock.getY());
						occupied = checkIfOccupied(absolutePosition);
						if (occupied) {
							break;
						}
					}
				}
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
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
=======

	/**
	 * Determines the number of steps to the closest dispenser of the required type
	 * taking into account obstacles/entities/blocks on the way
	 * 
	 * @param dispenserType The required dispenser type
	 * 
	 * @return The number of steps
	 */
	public int calcStepsToNextDispenser(String dispenserType) {
		Set<RelativeCoordinate> destinations = determineDispenserCandidates(dispenserType);
		if (destinations == null || destinations.size() == 0) {
			return -1; // Error - no dispenser of required type found
		}
		List<RelativeCoordinate> attachedBlocksRelative = getRelativeCoordinates(attachedBlocks);
		RelativeCoordinate currentPos = mapManager.getCurrentPosition();
		HashMap<String, RelativeCoordinate> mapDimensions = mapManager.analyzeMapDimensions();
		HashMap<RelativeCoordinate, Block> blockLayer = mapManager.getBlockLayer();
		HashMap<RelativeCoordinate, Obstacle> obstacleLayer = mapManager.getObstacleLayer();
		HashMap<RelativeCoordinate, Entity> entityLayer = mapManager.getEntityLayer();

		if (destinations.contains(currentPos)) {
			return 0;
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
			// TODO: Enable agent to walk two steps

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
						RelativeCoordinate absolutePosition = new RelativeCoordinate(newX + attachedBlock.getX(),
								newY + attachedBlock.getY());
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
							System.out.println("Closest: (" + newX + "|" + newY + ")");
							return node.stepNum + 1;
						}
					}
					// Mark field as 'discovered' and add it to the queue
					discovered.add(new RelativeCoordinate(newX, newY));
					queue.add(new Node(newX, newY, newDir, node.stepNum + 1));
				}
			}
		}
		return -2; // Error - no path found
	}

	/**
	 * Determines the direction to get to the closest obstacle
	 * 
	 * @return The direction the agent should walk towards
	 */
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
>>>>>>> Stashed changes
}
