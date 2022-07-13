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
			north = new Connection(relCo, null);
			northCoordinates.add(new RelativeCoordinate(0, -1));
			break;
		case ("e"):
			east = new Connection(relCo, null);
			eastCoordinates.add(new RelativeCoordinate(1, 0));
			break;
		case ("s"):
			south = new Connection(relCo,null);
			southCoordinates.add(new RelativeCoordinate(0, 1));
			break;
		case ("w"):
			west = new Connection(relCo, null);
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
	
	public void removeConnections(Connection start) {
		RelativeCoordinate toRemove = start.getPosition();
		attachedThings.remove(toRemove);
		String branch = findBranch(toRemove);
		switch (branch) {
		case "n":
			northCoordinates.remove(toRemove);
			break;
		case "e":
			eastCoordinates.remove(toRemove);
			break;
		case "s":
			southCoordinates.remove(toRemove);
			break;
		case "w":
			westCoordinates.remove(toRemove);
			break;
		default:
			break;
		}
		if (!(start.getConn1() == null)) {
			removeConnections(start.getConn1());
		}
		if (!(start.getConn2() == null)) {
			removeConnections(start.getConn2());
		}
		if (!(start.getConn3() == null)) {
			removeConnections(start.getConn3());
		}
	}
	
	public void removeConnections(RelativeCoordinate relCo) {
		String branch = findBranch(relCo);
		Connection toRemove = findConnection(relCo);
		Connection lastConnection = toRemove.getSource();
		if (!(lastConnection == null)) {
			if (lastConnection.getConn1().equals(lastConnection)) {
				lastConnection.setConn1(null);
			} else if (lastConnection.getConn2().equals(lastConnection)) {
				lastConnection.setConn2(null);
			} else {
				lastConnection.setConn3(null);
			}
		}
		if (!(toRemove.getConn1() == null)) {
			removeConnections(toRemove.getConn1());
		}
		if (!(toRemove.getConn2() == null)) {
			removeConnections(toRemove.getConn2());
		}
		if (!(toRemove.getConn3() == null)) {
			removeConnections(toRemove.getConn3());
		}
		attachedThings.remove(relCo);
		switch (branch) {
		case "n":
			northCoordinates.remove(relCo);
			break;
		case "e":
			eastCoordinates.remove(relCo);
			break;
		case "s":
			southCoordinates.remove(relCo);
			break;
		case "w":
			westCoordinates.remove(relCo);
			break;
		default:
			break;
		}		
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
		if (westCoordinates.contains(relCo)) {
			return "w";
		}
		return result;
	}
	
	public Connection findConnection(RelativeCoordinate relCo) {
		Connection result = null;
		result = north.findConnection(relCo);
		if (!(result ==  null)) {
			return result;
		}
		result = south.findConnection(relCo);
		if (!(result ==  null)) {
			return result;
		}
		result = east.findConnection(relCo);
		if (!(result ==  null)) {
			return result;
		}
		result = west.findConnection(relCo);
		return result;	
	}
	
	public Connection addReverseBranch(Connection myConnection, Connection myPartnersConnection, int xDiff, int yDiff, String direction) {
		RelativeCoordinate relCo = new RelativeCoordinate(myPartnersConnection.getPosition().getX() + xDiff, myPartnersConnection.getPosition().getY() + yDiff);
		attachedThings.add(relCo);
		switch (direction) {
		case "n":
			northCoordinates.add(relCo);
			break;
		case "e":
			eastCoordinates.add(relCo);
			break;
		case "s":
			southCoordinates.add(relCo);
			break;
		case "w":
			westCoordinates.add(relCo);
			break;
		default:
			break;
		}
		Connection temp = new Connection(relCo, myConnection);
		if (!(myPartnersConnection.getConn1() == null)) {
			addBranch(temp, myPartnersConnection.getConn1(), xDiff, yDiff, direction);
		}
		if (!(myPartnersConnection.getConn2() == null)) {
			addBranch(temp, myPartnersConnection.getConn2(), xDiff, yDiff, direction);
		}
		if (!(myPartnersConnection.getConn3() == null)) {
			addBranch(temp, myPartnersConnection.getConn3(), xDiff, yDiff, direction);
		}
		if (myConnection.getConn1() == null) {
			myConnection.setConn1(temp);
		} else if (myConnection.getConn2() == null) {
			myConnection.setConn2(temp);
		} else {
			myConnection.setConn3(temp);
		}
		if (!(myPartnersConnection.getSource() == null)) {
			return addReverseBranch(temp, myPartnersConnection.getSource(), xDiff, yDiff, direction);
		} else {
			Connection agent = new Connection(new RelativeCoordinate(xDiff, yDiff), temp);
			if (temp.getConn1() == null) {
				temp.setConn1(agent);
			} else if (temp.getConn2() == null) {
				temp.setConn2(agent);
			} else {
				temp.setConn3(agent);
			}
			return agent;
		}
	}
	
	public void addBranch(Connection source, Connection branch, int xDiff, int yDiff, String direction) {
		RelativeCoordinate relCo = new RelativeCoordinate(branch.getPosition().getX() + xDiff, branch.getPosition().getY() + yDiff);
		Connection temp = new Connection(relCo, source);
		attachedThings.add(relCo);
		switch (direction) {
		case "n":
			northCoordinates.add(relCo);
			break;
		case "e":
			eastCoordinates.add(relCo);
			break;
		case "s":
			southCoordinates.add(relCo);
			break;
		case "w":
			westCoordinates.add(relCo);
			break;
		default:
			break;
		}
		if (!(branch.getConn1() == null)) {
			addBranch(temp, branch.getConn1(), xDiff, yDiff, direction);
		}
		if (!(branch.getConn2() == null)) {
			addBranch(temp, branch.getConn2(), xDiff, yDiff, direction);
		}
		if (!(branch.getConn3() == null)) {
			addBranch(temp, branch.getConn3(), xDiff, yDiff, direction);
		}
		if (source.getConn1() == null) {
			source.setConn1(temp);
		} else if (source.getConn2() == null) {
			source.setConn2(temp);
		} else {
			source.setConn3(temp);
		}
	}

}
