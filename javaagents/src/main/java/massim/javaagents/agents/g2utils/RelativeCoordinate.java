package massim.javaagents.agents.g2utils;

import java.util.*;

import massim.javaagents.agents.g2pathcalc.Direction;

public class RelativeCoordinate {

<<<<<<< Updated upstream
    private final int x;
    private final int y;

    public static RelativeCoordinate getClosestCoordinate(List<RelativeCoordinate> relativeCoordinates) {
        RelativeCoordinate shortestCoordinate = null;
        for (RelativeCoordinate relativeCoordinate : relativeCoordinates) {
            if (shortestCoordinate == null) {
                shortestCoordinate = relativeCoordinate;
                continue;
            }
            if (relativeCoordinate.isCloserThan(shortestCoordinate)) {
                shortestCoordinate = relativeCoordinate;
            }
        }
        return shortestCoordinate;
    }

    public static RelativeCoordinate getRelativeCoordinate(String direction) {
        switch(direction) {
            case "n" -> {
                return new RelativeCoordinate(0, -1);
            }
            case "e" -> {
                return new RelativeCoordinate(1, 0);
            }
            case "s" -> {
                return new RelativeCoordinate(0, 1);
            }
            case "w" -> {
                return new RelativeCoordinate(-1, 0);
            }
        }
        return null;
    }

    public RelativeCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        
        RelativeCoordinate rCo = (RelativeCoordinate) o;
        
        if (this.x != rCo.x) {
        	return false;
        }
        if (this.y != rCo.y) {
        	return false;
        }
        
        return true;
    }

    public boolean isOneStepNorth() {
        if (this.x == 0 && this.y == -1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepEast() {
        if (this.x == 1 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepSouth() {
        if (this.x == 0 && this.y == 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isOneStepWest() {
        if (this.x == -1 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isNextToAgent() {
        if (isOneStepNorth() || isOneStepEast() || isOneStepSouth() || isOneStepWest()) {
            return true;        
        } else {
            return false;
        }
    }

    // Manhattan distance
    public int distanceFromAgent() {
        return Math.abs(this.x) + Math.abs(this.y);
    }

    public boolean isCloserThan(RelativeCoordinate relativeCoordinate) {
        if (this.distanceFromAgent() < relativeCoordinate.distanceFromAgent()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightNorth() {
        if (this.x == 0 && this.y < 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightEast() {
        if (this.x > 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightSouth() {
        if (this.x == 0 && this.y > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isStraightWest() {
        if (this.x < 0 && this.y == 0) {
            return true;
        } else {
            return false;
        }
    }

    public String getDirectDirection() {
        if (this.isStraightNorth()) {
            return "n";
        }
        if (this.isStraightEast()) {
            return "e";
        }
        if (this.isStraightSouth()) {
            return "s";
        }
        if (this.isStraightWest()) {
            return "w";
        }
        return "x";
    }
    
    public String toString() {
    	return "( "+ this.x + ", "+ this.y+ " )";
    }
    
    @Override
=======
	private final int x;
	private final int y;

	public static RelativeCoordinate getClosestCoordinate(List<RelativeCoordinate> relativeCoordinates) {
		return RelativeCoordinate.getClosestCoordinate(relativeCoordinates, new RelativeCoordinate(0, 0));
	}

	public static RelativeCoordinate getClosestCoordinate(List<RelativeCoordinate> relativeCoordinates,
			RelativeCoordinate agentPos) {
		RelativeCoordinate shortestCoordinate = null;
		for (RelativeCoordinate relativeCoordinate : relativeCoordinates) {
			if (shortestCoordinate == null) {
				shortestCoordinate = relativeCoordinate;
				continue;
			}
			if (relativeCoordinate.isCloserThan(shortestCoordinate, agentPos)) {
				shortestCoordinate = relativeCoordinate;
			}
		}
		return shortestCoordinate;
	}

	public static RelativeCoordinate getRelativeCoordinate(String direction) {
		switch (direction) {
		case "n" -> {
			return new RelativeCoordinate(0, -1);
		}
		case "e" -> {
			return new RelativeCoordinate(1, 0);
		}
		case "s" -> {
			return new RelativeCoordinate(0, 1);
		}
		case "w" -> {
			return new RelativeCoordinate(-1, 0);
		}
		}
		return null;
	}

	public RelativeCoordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}

		RelativeCoordinate rCo = (RelativeCoordinate) o;

		if (this.x != rCo.x) {
			return false;
		}
		if (this.y != rCo.y) {
			return false;
		}

		return true;
	}

	public boolean isOneStepNorth() {
		return isOneStepNorth(new RelativeCoordinate(0, 0));
	}

	public boolean isOneStepNorth(RelativeCoordinate pos) {
		if (this.x == pos.getX() && this.y == pos.getY() - 1) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isOneStepEast() {
		return isOneStepEast(new RelativeCoordinate(0, 0));
	}

	public boolean isOneStepEast(RelativeCoordinate pos) {
		if (this.x == pos.getX() + 1 && this.y == pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isOneStepSouth() {
		return isOneStepSouth(new RelativeCoordinate(0, 0));
	}

	public boolean isOneStepSouth(RelativeCoordinate pos) {
		if (this.x == pos.getX() && this.y == pos.getY() + 1) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isOneStepWest() {
		return isOneStepWest(new RelativeCoordinate(0, 0));
	}

	public boolean isOneStepWest(RelativeCoordinate pos) {
		if (this.x == pos.getX() - 1 && this.y == pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isNextToAgent() {
		return isNextToAgent(new RelativeCoordinate(0, 0));
	}

	public boolean isNextToAgent(RelativeCoordinate agentPos) {
		if (isOneStepNorth(agentPos) || isOneStepEast(agentPos) || isOneStepSouth(agentPos)
				|| isOneStepWest(agentPos)) {
			return true;
		} else {
			return false;
		}
	}

	// Manhattan distance
	public int distanceFromAgent() {
		return distanceFromAgent(new RelativeCoordinate(0, 0));
	}

	// Manhattan distance
	public int distanceFromAgent(RelativeCoordinate agentPos) {
		return Math.abs(this.x - agentPos.getX()) + Math.abs(this.y - agentPos.getY());
	}

	public boolean isCloserThan(RelativeCoordinate relativeCoordinate) {
		return isCloserThan(relativeCoordinate, new RelativeCoordinate(0, 0));
	}

	public boolean isCloserThan(RelativeCoordinate relativeCoordinate, RelativeCoordinate agentPos) {
		if (this.distanceFromAgent(agentPos) < relativeCoordinate.distanceFromAgent(agentPos)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isStraightNorth() {
		return isStraightNorth(new RelativeCoordinate(0, 0));
	}

	public boolean isStraightNorth(RelativeCoordinate pos) {
		if (this.x == pos.getX() && this.y < pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isStraightEast() {
		return isStraightEast(new RelativeCoordinate(0, 0));
	}

	public boolean isStraightEast(RelativeCoordinate pos) {
		if (this.x > pos.getX() && this.y == pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isStraightSouth() {
		return isStraightSouth(new RelativeCoordinate(0, 0));
	}

	public boolean isStraightSouth(RelativeCoordinate pos) {
		if (this.x == pos.getX() && this.y > pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isStraightWest() {
		return isStraightWest(new RelativeCoordinate(0, 0));
	}

	public boolean isStraightWest(RelativeCoordinate pos) {
		if (this.x < pos.getX() && this.y == pos.getY()) {
			return true;
		} else {
			return false;
		}
	}

	public String getDirectDirection() {
		return getDirectDirection(new RelativeCoordinate(0, 0));
	}

	public String getDirectDirection(RelativeCoordinate pos) {
		if (this.isStraightNorth(pos)) {
			return "n";
		}
		if (this.isStraightEast(pos)) {
			return "e";
		}
		if (this.isStraightSouth(pos)) {
			return "s";
		}
		if (this.isStraightWest(pos)) {
			return "w";
		}
		return "x";
	}

	public String toString() {
		return "( " + this.x + ", " + this.y + " )";
	}

	@Override
>>>>>>> Stashed changes
	public int hashCode() {

		
		Integer xInt = this.x;
		Integer yInt = this.y;
		int result = 7 * xInt.hashCode() + 11 * yInt.hashCode();
		return result;

	}

	/**
	 * Returns Position Agent ends up at if he walks a given direction from a given
	 * position
	 * 
	 * @param pos Position to start From
	 * @param dir Direction to walk from start point
	 * @return Coordinate of the Position the agent will end up at
	 */
	public RelativeCoordinate getCoordAfterWalkingInDir(String dir) {
		Direction direction = Direction.getDirectionOfString(dir);
		if (direction != null) {
			int xPos = getX();
			int yPos = getY();
			int xDir = direction.getDx();
			int yDir = direction.getDy();
			return new RelativeCoordinate(xPos + xDir, yPos + yDir);
		}
		return this;
	}

}
