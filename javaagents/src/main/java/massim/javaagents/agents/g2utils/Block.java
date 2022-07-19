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
		matrix[2][2] = this.getType();

		if (!this.isConnected()) {
			return matrix;
		} else {
			if (isConnectedN()) {
				String[][] temp = getBlockNorth().getBlockMatrix();
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 5; j++) {
						matrix[i + 1][j] = temp[i][j];
					}	
				}
			}
			if (isConnectedS()) {
				String[][] temp = getBlockNorth().getBlockMatrix();
				for (int i = 1; i < 4; i++) {
					for (int j = 0; j < 5; j++) {
						matrix[i + 1][j] = temp[i][j];
					}	
				}
			}
			if (isConnectedE()) {
				String[][] temp = getBlockNorth().getBlockMatrix();
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						matrix[i][j + 1] = temp[i][j];
					}	
				}
			}
			if (isConnectedW()) {
				String[][] temp = getBlockNorth().getBlockMatrix();
				for (int i = 1; i < 4; i++) {
					for (int j = 1; j < 5; j++) {
						matrix[i][j - 1] = temp[i][j];
					}	
				}
			}
		}

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
}
