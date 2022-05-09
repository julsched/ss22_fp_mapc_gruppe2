package massim.javaagents.agents.g2utils;

import java.util.*;

public class Block {

    private final RelativeCoordinate relativeCoordinate;
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

    public Block(RelativeCoordinate relativeCoordinate, String type) {
        this.relativeCoordinate = relativeCoordinate;
        this.type = type;
    }

    public RelativeCoordinate getRelativeCoordinate() {
        return this.relativeCoordinate;
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

    public boolean isCloserThan(Block block) {
        if (this.relativeCoordinate.isCloserThan(block.getRelativeCoordinate())) {
            return true;
        } else {
            return false;
        }
    }
}
