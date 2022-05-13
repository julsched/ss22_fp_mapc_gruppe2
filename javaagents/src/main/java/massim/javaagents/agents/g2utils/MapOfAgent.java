package massim.javaagents.agents.g2utils;

import java.util.HashMap;

public class MapOfAgent extends HashMap<RelativeCoordinate, Cell> {

	/**
	 * CURRENTLY NOT USED --> we need to find out how big the map is for this to work.
	 */
	private static final long serialVersionUID = 1L;
	
	//puts absolute Position of thing that is seen this step on map
	public Cell putThisStep(RelativeCoordinate currentAbsolutePos, RelativeCoordinate relativePos, Cell thing) {
		int absoluteX = currentAbsolutePos.getX() + relativePos.getX();
		int absoluteY = currentAbsolutePos.getY() + relativePos.getY();
		return put(new RelativeCoordinate(absoluteX, absoluteY), thing);	
	}
	

}
