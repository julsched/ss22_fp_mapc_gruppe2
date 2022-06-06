package massim.javaagents.agents.g2utils;

import java.util.*;

import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, List<Cell>> currentMap;
	private RelativeCoordinate currentPosition;
	private int currentStep;
	private List<Entity> entities = new ArrayList<>();
	private Orientation rotated = Orientation.NORTH;
	private List<Percept> percepts;
	
	private AgentInformation exchangePartner;
	
	private HashMap<RelativeCoordinate, List<Cell>> lastMap;
	private RelativeCoordinate lastPosition;
	private List<Entity> lastEntities = new ArrayList<>();


	
	public MapManagement(int currentStep, RelativeCoordinate currentPosition) {
		this.currentMap = new HashMap<RelativeCoordinate, List<Cell>>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastMap = new HashMap<RelativeCoordinate, List<Cell>>();
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
				}
			}
		}
		//System.out.println("Current Position:");
		//System.out.println(currentPosition);
		//System.out.println("Map:");
		//System.out.println(currentMap);
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
	
	public HashMap<RelativeCoordinate, List<Cell>> getMap() {
		return currentMap;
	}
	
	public HashMap<RelativeCoordinate, List<Cell>> getLastMap() {
		return lastMap;
	}
	
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	/*public void mergeMaps(HashMap<RelativeCoordinate, Cell> sentMap, RelativeCoordinate sentPosition, int stepOfSentMap) {
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
			updateMap(percepts);
		}
	}*/

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
