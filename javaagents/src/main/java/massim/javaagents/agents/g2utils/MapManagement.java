package massim.javaagents.agents.g2utils;

import java.util.*;

import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Cell> currentMap;
	private RelativeCoordinate currentPosition;
	private int currentStep;
	private List<Entity> entities = new ArrayList<>();
	private Orientation rotated = Orientation.NORTH;
	private List<Percept> percepts;
	
	private AgentInformation exchangePartner;
	
	private HashMap<RelativeCoordinate, Cell> lastMap;
	private RelativeCoordinate lastPosition;
	private List<Entity> lastEntities = new ArrayList<>();
	
	public MapManagement(int currentStep, RelativeCoordinate currentPosition) {
		this.currentMap = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = currentPosition;
		this.currentStep = currentStep;
		this.lastMap = new HashMap<RelativeCoordinate, Cell>();
		this.lastPosition = new RelativeCoordinate(0, 0);
	}
	
	public void updatePosition(int x, int y, String direction, int counter) {
		if ((direction.equals("n") && rotated.equals(Orientation.NORTH))
				|| (direction.equals("w") && rotated.equals(Orientation.EAST))
				|| (direction.equals("s") && rotated.equals(Orientation.SOUTH))
				|| (direction.equals("e") && rotated.equals(Orientation.WEST))) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() + 1);
		} else if ((direction.equals("s") && rotated.equals(Orientation.NORTH))
				|| (direction.equals("e") && rotated.equals(Orientation.EAST))
				|| (direction.equals("n") && rotated.equals(Orientation.SOUTH))
				|| (direction.equals("e") && rotated.equals(Orientation.WEST))) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() - 1);
		} else if ((direction.equals("e") && rotated.equals(Orientation.NORTH))
				|| (direction.equals("n") && rotated.equals(Orientation.EAST))
				|| (direction.equals("w") && rotated.equals(Orientation.SOUTH))
				|| (direction.equals("s") && rotated.equals(Orientation.WEST))) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() + 1, currentPosition.getY());
		} else if ((direction.equals("w") && rotated.equals(Orientation.NORTH))
				|| (direction.equals("s") && rotated.equals(Orientation.EAST))
				|| (direction.equals("e") && rotated.equals(Orientation.SOUTH))
				|| (direction.equals("n") && rotated.equals(Orientation.WEST))) {
			if (counter == 1) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() - 1, currentPosition.getY() - 1);
		}
		
	}
	
	public void updateOrientation(boolean clockwise) {
		this.rotated = Orientation.changeOrientation(rotated, clockwise);
	}
	
	public void updateMap(List<Percept> percepts) {
		
		this.percepts = percepts;
		
		// alte Map kopieren
		HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
		for (RelativeCoordinate key : currentMap.keySet()) {
			temp.put(key, currentMap.get(key));
		}
		this.lastMap = temp;
		
		// Map aktualisieren
		if (percepts == null) { // Error handling if no percepts are available
			return;
		} else {
			Iterator<Percept> it = percepts.iterator();
			Percept percept;
			while (it.hasNext()) {
				percept = it.next();
				if (percept.getName().equals("things")) {
					String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					if (thingType.equals("dispenser")) {
						String type = ((Identifier) percept.getParameters().get(3)).getValue();
						Dispenser dispenser = new Dispenser(absolutePosition, type, currentStep);
						currentMap.put(absolutePosition, dispenser);
					}
					if (thingType.equals("block")) {
						String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
						Block block = new Block(absolutePosition, blockType, currentStep);
						currentMap.put(absolutePosition, block);
					}
					if (thingType.equals("obstacle")) {
						Obstacle obstacle = new Obstacle(absolutePosition, currentStep);
						currentMap.put(absolutePosition, obstacle);
					}
				}
				if (percept.getName().equals("goalzone")) {		
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					currentMap.put(absolutePosition, new Goalzone(absolutePosition, currentStep));	
				}
				if (percept.getName().equals("rolezone")) {		
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPosition.getX() + x, this.currentPosition.getY() + y);
					currentMap.put(absolutePosition, new Rolezone(absolutePosition, currentStep));	
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
			updateMap(percepts);
		}
	}

}
