package massim.javaagents.agents.g2utils;

import java.util.*;

public class Task {

    private final String name;
    private final int deadline;
    private final int reward;
    private final List<TaskRequirement> requirements;
    
    private String[][] blockMatrix;;
    private HashMap<String, Integer> blockTypeMap;

    public Task(String name, int deadline, int reward, List<TaskRequirement> requirements){
        this.name = name;
        this.deadline = deadline;
        this.reward = reward;
        this.requirements = requirements;
        
        // create blockMatrix to choose where to place blocks
        this.blockMatrix = createBlockMatrix();
        // create blockTypeMap to decide which block to get
		this.blockTypeMap = createBlockTypeMap();
    }

    public String getName() {
        return this.name;
    }

    public int getDeadline() {
        return this.deadline;
    }

    public int getReward() {
        return this.reward;
    }

    public List<TaskRequirement> getRequirements() {
        return this.requirements;
    }
    
	public String[][] getBlockMatrix() {
		return blockMatrix;
	}

	public HashMap<String, Integer> getBlockTypeMap() {
		return blockTypeMap;
	}

    public boolean equals(Task task) {
        if (this.name.equals(task.getName())) {
            return true;
        } else {
            return false;
        }
    }

	/**
	 * editor: michael
	 *
	 * creates BlockMatrix, sets blockName in place of array
	 * only works if blocks are arranged under agent 
	 *
	 * @return
	 */
	public String[][] createBlockMatrix(){
		String[][] matrix = new String[5][5];
		for (int i =0; i< this.requirements.size(); i++) {
			matrix
				[this.requirements.get(i).getRelativeCoordinate().getX()+2]
				[this.requirements.get(i).getRelativeCoordinate().getY()-1] 
						= this.requirements.get(i).getBlockType();
			//matrix[X][Y]
		}
		/*test for a noob
		for (int i = 0; i < matrix.length; i++) {
		    for (int j = 0; j < matrix[i].length; j++) {
		        System.out.print(matrix[j][i] + " ");
		    }
		    System.out.println();
		}
		*/
		return matrix;		
	}
	
	/**
	 * editor: michael
	 * 
	 * creates HashMap to connect blockName and amount needed for task
	 *
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Integer> createBlockTypeMap(){
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("b0",0);
		map.put("b1",0);
		map.put("b2",0);
		map.put("b3",0);
		
		for (int i =0; i< this.requirements.size(); i++) {			
			switch((String)this.requirements.get(i).getBlockType()) {
			case "b0":
				map.put("b0", map.get("b0")+1);
				break;
			case "b1":
				map.put("b1", map.get("b1")+1);
				break;
			case "b2":
				map.put("b2", map.get("b2")+1);
				break;
			case "b3":
				map.put("b3", map.get("b3")+1);
				break;
			default:
				System.out.println("createBlockTypeMap cannot handle: "+this.requirements.get(i).getBlockType()+"!");
				break;
			}
		}
		return map;
	}
	
	public int getNumberOfDifferentBlocks() {
		int i = 0;
		for (Map.Entry<String, Integer> it : this.blockTypeMap.entrySet()) {
			if (it.getValue() > 0) {
				i++;
			}
		}
		return i;		
	}
	
	public boolean isOneBlockTask() {
		return !this.isMultiBlockTask();
	}
	
	/**
	 * editor: michael
	 *
	 * determine if task needs more than a single block
	 *
	 * @return
	 */
	public boolean isMultiBlockTask() {
		if (this.requirements.size() > 1) return true;
		return false;		
	}
}