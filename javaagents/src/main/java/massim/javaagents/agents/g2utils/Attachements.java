package massim.javaagents.agents.g2utils;

import java.util.ArrayList;
import java.util.List;

public class Attachements {
	
	private List<RelativeCoordinate> attachedThings = new ArrayList<>();
	private List<RelativeCoordinate> north = new ArrayList<>();
	private List<RelativeCoordinate> east = new ArrayList<>();
	private List<RelativeCoordinate> south = new ArrayList<>();
	private List<RelativeCoordinate> west = new ArrayList<>();
	
	public void addThing(String direction, RelativeCoordinate relCo) {
		attachedThings.add(relCo);
		switch (direction) {
		case ("n"):
			north.add(relCo);
			break;
		case ("e"):
			east.add(relCo);
			break;
		case ("s"):
			south.add(relCo);
			break;
		case ("w"):
			west.add(relCo);
			break;
		default:
			break;
		}
	}

}
