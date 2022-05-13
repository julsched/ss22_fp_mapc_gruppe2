package massim.javaagents.agents.g2utils;

public abstract class Cell {
private int lastSeen;
	
	public Cell(int lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	public int getLastSeen() {
		return lastSeen;
	}
	
	//Used to print out content of Cell
	public String toString() {
		return this.getClass().getSimpleName()+ "\r\n";
	}
}
