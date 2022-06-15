package massim.javaagents.agents.g2utils;

public abstract class Cell {

	final RelativeCoordinate relativeCoordinate;
	private int lastSeen;
	
	public Cell (RelativeCoordinate relativeCoordinate, int lastSeen) {
		this.relativeCoordinate = relativeCoordinate;
		this.lastSeen = lastSeen;
	}
	
	public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
    }

	public int getLastSeen() {
		return lastSeen;
	}
	
	//Used to print out content of Cell
	public String toString() {
		return this.getClass().getSimpleName()+ "\r\n";
	}

	public boolean isCloserThan(Cell cell) {
        if (this.relativeCoordinate.isCloserThan(cell.getRelativeCoordinate())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isNextToAgent() {
        return isNextToAgent(new RelativeCoordinate(0, 0));
    }

	public boolean isNextToAgent(RelativeCoordinate agentPos) {
        if (this.relativeCoordinate.isNextToAgent(agentPos)) {
            return true;
        } else {
            return false;
        }
    }

    public int distanceFromAgent() {
        return this.relativeCoordinate.distanceFromAgent();
    }

    public String getDirectDirection() {
        return getDirectDirection(new RelativeCoordinate(0, 0));
    }

    public String getDirectDirection(RelativeCoordinate pos) {
        return this.relativeCoordinate.getDirectDirection(pos);
    }
}
