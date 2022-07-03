package massim.javaagents.agents.g2utils;

import java.util.*;

public class Block extends Cell {

    private final String type;
    private String[][] blockMatrix;
    private Block blockNorth = null;
    private Block blockEast = null;
    private Block blockSouth = null;
    private Block blockWest = null;


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
			matrix[2][2] = this.getType();
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


	/**
	 * editor: michael
	 *
	 * @return block that is connected with the block in North direction
	 */
	public Block getBlockNorth() {
		return this.blockNorth;
	}

	public void setBlockNorth(Block block) {
		this.blockNorth = block;
	}

	public Block getBlockEast() {
		return this.blockEast;
	}

	public void setBlockEast(Block block) {
		this.blockEast = block;
	}

	public Block getBlockSouth() {
		return this.blockSouth;
	}

	public void setBlockSouth(Block block) {
		this.blockSouth = block;
	}

	public Block getBlockWest() {
		return this.blockWest;
	}

	public void setBlockWest(Block block) {
		this.blockWest = block;
	}

	public boolean isConnected() {
		if (this.getBlockNorth() == null || this.getBlockEast() == null 
				|| this.getBlockSouth() == null || this.getBlockWest() == null) {
			return true;
		}
		return false;
	}

	public boolean isConnectedN() {
		if (this.getBlockNorth() != null) return true;
		return false;
	}

	public boolean isConnectedE() {
		if (this.getBlockEast() != null) return true;
		return false;
	}

	public boolean isConnectedS() {
		if (this.getBlockSouth() != null) return true;
		return false;
	}

	public boolean isConnectedW() {
		if (this.getBlockWest() != null) return true;
		return false;
	}

	public boolean isAttachedSouth() {
		if (this.getRelativeCoordinate().getX() == 0 && this.getRelativeCoordinate().getY() == -1) {
			return true;
		}
		return false;
	}
}
