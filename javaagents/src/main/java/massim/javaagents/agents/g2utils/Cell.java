package massim.javaagents.agents.g2utils;

public abstract class Cell {
private int lastSeen;
	
	public Cell(int lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	public int getLastSeen() {
		return lastSeen;
	}
}
