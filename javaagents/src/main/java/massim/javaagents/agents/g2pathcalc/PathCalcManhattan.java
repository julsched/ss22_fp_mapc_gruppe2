package massim.javaagents.agents.g2pathcalc;

import java.util.List;
import java.util.Set;

import massim.javaagents.agents.g2utils.Block;
import massim.javaagents.agents.g2utils.MapManagement;
import massim.javaagents.agents.g2utils.RelativeCoordinate;

public class PathCalcManhattan extends PathCalcSuper{
	
	public PathCalcManhattan(MapManagement mapManager, List<Block> attachedBlocks) {
		super(mapManager, attachedBlocks);
	}

	public String calculateShortestPathMap(Set<RelativeCoordinate> destinations) {
		System.out.println("calculateShortestPathMap()");
    	if (destinations == null || destinations.size() == 0) {
            return null;
        }
        
    	RelativeCoordinate destination = getClosestDestManhattan(destinations);
    	if(destination == null) {
    		return null;
    	}
    	RelativeCoordinate currentPos = mapManager.getCurrentPosition();
		// Position of agent inside the map
		int xA = currentPos.getX();
		int yA = currentPos.getY();
		
		int xdest = destination.getX();
		int ydest = destination.getY();
		
		int xDist = xdest - xA;
		int yDist = ydest - yA;
		if(xDist!= 0) {
			if(xDist < 1) {
				return "w";
			}else {
				return "e";
			}
		}if (yDist !=0) {
			if(yDist <1) {
				return "n";
			}else {
				return "s";
			}
		}
       
    	return "";
    }

	private RelativeCoordinate getClosestDestManhattan(Set<RelativeCoordinate> destinations) {
		 int shortestDist = -1;
	        RelativeCoordinate destination = null;
	        for(RelativeCoordinate dest : destinations) {
	        	if (shortestDist == -1) {
	        		shortestDist = manhattanDistFromCurrentPos(dest);
	        	}else {
	        		int dist = manhattanDistFromCurrentPos(dest);
	        		if (dist < shortestDist) {
	        			shortestDist = dist;
	        			destination = dest;
	        		}
	        	}
	        	
	        }
	        return destination;
	}

	private int manhattanDistFromCurrentPos(RelativeCoordinate coord) {
		RelativeCoordinate currentPos = mapManager.getCurrentPosition();
		// Position of agent inside the map
		int xA = currentPos.getX();
		int yA = currentPos.getY();

		int x = coord.getX();
		int y = coord.getY();
		return Math.abs(xA-x) + Math.abs(yA-y);
	}

}
