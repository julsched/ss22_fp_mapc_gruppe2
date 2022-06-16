package massim.javaagents.agents.g2utils;

public class ClearMarker extends Cell {
	
	private boolean immediate;
	
	public ClearMarker(RelativeCoordinate relativeCoordinate, int lastSeen, boolean immediate) {
		super(relativeCoordinate, lastSeen);
		this.immediate = immediate;
	}

}
