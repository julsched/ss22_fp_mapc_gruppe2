package massim.javaagents.agents.g2utils;

import java.util.*;

import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Cell> currentMap;
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private HashMap<RelativeCoordinate, Cell> knownArea;
	private RelativeCoordinate currentPosition;
	private int currentStep;
	private List<Entity> entities = new ArrayList<>();
	private List<Percept> percepts;
	
	private AgentInformation exchangePartner;
	
	private HashMap<RelativeCoordinate, Cell> lastMap;
	private RelativeCoordinate lastPosition;
	private List<Entity> lastEntities = new ArrayList<>();
	
	public MapManagement(int currentStep, RelativeCoordinate currentPosition) {
		this.currentMap = new HashMap<RelativeCoordinate, Cell>();
		this.blockLayer = new HashMap<RelativeCoordinate, Block>();
		this.dispenserLayer = new HashMap<RelativeCoordinate, Dispenser>();
		this.goalzoneLayer = new HashMap<RelativeCoordinate, Goalzone>();
		this.obstacleLayer = new HashMap<RelativeCoordinate, Obstacle>();
		this.rolezoneLayer = new HashMap<RelativeCoordinate, Rolezone>();
		this.knownArea = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastMap = new HashMap<RelativeCoordinate, Cell>();
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
		
		// alte Map kopieren
		HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
		for (RelativeCoordinate key : currentMap.keySet()) {
			temp.put(key, currentMap.get(key));
		}
		this.lastMap = temp;
		
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
				}
			}
		}
		
	}
	
	public void setEntities(List<Entity> sentEntities) {
		lastEntities = entities;
		entities = sentEntities;
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
	
	public HashMap<RelativeCoordinate, Cell> getMap() {
		return currentMap;
	}
	
	public HashMap<RelativeCoordinate, Cell> getLastMap() {
		return lastMap;
	}
	
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	public void mergeMaps(HashMap<RelativeCoordinate, Cell> sentMap, RelativeCoordinate sentPosition, int stepOfSentMap) {
		if (currentStep == stepOfSentMap) {
			RelativeCoordinate rc = exchangePartner.getRelativeCoordinate();
			ArrayList<RelativeCoordinate> agents = new ArrayList<RelativeCoordinate>();
			
			Iterator<Entity> it = entities.iterator();
			while (it.hasNext()) {
				Entity ent = it.next();
				int xCoor = ent.getRelativeCoordinate().getX() + currentPosition.getX();
				int yCoor = ent.getRelativeCoordinate().getY() + currentPosition.getY();
				if (Math.abs(rc.getX() - xCoor) < 2 && Math.abs(rc.getY() - yCoor) < 2) {
					agents.add(new RelativeCoordinate(xCoor, yCoor));
				}
			}
			if (agents.size() < 1 || agents.size() > 1) {
				return;
			}
			RelativeCoordinate pos = agents.get(0);
			int xDiff = pos.getX() - sentPosition.getX();
			int yDiff = pos.getY() - sentPosition.getY();
			for (RelativeCoordinate key : sentMap.keySet()) {
				RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
				if (!currentMap.containsKey(newKey) || currentMap.get(newKey).getLastSeen() < sentMap.get(key).getLastSeen()) {
					currentMap.put(newKey, sentMap.get(key));
				}			
			}
		} else if (currentStep == stepOfSentMap + 1) {
			
			HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
			for (RelativeCoordinate key : lastMap.keySet()) {
				temp.put(key, lastMap.get(key));
			}
			RelativeCoordinate rc = exchangePartner.getRelativeCoordinate();
			ArrayList<RelativeCoordinate> agents = new ArrayList<RelativeCoordinate>();
			
			Iterator<Entity> it = lastEntities.iterator();
			while (it.hasNext()) {
				Entity ent = it.next();
				int xCoor = ent.getRelativeCoordinate().getX() + lastPosition.getX();
				int yCoor = ent.getRelativeCoordinate().getY() + lastPosition.getY();
				if (Math.abs(rc.getX() - xCoor) < 2 && Math.abs(rc.getY() - yCoor) < 2) {
					agents.add(new RelativeCoordinate(xCoor, yCoor));
				}
			}
			if (agents.size() < 1 || agents.size() > 1) {
				return;
			}
			RelativeCoordinate pos = agents.get(0);
			int xDiff = pos.getX() - sentPosition.getX();
			int yDiff = pos.getY() - sentPosition.getY();
			for (RelativeCoordinate key : sentMap.keySet()) {
				RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
				if (!temp.containsKey(newKey) || temp.get(newKey).getLastSeen() < sentMap.get(key).getLastSeen()) {
					temp.put(newKey, sentMap.get(key));
				}			
			}
			currentMap = temp;
	//		updateMap(percepts);
		}
	}

}
