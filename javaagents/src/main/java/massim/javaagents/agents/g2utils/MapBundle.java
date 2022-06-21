package massim.javaagents.agents.g2utils;

import java.util.ArrayList;
import java.util.HashMap;

public class MapBundle {
	
	private String owner;
	private int step;
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private ArrayList<RelativeCoordinate> teamMembers;
	private RelativeCoordinate currentPos;
	int currentStep;
	
	public MapBundle(String owner, int step, HashMap<RelativeCoordinate, Block> blockLayer, HashMap<RelativeCoordinate, Dispenser> dispenserLayer, HashMap<RelativeCoordinate, Goalzone> goalzoneLayer, HashMap<RelativeCoordinate, Obstacle> obstacleLayer, HashMap<RelativeCoordinate, Rolezone> rolezoneLayer, ArrayList<RelativeCoordinate> teamMembers, RelativeCoordinate currentPos, int currentStep) {
		this.owner = owner;
		this.step = step;
		this.blockLayer = blockLayer;
		this.dispenserLayer = dispenserLayer;
		this.goalzoneLayer = goalzoneLayer;
		this.obstacleLayer = obstacleLayer;
		this.rolezoneLayer = rolezoneLayer;
		this.teamMembers = teamMembers;
		this.currentPos = currentPos;
		this.currentStep = currentStep;
	}
	
	public ArrayList<RelativeCoordinate> getTeamMembers() {
		return teamMembers;
	}
	
	public RelativeCoordinate getPosition() {
		return currentPos;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public HashMap<RelativeCoordinate, Block> getBlockLayer() {
		return blockLayer;
	}
	
	public HashMap<RelativeCoordinate, Dispenser> getDispenserLayer() {
		return dispenserLayer;
	}
	
	public HashMap<RelativeCoordinate, Goalzone> getGoalzoneLayer() {
		return goalzoneLayer;
	}
	
	public HashMap<RelativeCoordinate, Rolezone> getRolezoneLayer() {
		return rolezoneLayer;
	}
	
	public HashMap<RelativeCoordinate, Obstacle> getObstacleLayer() {
		return obstacleLayer;
	}

}
