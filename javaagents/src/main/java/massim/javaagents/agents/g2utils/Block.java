package massim.javaagents.agents.g2utils;

import java.util.*;

public class Block extends Cell {

    private final String type;
    private String[][] blockMatrix;


    public static Block getClosestBlock(List<Block> blocks) {
        Block closestBlock = null;
        for (Block block : blocks) {
            if (closestBlock == null) {
                closestBlock = block;
                continue;
            }
            if (block.isCloserThan(closestBlock)) {
                closestBlock = block;
            }
        }
        return closestBlock;
    }

    public Block(RelativeCoordinate relativeCoordinate, String type, int lastSeen) {
    	super(relativeCoordinate, lastSeen);
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public boolean sameTypeAs(Block block) {
        if (this.type.equals(block.getType())) {
            return true;
        } else {
            return false;
        }
    }
    
	public String[][] createBlockMatrix(){
		String[][] matrix = new String[5][5];
		
		if (!this.isConnected()) {
			matrix[2][0] = this.getType();
			return matrix;
		}
		
		// todo matrix for each block iot determine connected neighbors
		
		
		return matrix;		
	}

	public String[][] getBlockMatrix(){
		return this.blockMatrix;
	}
	
	public void setBlockMatrix(String[][] matrix) {
		this.blockMatrix = matrix;
	}
	
	public boolean isConnected() {
		if (this.isConnectedWithN() == null || this.isConnectedWithE() == null 
				|| this.isConnectedWithS() == null || this.isConnectedWithW() == null) {
			return true;
		}
		return false;
	}
	
	
	// todo
	
	/**
	 * editor: michael
	 *
	 * @return block that is connected with the block in North direction
	 */
	public Block isConnectedWithN() {
		return null;
	}
	
	public Block isConnectedWithE() {
		return null;
	}
	
	public Block isConnectedWithS() {
		return null;
	}
	
	public Block isConnectedWithW() {
		return null;
	}
	
	
	
}
