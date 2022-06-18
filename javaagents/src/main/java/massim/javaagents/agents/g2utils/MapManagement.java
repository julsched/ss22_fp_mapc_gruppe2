package massim.javaagents.agents.g2utils;

import java.util.*;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private HashMap<RelativeCoordinate, Entity> entityLayer;
	private HashMap<RelativeCoordinate, Cell> knownArea;
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
		this.entityLayer = new HashMap<RelativeCoordinate, Entity>();
		this.knownArea = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastPosition = new RelativeCoordinate(0, 0);
	}

	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}
	
	public void updatePosition(int x, int y, String direction, int counter) {
		if (direction.equals("n")) {
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
			currentPosition = new RelativeCoordinate(currentPosition.getX() - 1, currentPosition.getY());
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
						String type = cell.getClass().getSimpleName();
						switch (type) {
						case ("Dispenser"):
							Dispenser disp = (Dispenser) cell;
							disp.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Rolezone"):
							Rolezone rz = (Rolezone) cell;
							rz.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Obstacle"):
							Obstacle obs = (Obstacle) cell;
							obs.setRelativeCoordinate(absolutePos);
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
							entityLayer.put(absolutePos, null);
							dispenserLayer.put(absolutePos, null);
							break;
						case ("Block"):
							Block block = (Block) cell;
							block.setRelativeCoordinate(absolutePos);
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
							entityLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
							break;
						case ("Goalzone"):
							Goalzone gz = (Goalzone) cell;
							gz.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Entity"):
							Entity entity = (Entity) cell;
							entity.setRelativeCoordinate(absolutePos);
							entityLayer.put(absolutePos, entity);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							blockLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
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
					entityLayer.put(absolutePos, null);
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
						String type = cell.getClass().getSimpleName();
						switch (type) {
						case ("Dispenser"):
							Dispenser disp = (Dispenser) cell;
							disp.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Rolezone"):
							Rolezone rz = (Rolezone) cell;
							rz.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Obstacle"):
							Obstacle obs = (Obstacle) cell;
							obs.setRelativeCoordinate(absolutePos);
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
							entityLayer.put(absolutePos, null);
							dispenserLayer.put(absolutePos, null);
							break;
						case ("Block"):
							Block block = (Block) cell;
							block.setRelativeCoordinate(absolutePos);
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
							entityLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
							break;
						case ("Goalzone"):
							Goalzone gz = (Goalzone) cell;
							gz.setRelativeCoordinate(absolutePos);
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
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Entity"):
							Entity entity = (Entity) cell;
							entity.setRelativeCoordinate(absolutePos);
							entityLayer.put(absolutePos, entity);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							blockLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
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
					entityLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
				}
			}
		}

		// For testing
		/*System.out.println("Current Position:");
		System.out.println(currentPosition);
		System.out.println("Goal Zone Layer:");
		System.out.println(goalzoneLayer);
		System.out.println("Role Zone Layer:");
		System.out.println(rolezoneLayer);
		System.out.println("Dispenser Layer:");
		System.out.println(dispenserLayer);
		System.out.println("Block Layer:");
		System.out.println(blockLayer);
		System.out.println("Obstacle Layer:");
		System.out.println(obstacleLayer);
		System.out.println("Entity Layer:");
		System.out.println(entityLayer);
		System.out.println("Known Area:");
		System.out.println(knownArea);*/
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

	public HashMap<RelativeCoordinate, Entity> getEntityLayer() {
		return entityLayer;
	}

	public RelativeCoordinate getCurrentPosition() {
		return currentPosition;
	}
	
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	public void mergeMaps(HashMap<RelativeCoordinate, Block> sentBlocks, HashMap<RelativeCoordinate, Dispenser> sentDispensers, HashMap<RelativeCoordinate, Goalzone> sentGoalzones, HashMap<RelativeCoordinate, Rolezone> sentRolezones, HashMap<RelativeCoordinate, Obstacle> sentObstacles, RelativeCoordinate sentPosition, int stepOfSentMap) {
		
		RelativeCoordinate rc = exchangePartner.getRelativeCoordinate();
		ArrayList<RelativeCoordinate> agents = new ArrayList<RelativeCoordinate>();
		
		// Bestimmung der Position des sendenden Agenten
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
	}

	public HashMap<String, RelativeCoordinate> analyzeMapDimensions() {
		// Most northern cell that has been analyzed
		RelativeCoordinate north = new RelativeCoordinate(0, 0);
		// Most eastern cell that has been analyzed
		RelativeCoordinate east = new RelativeCoordinate(0, 0);
		// Most southern cell that has been analyzed
		RelativeCoordinate south = new RelativeCoordinate(0, 0);
		// Most western cell that has been analyzed
		RelativeCoordinate west = new RelativeCoordinate(0, 0);
		for (RelativeCoordinate relativeCoordinate : knownArea.keySet()) {
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
