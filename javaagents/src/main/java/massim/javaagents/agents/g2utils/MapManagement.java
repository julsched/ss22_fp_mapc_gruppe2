package massim.javaagents.agents.g2utils;

import java.util.*;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private HashMap<RelativeCoordinate, Cell> knownArea;
	//private HashMap<RelativeCoordinate, List<Cell>> currentMap; //JULIAs
	private RelativeCoordinate currentPosition;
	private int currentStep;
	private ArrayList<RelativeCoordinate> teamMembers = new ArrayList<>();
	
	private AgentInformation exchangePartner;
	
	private RelativeCoordinate lastPosition;
	private ArrayList<RelativeCoordinate> lastTeamMembers = new ArrayList<>();
	
	public MapManagement(int currentStep, RelativeCoordinate currentPosition) {
		
		this.blockLayer = new HashMap<RelativeCoordinate, Block>();
		this.dispenserLayer = new HashMap<RelativeCoordinate, Dispenser>();
		this.goalzoneLayer = new HashMap<RelativeCoordinate, Goalzone>();
		this.obstacleLayer = new HashMap<RelativeCoordinate, Obstacle>();
		this.rolezoneLayer = new HashMap<RelativeCoordinate, Rolezone>();
		this.knownArea = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
	/*private HashMap<RelativeCoordinate, List<Cell>> lastMap;
	private RelativeCoordinate lastPosition;
	private List<Entity> lastEntities = new ArrayList<>();

	public MapManagement(int currentStep, RelativeCoordinate currentPosition) {
		this.currentMap = new HashMap<RelativeCoordinate, List<Cell>>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastMap = new HashMap<RelativeCoordinate, List<Cell>>();*/
		this.lastPosition = new RelativeCoordinate(0, 0);
	}
	
	public RelativeCoordinate getCurrentPos() {
		return this.currentPosition;
	}

	public void updatePosition(int x, int y, String direction, int counter) {
		if (direction.equals("n")) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() + 1);
		} else if (direction.equals("s")) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() - 1);
		} else if (direction.equals("s")) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() + 1);
		} else if (direction.equals("e")) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() + 1, currentPosition.getY());
		} else if (direction.equals("w")) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() - 1, currentPosition.getY() - 1);
		}		
	}
	
	public void updateMap(HashMap<RelativeCoordinate, List<Cell>> tempMap, int vision) {
		
		int currentX = currentPosition.getX();
		int currentY = currentPosition.getY();
		
		for (int x = -vision; x < (vision + 1); x++) {
			for (int y = 0; (y + Math.abs(x)) < (vision + 1); y++) {
				RelativeCoordinate tempPos = new RelativeCoordinate(x, y);
				RelativeCoordinate absolutePos = new RelativeCoordinate(x + currentX, y + currentY);
				if (tempMap.containsKey(tempPos)) {
					Iterator<Cell> it = tempMap.get(tempPos).iterator();
					while (it.hasNext()) {
						Cell cell = it.next();
						String type = cell.getClass().toString();
						switch (type) {
						case ("Dispenser"):
							Dispenser disp = (Dispenser) cell;
							dispenserLayer.put(absolutePos, disp);
							obstacleLayer.put(absolutePos, null);
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							break;
						case ("Rolezone"):
							Rolezone rz = (Rolezone) cell;
							rolezoneLayer.put(absolutePos, rz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							break;
						case ("Obstacle"):
							Obstacle obs = (Obstacle) cell;
							obstacleLayer.put(absolutePos, obs);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							dispenserLayer.put(absolutePos, null);
							break;
						case ("Block"):
							Block block = (Block) cell;
							blockLayer.put(absolutePos, block);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							obstacleLayer.put(absolutePos, null);
							break;
						case ("Goalzone"):
							Goalzone gz = (Goalzone) cell;
							goalzoneLayer.put(absolutePos, gz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							break;
						default:
							break;
						}
						knownArea.put(absolutePos, null);
					}
				} else {
					goalzoneLayer.put(absolutePos, null);
					rolezoneLayer.put(absolutePos, null);
					dispenserLayer.put(absolutePos, null);
					blockLayer.put(absolutePos, null);
					obstacleLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
				}
			}
			for (int y = 0; (Math.abs(y) + Math.abs(x)) < (vision + 1); y--) {
				RelativeCoordinate tempPos = new RelativeCoordinate(x, y);
				RelativeCoordinate absolutePos = new RelativeCoordinate(x + currentX, y + currentY);
				if (tempMap.containsKey(tempPos)) {
					Iterator<Cell> it = tempMap.get(tempPos).iterator();
					while (it.hasNext()) {
						Cell cell = it.next();
						String type = cell.getClass().toString();
						switch (type) {
						case ("Dispenser"):
							Dispenser disp = (Dispenser) cell;
							dispenserLayer.put(absolutePos, disp);
							obstacleLayer.put(absolutePos, null);
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							break;
						case ("Rolezone"):
							Rolezone rz = (Rolezone) cell;
							rolezoneLayer.put(absolutePos, rz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							break;
						case ("Obstacle"):
							Obstacle obs = (Obstacle) cell;
							obstacleLayer.put(absolutePos, obs);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							dispenserLayer.put(absolutePos, null);
							break;
						case ("Block"):
							Block block = (Block) cell;
							blockLayer.put(absolutePos, block);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							obstacleLayer.put(absolutePos, null);
							break;
						case ("Goalzone"):
							Goalzone gz = (Goalzone) cell;
							goalzoneLayer.put(absolutePos, gz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							break;
						default:
							break;
						}
						knownArea.put(absolutePos, null);
					}
				} else {
					goalzoneLayer.put(absolutePos, null);
					rolezoneLayer.put(absolutePos, null);
					dispenserLayer.put(absolutePos, null);
					blockLayer.put(absolutePos, null);
					obstacleLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
/* 
			currentPosition = new RelativeCoordinate(currentPosition.getX() - 1, currentPosition.getY());
		}
	}

	public void updateOrientation(boolean clockwise) {
		this.rotated = Orientation.changeOrientation(rotated, clockwise);
	}
	
	public void updateMap(List<Percept> percepts, int currentStep, int vision, List<RelativeCoordinate> attachedBlocks) {
		
		this.percepts = percepts;
		this.currentStep = currentStep;
		
		// alte Map kopieren
		HashMap<RelativeCoordinate, List<Cell>> temp = new HashMap<RelativeCoordinate, List<Cell>>();
		for (RelativeCoordinate key : currentMap.keySet()) {
			temp.put(key, currentMap.get(key));
		}
		this.lastMap = temp;
		
		// Map aktualisieren
		if (percepts == null) { // Error handling if no percepts are available
			return;
		} else {
			List<RelativeCoordinate> visionCells = getVisionCells(vision);
			Iterator<Percept> it = percepts.iterator();
			Percept percept;
			while (it.hasNext()) {
				percept = it.next();
				if (percept.getName().equals("thing")) {
					String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					List<Cell> cells = currentMap.get(absolutePosition);
					if (thingType.equals("dispenser")) {
						String type = ((Identifier) percept.getParameters().get(3)).getValue();
						Dispenser dispenser = new Dispenser(absolutePosition, type, currentStep);
						if (cells == null || cells.get(0).getLastSeen() < currentStep) {
							cells = new ArrayList<>();
						}
						cells.add(dispenser);
						currentMap.put(absolutePosition, cells);
						visionCells.remove(new RelativeCoordinate(x, y));
					}
					if (thingType.equals("block")) {
						visionCells.remove(new RelativeCoordinate(x, y));
						if (!attachedBlocks.contains(new RelativeCoordinate(x, y))) {
							String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
							Block block = new Block(absolutePosition, blockType, currentStep);
							if (cells == null || cells.get(0).getLastSeen() < currentStep) {
								cells = new ArrayList<>();
							}
							cells.add(block);
							currentMap.put(absolutePosition, cells);
						} else {
							if (cells == null || (cells != null && cells.get(0).getLastSeen() < currentStep)) {
								//currentMap.remove(absolutePosition);
								currentMap.put(absolutePosition, null);
							}
						}
					}
					if (thingType.equals("obstacle")) {
						Obstacle obstacle = new Obstacle(absolutePosition, currentStep);
						if (cells == null || cells.get(0).getLastSeen() < currentStep) {
							cells = new ArrayList<>();
						}
						cells.add(obstacle);
						currentMap.put(absolutePosition, cells);
						visionCells.remove(new RelativeCoordinate(x, y));
					}
					// TODO: do with entity
					if (thingType.equals("entity")) {
						// Agent itself should not be saved in Map since we know its current Position already
						if (x == 0 && y == 0) {
							continue;
						}
						Obstacle obstacle = new Obstacle(absolutePosition, currentStep);
						if (cells == null || cells.get(0).getLastSeen() < currentStep) {
							cells = new ArrayList<>();
						}
						cells.add(obstacle);
						currentMap.put(absolutePosition, cells);
						visionCells.remove(new RelativeCoordinate(x, y));
					}
				}
				if (percept.getName().equals("goalZone")) {		
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					visionCells.remove(new RelativeCoordinate(x, y));
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					List<Cell> cells = currentMap.get(absolutePosition);
					if (cells == null || cells.get(0).getLastSeen() < currentStep) {
						cells = new ArrayList<>();
					}
					cells.add(new Goalzone(absolutePosition, currentStep));
					currentMap.put(absolutePosition, cells);	
				}
				if (percept.getName().equals("roleZone")) {		
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					visionCells.remove(new RelativeCoordinate(x, y));
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					List<Cell> cells = currentMap.get(absolutePosition);
					if (cells == null || cells.get(0).getLastSeen() < currentStep) {
						cells = new ArrayList<>();
					}
					cells.add(new Rolezone(absolutePosition, currentStep));
					currentMap.put(absolutePosition, cells);	
				}
			}
			for (RelativeCoordinate visionCell : visionCells) {
				List<Cell> cells = currentMap.get(new RelativeCoordinate(this.currentPosition.getX() + visionCell.getX(), this.currentPosition.getY() + visionCell.getY()));
				if (cells == null || (cells != null && cells.get(0).getLastSeen() < currentStep)) {
					currentMap.put(new RelativeCoordinate(this.currentPosition.getX() + visionCell.getX(), this.currentPosition.getY() + visionCell.getY()), null);
					*/
				}
			}
		}
		//System.out.println("Current Position:");
		//System.out.println(currentPosition);
		//System.out.println("Map:");
		//System.out.println(currentMap);
	}
	
	public void setTeamMembers(ArrayList<RelativeCoordinate> seenTeamMembers) {
		lastTeamMembers = teamMembers;
		teamMembers = seenTeamMembers;
	}
	
	public void createExchangePartner(RelativeCoordinate position) {
		exchangePartner = new AgentInformation(position);
	}
	
	public void setExchangePartner(String name, String role, int energy) {
		if (!(exchangePartner == null)) {
			exchangePartner.setName(name);
			exchangePartner.setRole(role);
			exchangePartner.setEnergy(energy);
		}
	}
	
	public AgentInformation getExchangePartner() {
		return exchangePartner;
	}
	
	public HashMap<RelativeCoordinate, Block> getBlockLayer() {
		return blockLayer;
	}
	
	public HashMap<RelativeCoordinate, Dispenser> getDispenserLayer() {
		return dispenserLayer;
	}
	
	public HashMap<RelativeCoordinate, Goalzone> getGoalzoneLayer() {
		return goalzoneLayer;
	}
	
	public HashMap<RelativeCoordinate, Rolezone> getRolezoneLayer() {
		return rolezoneLayer;
	}
	
	public HashMap<RelativeCoordinate, Obstacle> getObstacleLayer() {
		return obstacleLayer;
	}
	
		/* //JULIAs 
	public HashMap<RelativeCoordinate, List<Cell>> getMap() {
		return currentMap;
	}
	
	public HashMap<RelativeCoordinate, List<Cell>> getLastMap() {
		return lastMap;
	}*/
	
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	public void mergeMaps(HashMap<RelativeCoordinate, Block> sentBlocks, HashMap<RelativeCoordinate, Dispenser> sentDispensers, HashMap<RelativeCoordinate, Goalzone> sentGoalzones, HashMap<RelativeCoordinate, Rolezone> sentRolezones, HashMap<RelativeCoordinate, Obstacle> sentObstacles, RelativeCoordinate sentPosition, int stepOfSentMap) {
		
		RelativeCoordinate rc = exchangePartner.getRelativeCoordinate();
		ArrayList<RelativeCoordinate> agents = new ArrayList<RelativeCoordinate>();
		
		// Bestimmung der Position des sendenden Agenten
	/*public void mergeMaps(HashMap<RelativeCoordinate, Cell> sentMap, RelativeCoordinate sentPosition, int stepOfSentMap) {
		if (currentStep == stepOfSentMap) {
			Iterator<RelativeCoordinate> it = teamMembers.iterator();
			while (it.hasNext()) {
				RelativeCoordinate friendPosition = it.next();
				int xCoor = friendPosition.getX() + currentPosition.getX();
				int yCoor = friendPosition.getY() + currentPosition.getY();
				if (Math.abs(rc.getX() - xCoor) < 2 && Math.abs(rc.getY() - yCoor) < 2) {
					agents.add(new RelativeCoordinate(xCoor, yCoor));
				}
			}
		} else if (currentStep == stepOfSentMap + 1) {
			Iterator<RelativeCoordinate> it = lastTeamMembers.iterator();
			while (it.hasNext()) {
				RelativeCoordinate friendPosition = it.next();
				int xCoor = friendPosition.getX() + lastPosition.getX();
				int yCoor = friendPosition.getY() + lastPosition.getY();
				if (Math.abs(rc.getX() - xCoor) < 2 && Math.abs(rc.getY() - yCoor) < 2) {
					agents.add(new RelativeCoordinate(xCoor, yCoor));
				}
			}
		} else {
			return;
		}
		if (agents.size() < 1 || agents.size() > 1) {
			return;
		}
		
		// Mergen der verschiedenen KartenLayer
		RelativeCoordinate pos = agents.get(0);
		int xDiff = pos.getX() - sentPosition.getX();
		int yDiff = pos.getY() - sentPosition.getY();
		for (RelativeCoordinate key : sentBlocks.keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!blockLayer.containsKey(newKey) || blockLayer.get(newKey).getLastSeen() < sentBlocks.get(key).getLastSeen()) {
				blockLayer.put(newKey, sentBlocks.get(key));
			}
			knownArea.put(newKey, null);
		}
		for (RelativeCoordinate key : sentDispensers.keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!dispenserLayer.containsKey(newKey) || dispenserLayer.get(newKey).getLastSeen() < sentDispensers.get(key).getLastSeen()) {
				dispenserLayer.put(newKey, sentDispensers.get(key));
			}			
		}
		for (RelativeCoordinate key : sentGoalzones.keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!goalzoneLayer.containsKey(newKey) || goalzoneLayer.get(newKey).getLastSeen() < sentGoalzones.get(key).getLastSeen()) {
				goalzoneLayer.put(newKey, sentGoalzones.get(key));
			}			
		}
		for (RelativeCoordinate key : sentRolezones.keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!rolezoneLayer.containsKey(newKey) || rolezoneLayer.get(newKey).getLastSeen() < sentRolezones.get(key).getLastSeen()) {
				rolezoneLayer.put(newKey, sentRolezones.get(key));
			}			
		}
		for (RelativeCoordinate key : sentObstacles.keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!obstacleLayer.containsKey(newKey) || obstacleLayer.get(newKey).getLastSeen() < sentObstacles.get(key).getLastSeen()) {
				obstacleLayer.put(newKey, sentObstacles.get(key));
			}			
		}
	}
	
	public void removeObstacle(RelativeCoordinate obstaclePosition) {
		obstacleLayer.put(obstaclePosition, null);
	}
	
	public void updateLastPosition(int x, int y) {
		lastPosition = new RelativeCoordinate(lastPosition.getX() + x, lastPosition.getY() + y);
	}*/

	// /* //JULIAs 
	public List<RelativeCoordinate> getVisionCells(int vision) {
		List<RelativeCoordinate> visionCells = new ArrayList<>();
		for (int x = -vision; x <= vision; x++) {
			for (int y = -vision; y <= vision; y++) {
				if ((Math.abs(x) + Math.abs(y)) <= vision) {
					visionCells.add(new RelativeCoordinate(x, y));
				}
			}
		}
		return visionCells;
	}*/

	public HashMap<String, RelativeCoordinate> analyzeMapDimensions() {
		// Most northern cell that has been analyzed
		RelativeCoordinate north = new RelativeCoordinate(0, 0);
		// Most eastern cell that has been analyzed
		RelativeCoordinate east = new RelativeCoordinate(0, 0);
		// Most southern cell that has been analyzed
		RelativeCoordinate south = new RelativeCoordinate(0, 0);
		// Most western cell that has been analyzed
		RelativeCoordinate west = new RelativeCoordinate(0, 0);
		for (RelativeCoordinate relativeCoordinate : currentMap.keySet()) {
			if (relativeCoordinate.getX() > east.getX()) {
				east = relativeCoordinate;
			}
			if (relativeCoordinate.getX() < west.getX()) {
				west = relativeCoordinate;
			}
			if (relativeCoordinate.getY() > south.getY()) {
				south = relativeCoordinate;
			}
			if (relativeCoordinate.getY() < north.getY()) {
				north = relativeCoordinate;
			}
		}
		HashMap<String, RelativeCoordinate> map = new HashMap<>();
		map.put("north", north);
		map.put("east", east);
		map.put("south", south);
		map.put("west", west);
		return map;
	}
}
