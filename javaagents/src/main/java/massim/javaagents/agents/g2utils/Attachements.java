package massim.javaagents.agents.g2utils;

import java.util.ArrayList;
import java.util.List;

public class Attachements {
	
	private List<RelativeCoordinate> attachedThings = new ArrayList<>();
	private Connection north;
	private Connection south;
	private Connection east;
	private Connection west;
	
	private List<RelativeCoordinate> northCoordinates = new ArrayList<RelativeCoordinate>();
	private List<RelativeCoordinate> eastCoordinates = new ArrayList<RelativeCoordinate>();
	private List<RelativeCoordinate> southCoordinates = new ArrayList<RelativeCoordinate>();
	private List<RelativeCoordinate> westCoordinates = new ArrayList<RelativeCoordinate>();
	
	public void addThing(String direction, RelativeCoordinate relCo) {
		attachedThings.add(relCo);
		switch (direction) {
		case ("n"):
			north = new Connection(relCo);
			northCoordinates.add(new RelativeCoordinate(0, -1));
			break;
		case ("e"):
			east = new Connection(relCo);
			eastCoordinates.add(new RelativeCoordinate(1, 0));
			break;
		case ("s"):
			south = new Connection(relCo);
			southCoordinates.add(new RelativeCoordinate(0, 1));
			break;
		case ("w"):
			west = new Connection(relCo);
			westCoordinates.add(new RelativeCoordinate(1, 0));
			break;
		default:
			break;
		}
	}
	
	public Connection getNorth() {
		return north;
	}
	
	public Connection getEast() {
		return east;
	}
	
	public Connection getSouth() {
		return south;
	}
	
	public Connection getWest() {
		return west;
	}
	
	public ArrayList<RelativeCoordinate> removeConnections(String direction) {
		ArrayList<RelativeCoordinate> toRemove = new ArrayList<RelativeCoordinate>();
		switch (direction) {
		case "n":
			toRemove = north.removeConnection();
			north = null;
			break;
		case "s":
			toRemove = south.removeConnection();
			south = null;
			break;
		case "e":
			toRemove = east.removeConnection();
			east = null;
			break;
		case "w":
			toRemove = west.removeConnection();
			west = null;
			break;
		default:
			break;
		}
		for (RelativeCoordinate relCo : toRemove) {
			attachedThings.remove(relCo);
		}
		return toRemove;
	}
	
	public void rotate(String clockwise) {
		Connection temp = north;
		if (clockwise.equals("cw")) {
			north = east;
			north.rotate(true);
			east = south;
			south = west;
			west = temp;
		} else {
			north = west;
			west = south;
			south = east;
			east = temp;
		}
	}
	
	public String findBranch(RelativeCoordinate relCo) {
		String result = "";
		if (northCoordinates.contains(relCo)) {
			return "n";
		}
		if (southCoordinates.contains(relCo)) {
			return "s";
		}
		if (eastCoordinates.contains(relCo)) {
			return "e";
		}
		return result;
	}

}
