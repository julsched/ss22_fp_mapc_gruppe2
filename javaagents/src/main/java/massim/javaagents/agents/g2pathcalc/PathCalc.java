package massim.javaagents.agents.g2pathcalc;

import eis.iilang.*;
import massim.javaagents.agents.g2utils.*;
import java.util.*;

public class PathCalc {

	private MapManagement mapManager;
	private List<Block> attachedBlocks; // relative coordinates
	private Role currentRole = null;

	public PathCalc(MapManagement mapManager, List<Block> attachedBlocks) {
		this.mapManager = mapManager;
		this.attachedBlocks = attachedBlocks;
	}

	public void setCurrentRole(Role role) {
		this.currentRole = role;
	}

	public Action calculateShortestPathVision(int vision, List<RelativeCoordinate> occupiedFields,
			Set<RelativeCoordinate> destinations) {
		List<RelativeCoordinate> attachedBlockCoordinates = getRelativeCoordinates(attachedBlocks);
		int speed = 1; //
		if (currentRole != null) { // TODO: Investigate why currentRole not set sometimes
			speed = currentRole.getCurrentSpeed();
		}
		if (speed == 0) {
			// Too many things attached, agent cannot move.
			return null;
		}

		// Boolean array representing a map (true means field is occupied)
		boolean[][] map = new boolean[2 * vision + 3][2 * vision + 3];
		// Position of agent inside the map (center)
		int xA = vision + 1;
		int yA = vision + 1;

		// Fill fields surrounding the vision zone with 'true'
		map[0][vision + 1] = true;
		map[2 * vision + 2][vision + 1] = true;
		map[vision + 1][0] = true;
		map[vision + 1][2 * vision + 2] = true;
		for (int n = 0; n < (2 * vision + 3); n++) {
			if (n != 0 && n != (vision + 1) && n != (2 * vision + 2)) {
				int range = Math.abs((vision + 1) - n);
				for (int x = 1; x <= range; x++) {
					map[x][n] = true;
				}
				for (int x = (2 * vision + 1); x > (2 * vision + 1 - range); x--) {
					map[x][n] = true;
				}
			}
		}

		// Fill occupied fields inside the vision zone with 'true'
		for (RelativeCoordinate field : occupiedFields) {
			if (!attachedBlockCoordinates.contains(field)) {
				int x = field.getX();
				int y = field.getY();
				map[x + vision + 1][y + vision + 1] = true;
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

			// TODO: check if agent can walk in certain direction when it has block(s)
			// attached in this direction

			for (Direction dir : Direction.values()) {
				int newX = node.x + dir.getDx();
				int newY = node.y + dir.getDy();
				List<Direction> newDirList;
				if (node.initialDirs == null) {
					newDirList = new ArrayList<>();
					newDirList.add(dir);
				} else if (node.initialDirs.size() < speed) {
					newDirList = new ArrayList<Direction>(node.initialDirs);
					newDirList.add(dir);
				} else {
					newDirList = new ArrayList<Direction>(node.initialDirs);
				}
				for (RelativeCoordinate destination : destinations) {
					int xG = destination.getX() + vision + 1;
					int yG = destination.getY() + vision + 1;
					// Destination reached?
					if (newX == xG && newY == yG) {
						int steps;
						if (newDirList.size() < speed) {
							steps = newDirList.size();
						} else {
							steps = speed;
						}
						switch (steps) {
						case 1:
							return new Action("move", new Identifier(newDirList.get(0).toString()));
						case 2:
							return new Action("move", new Identifier(newDirList.get(0).toString()),
									new Identifier(newDirList.get(1).toString()));
						case 3:
							return new Action("move", new Identifier(newDirList.get(0).toString()),
									new Identifier(newDirList.get(1).toString()),
									new Identifier(newDirList.get(2).toString()));
						default:
							System.out.println("Speed " + speed + " not (yet) supported by PathCalc");}
						if (steps == 0) {
							return null;
						}
						List<Parameter> params = new ArrayList<>();
						for (int i = 0; i < steps; i++) {
							params.add(new Identifier(newDirList.get(i).toString()));
						}
						return new Action("move", params);
					}
				}

				// Is there a path in the direction and has that field not yet been analyzed?
				if (!map[newX][newY] && !discovered[newX][newY]) {
					// Mark field as 'discovered' and add it to the queue
					discovered[newX][newY] = true;
					queue.add(new Node(newX, newY, newDirList));
				}
			}
		}
		return null;
	}

	/**
	 * Determines the direction towards the given dispenser taking into account
	 * obstacles/entities/blocks on the way
	 * 
	 * @param disp The dispenser
	 * 
	 * @return The move action or null in case no path was identified
	 */
	public Action calculateShortestPathMap(Dispenser disp) {
		if (disp == null || disp.getRelativeCoordinate() == null) {
			return null;
		}
		RelativeCoordinate relativeCoordinate = disp.getRelativeCoordinate();
		if (checkIfOccupied(relativeCoordinate)) {
			return null;
		}
		RelativeCoordinate north = new RelativeCoordinate(relativeCoordinate.getX(), relativeCoordinate.getY() - 1);
		RelativeCoordinate east = new RelativeCoordinate(relativeCoordinate.getX() + 1, relativeCoordinate.getY());
		RelativeCoordinate south = new RelativeCoordinate(relativeCoordinate.getX(), relativeCoordinate.getY() + 1);
		RelativeCoordinate west = new RelativeCoordinate(relativeCoordinate.getX() - 1, relativeCoordinate.getY());
		Set<RelativeCoordinate> destinations = new HashSet<>();
		destinations.add(north);
		destinations.add(east);
		destinations.add(south);
		destinations.add(west);
		return calculateShortestPathMap(destinations);
	}

	/**
	 * Determines the direction of the closest destination taking into account
	 * obstacles/entities/blocks on the way
	 * 
	 * @param destinations The possible destinations
	 * 
	 * @return The move action or null in case no path was identified
	 */
	public Action calculateShortestPathMap(Set<RelativeCoordinate> destinations) {
		if (destinations == null || destinations.size() == 0) {
			return null;
		}

		int speed = 1; //
		if (currentRole != null) { // TODO: Investigate why currentRole not set sometimes
			speed = currentRole.getCurrentSpeed();
		}

		if (speed == 0) {
			// Too many things attached, agent cannot move.
			return null;
		}
		List<RelativeCoordinate> attachedBlocksRelative = getRelativeCoordinates(attachedBlocks);
		RelativeCoordinate currentPos = mapManager.getPosition();
		HashMap<String, RelativeCoordinate> mapDimensions = mapManager.analyzeMapDimensions();

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
					List<Direction> newDirList;
					if (node.initialDirs == null) {
						newDirList = new ArrayList<>();
						newDirList.add(dir);
					} else if (node.initialDirs.size() < speed) {
						newDirList = new ArrayList<Direction>(node.initialDirs);
						newDirList.add(dir);
					} else {
						newDirList = new ArrayList<Direction>(node.initialDirs);
					}

					for (RelativeCoordinate destination : destinations) {
						int xG = destination.getX();
						int yG = destination.getY();
						// Destination reached?
						if (newX == xG && newY == yG) {
							System.out.println("Destination: (" + newX + "|" + newY + ")");
							int steps;
							if (newDirList.size() < speed) {
								steps = newDirList.size();
							} else {
								steps = speed;
							}
							switch (steps) {
							case 1:
								return new Action("move", new Identifier(newDirList.get(0).toString()));
							case 2:
								return new Action("move", new Identifier(newDirList.get(0).toString()),
										new Identifier(newDirList.get(1).toString()));
							case 3:
								return new Action("move", new Identifier(newDirList.get(0).toString()),
										new Identifier(newDirList.get(1).toString()),
										new Identifier(newDirList.get(2).toString()));
							default:
								System.out.println("Speed " + speed + " not (yet) supported by PathCalc");
							}
							if (steps == 0) {
								return null;
							}
							List<Parameter> params = new ArrayList<>();
							for (int i = 0; i < steps; i++) {
								params.add(new Identifier(newDirList.get(i).toString()));
							}
							return new Action("move", params);
						}
					}
					// Mark field as 'discovered' and add it to the queue
					discovered.add(new RelativeCoordinate(newX, newY));
					queue.add(new Node(newX, newY, newDirList));
				}
			}
		}
		return null;
	}

	public Action calculateShortestPathManhattan(Set<RelativeCoordinate> destinations) {
		if (destinations == null || destinations.size() == 0) {
			System.out.println("NO DESTINATIONS!");
			return null;
		}

		int speed = 1; //
		if (currentRole != null) { // TODO: Investigate why currentRole not set sometimes
			speed = currentRole.getCurrentSpeed();
		}

		if (speed == 0) {
			// Too many things attached, agent cannot move.
			System.out.println("TOO MANY THINGS ATTACHED, CANT MOVE");
			return null;
		}
		// Calculate Dir(s) to go to
		// Add Dir to go to to List
		// Walk all steps you can

		RelativeCoordinate pos = mapManager.getPosition();
		ArrayList<String> directions = new ArrayList<>();
		HashMap<RelativeCoordinate, Obstacle> obstacles = mapManager.getObstacleLayer();
		System.out.println("CALCULATING SHORTEST MANHATTAN");
		for (int i = 1; i <= speed; i++) {
			RelativeCoordinate destination = getClosestDestAnyPosManhattan(pos, destinations);
			if (destination == null) {
				System.out.println("GET CLOSEST ANY POS IS NULL");
				break;
			}

			// Position of agent inside the map
			int xA = pos.getX();
			int yA = pos.getY();

			int xdest = destination.getX();
			int ydest = destination.getY();

			int xDist = xdest - xA;
			int yDist = ydest - yA;

			if (xDist == 0 && yDist == 0) { // already at destination
				System.out.println("ALREADY AT DESTINATION");
				break;
			}

			String dir = "";
			if (xDist != 0) {
				if (xDist < 1) {
					dir = "w";

				} else {
					dir = "e";
				}
			}
			if (yDist != 0) {
				if (yDist < 1) {
					dir = "n";
				} else {
					dir = "s";
				}
			}

			pos = pos.getCoordAfterWalkingInDir(dir);
			System.out.println("DIR IDENTIFIED: " + dir);
			// if one block attached, always make sure, agent drags it behind him
			if (attachedBlocks.size() == 1) {
				System.out.println("ONE BLOCK ATTACHED MAYBE ROTATE");
				String attachedBlockDir = getBlockDir(attachedBlocks.get(0));
				if (getOppositeDirection(attachedBlockDir) != dir) {
					System.out.println("NOT DRAGGING BLOCK!");
					if (directions.size() == 0) { // if agent needs to rotate now, rotate
						System.out.println("NEED TO ROTATE NOW!");
						RelativeCoordinate blockPos = pos.getCoordAfterWalkingInDir(getOppositeDirection(dir)).getCoordAfterWalkingInDir(getOppositeDirection(dir));
						if (obstacles.containsKey(blockPos) && obstacles.get(blockPos) != null) { // if rotation is blocked, clear
																					// so rotation is possible
							System.out.println("CANT ROTATE NOW; CLEAR FIRST: " + getOppositeDirection(dir));
//							return clear(pos.getDirectDirection());
							return clear(getOppositeDirection(dir));
						}
						System.out.println("ROTATING! dir "+dir+ " block: "+attachedBlockDir);
						return rotateAccordingToAttachedBlock(dir, attachedBlockDir);
					}
					break;
				}
			}

			if (obstacles.containsKey(pos) && obstacles.get(pos) != null) {
				System.out.println("DESTINATION IS OCCUPIED");
				if (directions.size() == 0) { // if agent needs to clear now, clear
					System.out.println("NOW CLEARING IN DIR " + dir);
					Action action = new Action("clear", new Numeral(xdest), new Numeral(ydest));
					System.out.println("ACTION: " + action + "CLEARING ( " + xdest + " | " + ydest + " )");
					System.out.println("I AM HERE  " + mapManager.getPosition());
					return clear(dir);
				}
				break;
			}
			System.out.println("ADDING DIR");
			directions.add(dir);
		}

		int steps = speed;

		if (directions.size() <= speed) {
			steps = directions.size();
		}
		System.out.println("NOW SWITCHING STEPS " + steps);
		switch (steps) {
		case 1:
			return new Action("move", new Identifier(directions.get(0)));
		case 2:
			return new Action("move", new Identifier(directions.get(0)), new Identifier(directions.get(1)));
		case 3:
			return new Action("move", new Identifier(directions.get(0)), new Identifier(directions.get(1)),
					new Identifier(directions.get(2)));
		default:
			System.out.println("Speed " + speed + " not (yet) supported by PathCalc");
			return null;
		}

	}

	private Action clear(String dir) {
		int x = 0;
		int y = 0;
		switch (dir) {
		case ("n"): {
			x = 0;
			y = -1;
			break;
		}
		case ("e"): {
			x = 1;
			y = 0;
			break;
		}
		case ("s"): {
			x = 0;
			y = 1;
			break;
		}
		case ("w"): {
			x = -1;
			y = 0;
			break;
		}
//		default: {
//			return new Action("skip");
//		}
		}
		return new Action("clear", new Numeral(x), new Numeral(y));
	}

	/**
	 * Determines the direction towards the given dispenser
	 * 
	 * @param disp The dispenser
	 * 
	 * @return The move action or null in case no path was identified
	 */
	public Action calculateShortestPathManhattan(Dispenser disp) {
		if (disp == null || disp.getRelativeCoordinate() == null) {
			return null;
		}
		RelativeCoordinate relativeCoordinate = disp.getRelativeCoordinate();
		if (checkIfOccupied(relativeCoordinate)) {
			return null;
		}
		RelativeCoordinate north = new RelativeCoordinate(relativeCoordinate.getX(), relativeCoordinate.getY() - 1);
		RelativeCoordinate east = new RelativeCoordinate(relativeCoordinate.getX() + 1, relativeCoordinate.getY());
		RelativeCoordinate south = new RelativeCoordinate(relativeCoordinate.getX(), relativeCoordinate.getY() + 1);
		RelativeCoordinate west = new RelativeCoordinate(relativeCoordinate.getX() - 1, relativeCoordinate.getY());
		Set<RelativeCoordinate> destinations = new HashSet<>();
		destinations.add(north);
		destinations.add(east);
		destinations.add(south);
		destinations.add(west);
		return calculateShortestPathManhattan(destinations);
	}

	private Action rotateAccordingToAttachedBlock(String prefDir, String attachedBlockDir) {
		String rotationDir = "";
		if (attachedBlockDir.equals(prefDir)) { // Rotation direction irrelevant
			rotationDir = "cw";
		} else {
			switch (prefDir) {
			case ("n"): {
				if (attachedBlockDir.equals("e")) {
					rotationDir = "cw";
				} else {
					rotationDir = "ccw";
				}
				break;
			}
			case ("e"): {
				if (attachedBlockDir.equals("s")) {
					rotationDir = "cw";
				} else {
					rotationDir = "ccw";
				}
				break;
			}
			case ("s"): {
				if (attachedBlockDir.equals("w")) {
					rotationDir = "cw";
				} else {
					rotationDir = "ccw";
				}
				break;
			}
			case ("w"): {
				if (attachedBlockDir.equals("n")) {
					rotationDir = "cw";
				} else {
					rotationDir = "ccw";

				}
				break;
			}
			}

		}
		System.out.println("ROTATING "+ rotationDir);
		return new Action("rotate", new Identifier(rotationDir));

	}

	/**
	 * Returns the opposite direction
	 * 
	 * @param direction Direction for which opposite direction is required
	 * @return The opposite direction
	 */
	private String getOppositeDirection(String direction) {
		switch (direction) {
		case "n":
			return "s";
		case "e":
			return "w";
		case "s":
			return "n";
		case "w":
			return "e";
		case "cw":
			return "ccw";
		case "ccw":
			return "cw";
		default:
			return null;
		}
	}

	private String getBlockDir(Block b) {
		if ((attachedBlocks.size() == 1)) {
			if (b.distanceFromAgent() == 1) {
				return b.getDirectDirection();
			}
		}
		return "";
	}

	private RelativeCoordinate getClosestDestAnyPosManhattan(RelativeCoordinate pos,
			Set<RelativeCoordinate> destinations) {
		if (pos != null && destinations != null && destinations.size() != 0) {
			int shortestDist = -1;
			RelativeCoordinate destination = null;
			for (RelativeCoordinate dest : destinations) {
				if (shortestDist == -1) {
					shortestDist = manhattanDistFromPos(pos, dest);
				} else {
					int dist = manhattanDistFromPos(pos, dest);
					if (dist < shortestDist) {
						shortestDist = dist;
						destination = dest;
					}
				}

			}
			return destination;
		} else {
			return null;
		}
	}

	private RelativeCoordinate getClosestDestCurrentPosManhattan(Set<RelativeCoordinate> destinations) {
		RelativeCoordinate currentPos = mapManager.getPosition();
		return getClosestDestAnyPosManhattan(currentPos, destinations);
	}

	private int manhattanDistFromPos(RelativeCoordinate pos, RelativeCoordinate goalCoord) {
		if (pos != null && goalCoord != null) {
			// start Position
			int xA = pos.getX();
			int yA = pos.getY();

			int x = goalCoord.getX();
			int y = goalCoord.getY();
			return Math.abs(xA - x) + Math.abs(yA - y);
		}
		return -1;
	}

	private int manhattanDistFromCurrentPos(RelativeCoordinate coord) {
		RelativeCoordinate currentPos = mapManager.getPosition();
		return manhattanDistFromPos(currentPos, coord);
	}

	/**
	 * Checks if the provided coordinate is occupied by a block/entity/obstacle
	 * 
	 * @param coordinate The absolute coordinate to be checked
	 * 
	 * @return true if coordinate is occupied, otherwise false
	 */
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

	/**
	 * Provides a list of relative block coordinates
	 * 
	 * @param blocks The blocks the coordinates are required of
	 * 
	 * @return The list of relative block coordinates
	 */
	public List<RelativeCoordinate> getRelativeCoordinates(List<Block> blocks) {
		List<RelativeCoordinate> coordinates = new ArrayList<>();
		if (blocks != null && !blocks.isEmpty()) {
			for (Block block : blocks) {
				coordinates.add(block.getRelativeCoordinate());
			}
		}
		return coordinates;
	}

	/**
	 * Provides a list of absolute block coordinates
	 * 
	 * @param blocks The blocks the coordinates are required of
	 * 
	 * @return The list of absolute block coordinates
	 */
	public List<RelativeCoordinate> getAbsoluteCoordinates(List<Block> blocks) {
		List<RelativeCoordinate> coordinatesRelative = getRelativeCoordinates(blocks);
		List<RelativeCoordinate> coordinatesAbsolute = new ArrayList<>();
		if (coordinatesRelative != null && !coordinatesRelative.isEmpty()) {
			for (RelativeCoordinate relativeCoordinate : coordinatesRelative) {
				coordinatesAbsolute
						.add(new RelativeCoordinate(mapManager.getPosition().getX() + relativeCoordinate.getX(),
								mapManager.getPosition().getY() + relativeCoordinate.getY()));
			}
		}
		return coordinatesAbsolute;
	}

	/**
	 * Determines non-occupied goal zone cells which have enough space around them
	 * for the task to be submitted
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
		// them to submit the current task (surrounding fields do not have to be goal
		// zone fields)
		Set<RelativeCoordinate> goalZoneFieldCandidates = new HashSet<>();
		for (RelativeCoordinate goalZoneField : goalZoneFieldsFree) {
			// TODO: Adjust for multi-block tasks
			boolean enoughSpace = true;
			for (TaskRequirement requirement : currentTask.getRequirements()) {
				RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(
						goalZoneField.getX() + requirement.getRelativeCoordinate().getX(),
						goalZoneField.getY() + requirement.getRelativeCoordinate().getY());
				if (checkIfOccupied(fieldToBeChecked)) { // if (!goalZoneFieldsFree.contains(fieldToBeChecked)) {}
					enoughSpace = false;
					break;
				}
			}
			if (enoughSpace) {
				goalZoneFieldCandidates.add(goalZoneField);
			}
			RelativeCoordinate requirement = currentTask.getRequirements().get(0).getRelativeCoordinate();
			RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(goalZoneField.getX() + requirement.getX(),
					goalZoneField.getY() + requirement.getY());
			if (!checkIfOccupied(fieldToBeChecked)) { // if (goalZoneFieldsFree.contains(fieldToBeChecked)) {}
				goalZoneFieldCandidates.add(goalZoneField);
			}
		}
		return goalZoneFieldCandidates;
	}

	/**
	 * Determines non-occupied goal zone cells
	 * 
	 * @return The absolute coordinates of the identified goal zone cells
	 */
	public Set<RelativeCoordinate> determineGoalZoneFields() {
		// First check which goal zone cells are free (no obstacle/block/entity)
		Set<RelativeCoordinate> goalZoneFieldsFree = new HashSet<>();
		HashMap<RelativeCoordinate, Goalzone> goalzoneLayer = mapManager.getGoalzoneLayer();
		for (Map.Entry<RelativeCoordinate, Goalzone> entry : goalzoneLayer.entrySet()) {
			if (entry.getValue() != null) {
				boolean occupied = checkIfOccupied(entry.getKey());
				if (!occupied) {
					goalZoneFieldsFree.add(entry.getKey());
				}
			}
		}
		return goalZoneFieldsFree;
	}

    /**
	 * Determines non-occupied role zone cells which have enough space around them to fit on 
	 * 
	 * 
	 * @return The absolute coordinates of the identified role zone cells
	 */
	public Set<RelativeCoordinate> determineRoleZoneFieldCandidates() {
		// First check which role zone cells are free (no obstacle/block/entity)
		Set<RelativeCoordinate> roleZoneFieldCandidates = new HashSet<>();
		HashMap<RelativeCoordinate, Rolezone> rolezoneLayer = mapManager.getRolezoneLayer();
		for (Map.Entry<RelativeCoordinate, Rolezone> entry : rolezoneLayer.entrySet()) {
			if (entry.getValue() != null) {
//				boolean occupied = checkIfOccupied(entry.getKey());
//				if (!occupied) {
				roleZoneFieldCandidates.add(entry.getKey());
//				}
			}
		}

//		Set<RelativeCoordinate> roleZoneFieldCandidates = new HashSet<>();
//		for (RelativeCoordinate goalZoneField : goalZoneFieldsFree) {
//			// TODO: Adjust for multi-block tasks
//            boolean enoughSpace = true;
//            for (TaskRequirement requirement : currentTask.getRequirements()) {
//                RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(goalZoneField.getX() + requirement.getRelativeCoordinate().getX(),
//					goalZoneField.getY() + requirement.getRelativeCoordinate().getY());
//                if (checkIfOccupied(fieldToBeChecked)) { // if (!goalZoneFieldsFree.contains(fieldToBeChecked)) {}
//                    enoughSpace = false;
//                    break;
//                }
//            }
//            if (enoughSpace) {
//                goalZoneFieldCandidates.add(goalZoneField);
//            }
//			RelativeCoordinate requirement = currentTask.getRequirements().get(0).getRelativeCoordinate();
//			RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(goalZoneField.getX() + requirement.getX(),
//					goalZoneField.getY() + requirement.getY());
//			if (!checkIfOccupied(fieldToBeChecked)) { // if (goalZoneFieldsFree.contains(fieldToBeChecked)) {}
//				goalZoneFieldCandidates.add(goalZoneField);
//			}
//		}
		return roleZoneFieldCandidates;
//		return mapManager.getOnlyRoleZoneCoords();
	}

	/**
	 * Determines dispensers of the required type
	 * 
	 * @param dispenserType The required dispenser type
	 * 
	 * @return The absolute coordinates of the identified dispensers
	 */
	public Set<Dispenser> determineDispenserCandidates(String dispenserType) {
		Set<Dispenser> dispenserCandidates = new HashSet<>();
		HashMap<RelativeCoordinate, Dispenser> dispenserLayer = mapManager.getDispenserLayer();
		for (Map.Entry<RelativeCoordinate, Dispenser> entry : dispenserLayer.entrySet()) {
			if (entry.getValue() != null) {
				if (entry.getValue().getType().equals(dispenserType)) {
					dispenserCandidates.add(entry.getValue());
				}
			}
		}
		return dispenserCandidates;
	}

	/**
	 * Determines the number of steps to the closest dispenser of the required type
	 * taking into account obstacles/entities/blocks on the way
	 * 
	 * @param dispenserType The required dispenser type
	 * 
	 * @return The number of steps
	 */
	public int calcStepsToNextDispenser(String dispenserType) {
		Set<Dispenser> dispensers = determineDispenserCandidates(dispenserType);
		Set<RelativeCoordinate> destinations = new HashSet<>();
		for (Dispenser dispenser : dispensers) {
			destinations.add(dispenser.getRelativeCoordinate());
		}
		if (destinations.size() == 0) {
			return -1; // Error - no dispenser of required type found
		}
		List<RelativeCoordinate> attachedBlocksRelative = getRelativeCoordinates(attachedBlocks);
		RelativeCoordinate currentPos = mapManager.getPosition();
		HashMap<String, RelativeCoordinate> mapDimensions = mapManager.analyzeMapDimensions();

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
		queue.add(new Node(xA, yA, 0));
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
					for (RelativeCoordinate destination : destinations) {
						int xG = destination.getX();
						int yG = destination.getY();
						// Destination reached?
						if (newX == xG && newY == yG) {
							return node.stepNum + 1;
						}
					}
					// Mark field as 'discovered' and add it to the queue
					discovered.add(new RelativeCoordinate(newX, newY));
					queue.add(new Node(newX, newY, node.stepNum + 1));
				}
			}
		}
		return -2; // Error - no path found
	}

		/**
	 * Determines the closest dispenser of the specified type taking into account
	 * obstacles/entities/blocks on the way
	 * 
	 * @param dispenserType The type of the dispenser
	 * 
	 * @return The dispenser or null in case no dispenser was identified
	 */
	public Dispenser getClosestDispenser(String dispenserType) {
		Set<Dispenser> dispensers = determineDispenserCandidates(dispenserType);
		if (dispensers == null || dispensers.size() == 0) {
			return null; // Error - no dispenser of required type found
		}
		List<RelativeCoordinate> attachedBlocksRelative = getRelativeCoordinates(attachedBlocks);
		RelativeCoordinate currentPos = mapManager.getPosition();
		HashMap<String, RelativeCoordinate> mapDimensions = mapManager.analyzeMapDimensions();

		for (Dispenser dispenser : dispensers) {
			if (dispenser.getRelativeCoordinate().equals(currentPos)) {
				return dispenser;
			}
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
					List<Direction> newDirList;
					if (node.initialDirs == null) {
						newDirList = new ArrayList<>();
						newDirList.add(dir);
					} else {
						newDirList = new ArrayList<Direction>(node.initialDirs);
					}

					for (Dispenser dispenser : dispensers) {
						int xG = dispenser.getRelativeCoordinate().getX();
						int yG = dispenser.getRelativeCoordinate().getY();
						// Destination reached?
						if (newX == xG && newY == yG) {
							return dispenser;
						}
					}
					// Mark field as 'discovered' and add it to the queue
					discovered.add(new RelativeCoordinate(newX, newY));
					queue.add(new Node(newX, newY, newDirList));
				}
			}
		}
		return null;
	}

	/**
	 * Determines the direction to get to the closest obstacle
	 * 
	 * @return The direction the agent should walk towards
	 */
	public Action calculateShortestPathNextObstacle() {
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
