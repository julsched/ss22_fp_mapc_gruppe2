package massim.javaagents.agents.g2utils;

import java.util.ArrayList;

public class Connection {
	
	private RelativeCoordinate position;
	private Connection source;
	private Connection conn1 = null;
	private Connection conn2 = null;
	private Connection conn3 = null;
	private ArrayList<RelativeCoordinate> toRemove = new ArrayList<RelativeCoordinate>();
	
	public  Connection(RelativeCoordinate relCo, Connection source) {
		position = relCo;
		this.source = source;
	}
	
	public ArrayList<RelativeCoordinate> getToRemove() {
		return toRemove;
	}
	
	public ArrayList<RelativeCoordinate> removeConnection() {
		toRemove.add(position);
		if (!(conn1 == null)) {
			toRemove.addAll(conn1.getToRemove());
			conn1.removeConnection();
		}
		conn1 = null;
		if (!(conn2 == null)) {
			toRemove.addAll(conn2.getToRemove());
			conn2.removeConnection();
		}
		conn2 = null;
		if (!(conn3 == null)) {
			toRemove.addAll(conn3.getToRemove());
			conn3.removeConnection();
		}
		conn3 = null;
		return toRemove;
	}
	
	public void rotate(boolean clockwise) {
		if (!(conn1 == null)) {
			conn1.rotate(clockwise);
		}
		if (!(conn2 == null)) {
			conn2.rotate(clockwise);
		}
		if (!(conn3 == null)) {
			conn3.rotate(clockwise);
		}
		if (clockwise) {		
			position = new RelativeCoordinate(-position.getY(), position.getX());
		} else {
			position = new RelativeCoordinate(position.getY(), -position.getX());
		}
	}
	
	public Connection findConnection(RelativeCoordinate relCo) {
		
		if (relCo.equals(position)) {
			return this;
		}
		Connection result = null;
		if (!(conn1 == null)) {
			result = conn1.findConnection(relCo);
		}
		if (!(result == null) ) {
			return result;
		}
		if (!(conn2 == null)) {
			result = conn2.findConnection(relCo);
		}
		if (!(result == null)) {
			return result;
		}
		if (!(conn3 == null)) {
			result = conn3.findConnection(relCo);
		}
		return result;
		
	}

}
