package massim.javaagents.agents.g2utils;

import java.util.*;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private HashMap<RelativeCoordinate, ClearMarker> clearLayer;
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
		this.clearLayer = new HashMap<RelativeCoordinate, ClearMarker>();
		this.knownArea = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastPosition = new RelativeCoordinate(0, 0);
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
						case ("ClearMarker"):
							ClearMarker cm = (ClearMarker) cell;
							clearLayer.put(absolutePos, cm);
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
					clearLayer.put(absolutePos, null);
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
						case ("ClearMarker"):
							ClearMarker cm = (ClearMarker) cell;
							clearLayer.put(absolutePos, cm);
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
					clearLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
				}
			}
		}
		
	}
	
	public void setTeamMembers(ArrayList<RelativeCoordinate> seenTeamMembers) {
		lastTeamMembers = teamMembers;
		teamMembers = seenTeamMembers;
	}
	
	public ArrayList<RelativeCoordinate> getTeamMembers() {
		return teamMembers;
	}
	
	public ArrayList<RelativeCoordinate> getLastTeamMembers() {
		return lastTeamMembers;
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
	
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	public String mergeMaps(MapBundle mapBundle, RelativeCoordinate exchangePartner) {
		
		/*
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
			return false;
		}
		if (agents.size() < 1 || agents.size() > 1) {
			return false;
		}
		*/
		
		// Mergen der verschiedenen KartenLayer
		RelativeCoordinate pos = mapBundle.getPosition();
		int xDiff = pos.getX() - exchangePartner.getX();
		int yDiff = pos.getY() - exchangePartner.getY();
		for (RelativeCoordinate key : mapBundle.getBlockLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!blockLayer.containsKey(newKey) || blockLayer.get(newKey).getLastSeen() < mapBundle.getBlockLayer().get(key).getLastSeen()) {
				blockLayer.put(newKey, mapBundle.getBlockLayer().get(key));
			}
			knownArea.put(newKey, null);
		}
		for (RelativeCoordinate key : mapBundle.getDispenserLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!dispenserLayer.containsKey(newKey) || dispenserLayer.get(newKey).getLastSeen() < mapBundle.getDispenserLayer().get(key).getLastSeen()) {
				dispenserLayer.put(newKey, mapBundle.getDispenserLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getGoalzoneLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!goalzoneLayer.containsKey(newKey) || goalzoneLayer.get(newKey).getLastSeen() < mapBundle.getGoalzoneLayer().get(key).getLastSeen()) {
				goalzoneLayer.put(newKey, mapBundle.getGoalzoneLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getRolezoneLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!rolezoneLayer.containsKey(newKey) || rolezoneLayer.get(newKey).getLastSeen() < mapBundle.getRolezoneLayer().get(key).getLastSeen()) {
				rolezoneLayer.put(newKey, mapBundle.getRolezoneLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getObstacleLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
			if (!obstacleLayer.containsKey(newKey) || obstacleLayer.get(newKey).getLastSeen() < mapBundle.getObstacleLayer().get(key).getLastSeen()) {
				obstacleLayer.put(newKey, mapBundle.getObstacleLayer().get(key));
			}			
		}
		
		return mapBundle.getOwner();
	}
	
	public void removeObstacle(RelativeCoordinate obstaclePosition) {
		obstacleLayer.put(obstaclePosition, null);
	}
	
	public void updateLastPosition(int x, int y) {
		lastPosition = new RelativeCoordinate(lastPosition.getX() + x, lastPosition.getY() + y);
	}
	
}
