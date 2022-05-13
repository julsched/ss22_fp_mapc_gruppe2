package massim.javaagents.agents.g2utils;

import java.util.*;

public class Block extends Cell {

    private final String type;


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
}
