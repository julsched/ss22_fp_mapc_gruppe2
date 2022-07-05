package massim.javaagents.agents.g2utils;

import java.util.*;

public class MapManagement {
	
	private HashMap<RelativeCoordinate, Block> blockLayer;
	private HashMap<RelativeCoordinate, Dispenser> dispenserLayer;
	private HashMap<RelativeCoordinate, Goalzone> goalzoneLayer;
	private HashMap<RelativeCoordinate, Obstacle> obstacleLayer;
	private HashMap<RelativeCoordinate, Rolezone> rolezoneLayer;
	private HashMap<RelativeCoordinate, ClearMarker> clearLayer;
	private HashMap<RelativeCoordinate, Entity> entityLayer;
	private HashMap<RelativeCoordinate, Cell> knownArea;
	private RelativeCoordinate currentPosition;
	private int currentStep;
	private ArrayList<RelativeCoordinate> teamMembers = new ArrayList<>();
	
	private RelativeCoordinate lastPosition;
	private ArrayList<RelativeCoordinate> lastTeamMembers = new ArrayList<>();
	
	private boolean containsRolezone = false;
	private boolean containsGoalzone = false;
	private boolean containsDispenser = false;
	// Rolezone cells - no null values
	private Set<RelativeCoordinate> onlyRoleZoneCoords;
	


	/**
	 * Constructor
	 * 
	 * @param currentStep Step of the match
	 */
	public MapManagement(int currentStep) {	
		this.blockLayer = new HashMap<RelativeCoordinate, Block>();
		this.dispenserLayer = new HashMap<RelativeCoordinate, Dispenser>();
		this.goalzoneLayer = new HashMap<RelativeCoordinate, Goalzone>();
		this.obstacleLayer = new HashMap<RelativeCoordinate, Obstacle>();
		this.rolezoneLayer = new HashMap<RelativeCoordinate, Rolezone>();
		this.clearLayer = new HashMap<RelativeCoordinate, ClearMarker>();
		this.entityLayer = new HashMap<RelativeCoordinate, Entity>();
		this.knownArea = new HashMap<RelativeCoordinate, Cell>();
		this.currentPosition = new RelativeCoordinate(0, 0);
		this.currentStep = currentStep;
		this.lastPosition = new RelativeCoordinate(0, 0);
		this.onlyRoleZoneCoords = new HashSet<>();
	}

	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}
	
	/**
	 * Updates the agent's position based on the agent's movement
	 * 
	 * @param direction Direction of movement
	 * @param fromLastStep First movement in step
	 */
	public void updatePosition(String direction, boolean fromLastStep) {
		if (direction.equals("n")) {
			if (fromLastStep == true) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() - 1);
		} else if (direction.equals("s")) {
			if (fromLastStep == true) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY() + 1);
		} else if (direction.equals("e")) {
			if (fromLastStep == true) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() + 1, currentPosition.getY());
		} else if (direction.equals("w")) {
			if (fromLastStep == true) {
				lastPosition = new RelativeCoordinate(currentPosition.getX(), currentPosition.getY());
			}
			currentPosition = new RelativeCoordinate(currentPosition.getX() - 1, currentPosition.getY());
		}		
	}
	
	/**
	 * Updates the map based on in actual step seen things
	 * 
	 * @param tempMap Things seen in the actual step
	 * @param vision Vision range of the agent
	 */
	public void updateMap(HashMap<RelativeCoordinate, List<Cell>> tempMap, int vision) {
		
		int currentX = currentPosition.getX();
		int currentY = currentPosition.getY();
		
		for (int x = -vision; x < (vision + 1); x++) {
			for (int y = 0; (y + Math.abs(x)) < (vision + 1); y++) {
				RelativeCoordinate tempPos = new RelativeCoordinate(x, y);
				RelativeCoordinate absolutePos = new RelativeCoordinate(x + currentX, y + currentY);
				if (tempMap.containsKey(tempPos)) {
					Iterator<Cell> it = tempMap.get(tempPos).iterator();
					while (it.hasNext()) {
						Cell cell = it.next();
						if (!(cell == null)) {
							String type = cell.getClass().getSimpleName();
							switch (type) {
							case ("Dispenser"):
								containsDispenser = true;
								Dispenser disp = (Dispenser) cell;
								disp.setRelativeCoordinate(absolutePos);
								dispenserLayer.put(absolutePos, disp);
								obstacleLayer.put(absolutePos, null);
								if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
									blockLayer.put(absolutePos, null);
								}
								if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									goalzoneLayer.put(absolutePos, null);
								}
								if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									rolezoneLayer.put(absolutePos, null);
								}
								if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
									entityLayer.put(absolutePos, null);
								}
								break;
							case ("Rolezone"):
								containsRolezone = true;
								Rolezone rz = (Rolezone) cell;
								rz.setRelativeCoordinate(absolutePos);
								rolezoneLayer.put(absolutePos, rz);
								onlyRoleZoneCoords.add(absolutePos);
								if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
									obstacleLayer.put(absolutePos, null);
								}
								if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
									blockLayer.put(absolutePos, null);
								}
								if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									goalzoneLayer.put(absolutePos, null);
								}
								if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
									dispenserLayer.put(absolutePos, null);
								}
								if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
									entityLayer.put(absolutePos, null);
								}
								break;
							case ("Obstacle"):
								Obstacle obs = (Obstacle) cell;
								obs.setRelativeCoordinate(absolutePos);
								obstacleLayer.put(absolutePos, obs);
								if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									rolezoneLayer.put(absolutePos, null);
								}
								if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
									blockLayer.put(absolutePos, null);
								}
								if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									goalzoneLayer.put(absolutePos, null);
								}
								entityLayer.put(absolutePos, null);
								dispenserLayer.put(absolutePos, null);
								break;
							case ("Block"):
								Block block = (Block) cell;
								block.setRelativeCoordinate(absolutePos);
								blockLayer.put(absolutePos, block);
								if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									rolezoneLayer.put(absolutePos, null);
								}
								if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
									dispenserLayer.put(absolutePos, null);
								}
								if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									goalzoneLayer.put(absolutePos, null);
								}
								entityLayer.put(absolutePos, null);
								obstacleLayer.put(absolutePos, null);
								break;
							case ("Goalzone"):
								containsGoalzone = true;
								Goalzone gz = (Goalzone) cell;
								gz.setRelativeCoordinate(absolutePos);
								goalzoneLayer.put(absolutePos, gz);
								if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
									obstacleLayer.put(absolutePos, null);
								}
								if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
									blockLayer.put(absolutePos, null);
								}
								if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									rolezoneLayer.put(absolutePos, null);
								}
								if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
									dispenserLayer.put(absolutePos, null);
								}
								if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
									entityLayer.put(absolutePos, null);
								}
								break;
							case ("Entity"):
								Entity entity = (Entity) cell;
								entity.setRelativeCoordinate(absolutePos);
								entityLayer.put(absolutePos, entity);
								if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									rolezoneLayer.put(absolutePos, null);
								}
								if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
									dispenserLayer.put(absolutePos, null);
								}
								if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
									goalzoneLayer.put(absolutePos, null);
								}
								blockLayer.put(absolutePos, null);
								obstacleLayer.put(absolutePos, null);
								break;
							case ("ClearMarker"):
								ClearMarker cm = (ClearMarker) cell;
								clearLayer.put(absolutePos, cm);
								break;
							default:
								break;
							}
						}
						knownArea.put(absolutePos, null);
					}
				} else {
					goalzoneLayer.put(absolutePos, null);
					rolezoneLayer.put(absolutePos, null);
					dispenserLayer.put(absolutePos, null);
					blockLayer.put(absolutePos, null);
					obstacleLayer.put(absolutePos, null);
					clearLayer.put(absolutePos, null);
					entityLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
				}
			}
			for (int y = 0; (Math.abs(y) + Math.abs(x)) < (vision + 1); y--) {
				RelativeCoordinate tempPos = new RelativeCoordinate(x, y);
				RelativeCoordinate absolutePos = new RelativeCoordinate(x + currentX, y + currentY);
				if (tempMap.containsKey(tempPos)) {
					Iterator<Cell> it = tempMap.get(tempPos).iterator();
					while (it.hasNext()) {
						Cell cell = it.next();
						String type = cell.getClass().getSimpleName();
						switch (type) {
						case ("Dispenser"):
							Dispenser disp = (Dispenser) cell;
							disp.setRelativeCoordinate(absolutePos);
							dispenserLayer.put(absolutePos, disp);
							obstacleLayer.put(absolutePos, null);
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Rolezone"):
							Rolezone rz = (Rolezone) cell;
							rz.setRelativeCoordinate(absolutePos);
							rolezoneLayer.put(absolutePos, rz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Obstacle"):
							Obstacle obs = (Obstacle) cell;
							obs.setRelativeCoordinate(absolutePos);
							obstacleLayer.put(absolutePos, obs);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							entityLayer.put(absolutePos, null);
							dispenserLayer.put(absolutePos, null);
							break;
						case ("Block"):
							Block block = (Block) cell;
							block.setRelativeCoordinate(absolutePos);
							blockLayer.put(absolutePos, block);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							entityLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
							break;
						case ("Goalzone"):
							Goalzone gz = (Goalzone) cell;
							gz.setRelativeCoordinate(absolutePos);
							goalzoneLayer.put(absolutePos, gz);
							if ((!obstacleLayer.containsKey(absolutePos)) || (!(obstacleLayer.get(absolutePos) == null) && obstacleLayer.get(absolutePos).getLastSeen() < currentStep)) {
								obstacleLayer.put(absolutePos, null);
							}
							if ((!blockLayer.containsKey(absolutePos)) || (!(blockLayer.get(absolutePos) == null) && blockLayer.get(absolutePos).getLastSeen() < currentStep)) {
								blockLayer.put(absolutePos, null);
							}
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!entityLayer.containsKey(absolutePos)) || (!(entityLayer.get(absolutePos) == null) && entityLayer.get(absolutePos).getLastSeen() < currentStep)) {
								entityLayer.put(absolutePos, null);
							}
							break;
						case ("Entity"):
							Entity entity = (Entity) cell;
							entity.setRelativeCoordinate(absolutePos);
							entityLayer.put(absolutePos, entity);
							if ((!rolezoneLayer.containsKey(absolutePos)) || (!(rolezoneLayer.get(absolutePos) == null) && rolezoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								rolezoneLayer.put(absolutePos, null);
							}
							if ((!dispenserLayer.containsKey(absolutePos)) || (!(dispenserLayer.get(absolutePos) == null) && dispenserLayer.get(absolutePos).getLastSeen() < currentStep)) {
								dispenserLayer.put(absolutePos, null);
							}
							if ((!goalzoneLayer.containsKey(absolutePos)) || (!(goalzoneLayer.get(absolutePos) == null) && goalzoneLayer.get(absolutePos).getLastSeen() < currentStep)) {
								goalzoneLayer.put(absolutePos, null);
							}
							blockLayer.put(absolutePos, null);
							obstacleLayer.put(absolutePos, null);
							break;
						case ("ClearMarker"):
							ClearMarker cm = (ClearMarker) cell;
							clearLayer.put(absolutePos, cm);
							break;
						default:
							break;
						}
						knownArea.put(absolutePos, null);
					}
				} else {
					goalzoneLayer.put(absolutePos, null);
					rolezoneLayer.put(absolutePos, null);
					dispenserLayer.put(absolutePos, null);
					blockLayer.put(absolutePos, null);
					obstacleLayer.put(absolutePos, null);
					clearLayer.put(absolutePos, null);
					entityLayer.put(absolutePos, null);
					knownArea.put(absolutePos, null);
				}
			}
		}

		// For testing
		/*System.out.println("Current Position:");
		System.out.println(currentPosition);
		System.out.println("Goal Zone Layer:");
		System.out.println(goalzoneLayer);
		System.out.println("Role Zone Layer:");
		System.out.println(rolezoneLayer);
		System.out.println("Dispenser Layer:");
		System.out.println(dispenserLayer);
		System.out.println("Block Layer:");
		System.out.println(blockLayer);
		System.out.println("Obstacle Layer:");
		System.out.println(obstacleLayer);
		System.out.println("Entity Layer:");
		System.out.println(entityLayer);
		System.out.println("Known Area:");
		System.out.println(knownArea);*/
	}
	
	/**
	 * Updates the maps based on another map
	 * 
	 * @param mapManager External map
	 */
	public void updateMap(MapManagement mapManager) {
		
		for (RelativeCoordinate key : mapManager.getBlockLayer().keySet()) {
			if (!(blockLayer.get(key) == null) && !(mapManager.getBlockLayer().get(key) == null)) {
				if (blockLayer.get(key).getLastSeen() < mapManager.getBlockLayer().get(key).getLastSeen()) {
					blockLayer.put(key, mapManager.getBlockLayer().get(key));
				}
			}
			if (!blockLayer.containsKey(key)) {
				blockLayer.put(key, mapManager.getBlockLayer().get(key));
			}
			knownArea.put(key, null);
		}
		
		for (RelativeCoordinate key : mapManager.getDispenserLayer().keySet()) {
			if (!(dispenserLayer.get(key) == null) && !(mapManager.getDispenserLayer().get(key) == null)) {
				if (dispenserLayer.get(key).getLastSeen() < mapManager.getDispenserLayer().get(key).getLastSeen()) {
					dispenserLayer.put(key, mapManager.getDispenserLayer().get(key));
				}
			}
			if (!dispenserLayer.containsKey(key)) {
				dispenserLayer.put(key, mapManager.getDispenserLayer().get(key));
			}
		}
		
		for (RelativeCoordinate key : mapManager.getRolezoneLayer().keySet()) {
			if (!(rolezoneLayer.get(key) == null) && !(mapManager.getRolezoneLayer().get(key) == null)) {
				if (rolezoneLayer.get(key).getLastSeen() < mapManager.getRolezoneLayer().get(key).getLastSeen()) {
					rolezoneLayer.put(key, mapManager.getRolezoneLayer().get(key));
				}
			}
			if (!rolezoneLayer.containsKey(key)) {
				rolezoneLayer.put(key, mapManager.getRolezoneLayer().get(key));
			}
		}
		
		for (RelativeCoordinate key : mapManager.getGoalzoneLayer().keySet()) {
			if (!(goalzoneLayer.get(key) == null) && !(mapManager.getGoalzoneLayer().get(key) == null)) {
				if (goalzoneLayer.get(key).getLastSeen() < mapManager.getGoalzoneLayer().get(key).getLastSeen()) {
					goalzoneLayer.put(key, mapManager.getGoalzoneLayer().get(key));
				}
			}
			if (!rolezoneLayer.containsKey(key)) {
				goalzoneLayer.put(key, mapManager.getGoalzoneLayer().get(key));
			}
		}
		
		for (RelativeCoordinate key : mapManager.getObstacleLayer().keySet()) {
			if (!(obstacleLayer.get(key) == null) && !(mapManager.getObstacleLayer().get(key) == null)) {
				if (obstacleLayer.get(key).getLastSeen() < mapManager.getObstacleLayer().get(key).getLastSeen()) {
					obstacleLayer.put(key, mapManager.getObstacleLayer().get(key));
				}
			}
			if (!obstacleLayer.containsKey(key)) {
				obstacleLayer.put(key, mapManager.getObstacleLayer().get(key));
			}
		}
		
	}

	/**
	 * Getter for team members
	 * 
	 * @return List of all seen team members in the actual step
	 */
	public ArrayList<RelativeCoordinate> getTeamMembers() {
		return teamMembers;
	}

	/**
	 * Copies a list of all seen team members' positions in the actual step
	 * 
	 * @return List of seen team members in the actual step
	 */
	public ArrayList<RelativeCoordinate> copyTeamMembers() {
		ArrayList<RelativeCoordinate> temp = new ArrayList<RelativeCoordinate>();
		for (RelativeCoordinate rc : teamMembers) {
			temp.add(new RelativeCoordinate(rc.getX(), rc.getY()));
		}
		return temp;
	}
	
	/**
	 * Getter for team in the last step
	 * 
	 * @return List of all team members in the last step
	 */
	public ArrayList<RelativeCoordinate> getLastTeamMembers() {
		return lastTeamMembers;
	}
	
	/**
	 * Copies a list of all team members' position in the last step
	 * 
	 * @return List of al seen team members in the last step
	 */
	public ArrayList<RelativeCoordinate> copyLastTeamMembers() {
		ArrayList<RelativeCoordinate> temp = new ArrayList<RelativeCoordinate>();
		for (RelativeCoordinate rc : lastTeamMembers) {
			temp.add(new RelativeCoordinate(rc.getX(), rc.getY()));
		}
		return temp;
	}

	/**
	 * Getter for map of blocks
	 * 
	 * @return Map of blocks
	 */
	public HashMap<RelativeCoordinate, Block> getBlockLayer() {
		return blockLayer;
	}
	
	/**
	 * Getter for map of dispensers
	 * 
	 * @return Map of dispensers
	 */
	public HashMap<RelativeCoordinate, Dispenser> getDispenserLayer() {
		return dispenserLayer;
	}
	
	/**
	 * Getter for map of goalzones
	 * 
	 * @return Map of goalzones
	 */
	public HashMap<RelativeCoordinate, Goalzone> getGoalzoneLayer() {
		return goalzoneLayer;
	}
	
	/**
	 * Getter for map of rolezones
	 * 
	 * @return Map of Rolezones
	 */
	public HashMap<RelativeCoordinate, Rolezone> getRolezoneLayer() {
		return rolezoneLayer;
	}
	
	/**
	 * Getter for map of obstacles
	 * 
	 * @return Map of obstacles
	 */
	public HashMap<RelativeCoordinate, Obstacle> getObstacleLayer() {
		return obstacleLayer;
	}

	public HashMap<RelativeCoordinate, Entity> getEntityLayer() {
		return entityLayer;
	}
	
	/**
	 * Getter for position in actual step
	 * 
	 * @return Current position
	 */
	public RelativeCoordinate getPosition() {
		return currentPosition;
	}
	
	/**
	 * Setter for current position
	 * 
	 * @param newPosition Current Position
	 */
	public void setPosition(RelativeCoordinate newPosition) {
		currentPosition = newPosition;
	}
	
	/**
	 * Getter for position in last step
	 * 
	 * @return Position in last step
	 */
	public RelativeCoordinate getLastPosition() {
		return lastPosition;
	}
	
	/**
	 * Merges map with transfered map
	 * 
	 * @param mapBundle Transfered MAp
	 * @param exchangePartner Difference between coordinate system of own map and transfered map
	 * @return Owner of transfered map
	 */
	public String mergeMaps(MapBundle mapBundle, RelativeCoordinate exchangePartner) {
		
		// Mergen der verschiedenen KartenLayer
		for (RelativeCoordinate key : mapBundle.getBlockLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + exchangePartner.getX(), key.getY() + exchangePartner.getY());
			if (!(blockLayer.get(newKey) == null) && !(mapBundle.getBlockLayer().get(key) == null)) {
				if (blockLayer.get(newKey).getLastSeen() < mapBundle.getBlockLayer().get(key).getLastSeen()) {
					blockLayer.put(newKey, mapBundle.getBlockLayer().get(key));
				}
			}
			if (!blockLayer.containsKey(newKey)) {
				blockLayer.put(newKey, mapBundle.getBlockLayer().get(key));
			}
			knownArea.put(newKey, null);
		}
		for (RelativeCoordinate key : mapBundle.getDispenserLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + exchangePartner.getX(), key.getY() + exchangePartner.getY());
			if (!(dispenserLayer.get(newKey) == null) && !(mapBundle.getDispenserLayer().get(key) == null)) {
				if (dispenserLayer.get(newKey).getLastSeen() < mapBundle.getDispenserLayer().get(key).getLastSeen()) {
					dispenserLayer.put(newKey, mapBundle.getDispenserLayer().get(key));
				}
			}
			if (!dispenserLayer.containsKey(newKey)) {
				dispenserLayer.put(newKey, mapBundle.getDispenserLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getGoalzoneLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + exchangePartner.getX(), key.getY() + exchangePartner.getY());
			if (!(goalzoneLayer.get(newKey) == null) && !(mapBundle.getGoalzoneLayer().get(key) == null)) {
				if (goalzoneLayer.get(newKey).getLastSeen() < mapBundle.getGoalzoneLayer().get(key).getLastSeen()) {
					goalzoneLayer.put(newKey, mapBundle.getGoalzoneLayer().get(key));
				}
			}
			if (!goalzoneLayer.containsKey(newKey)) {
				goalzoneLayer.put(newKey, mapBundle.getGoalzoneLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getRolezoneLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + exchangePartner.getX(), key.getY() + exchangePartner.getY());
			if (!(rolezoneLayer.get(newKey) == null) && !(mapBundle.getRolezoneLayer().get(key) == null)) {
				if (rolezoneLayer.get(newKey).getLastSeen() < mapBundle.getRolezoneLayer().get(key).getLastSeen()) {
					rolezoneLayer.put(newKey, mapBundle.getRolezoneLayer().get(key));
				}
			}
			if (!rolezoneLayer.containsKey(newKey)) {
				rolezoneLayer.put(newKey, mapBundle.getRolezoneLayer().get(key));
			}			
		}
		for (RelativeCoordinate key : mapBundle.getObstacleLayer().keySet()) {
			RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + exchangePartner.getX(), key.getY() + exchangePartner.getY());
			if (!(obstacleLayer.get(newKey) == null) && !(mapBundle.getObstacleLayer().get(key) == null)) {
				if (obstacleLayer.get(newKey).getLastSeen() < mapBundle.getObstacleLayer().get(key).getLastSeen()) {
					obstacleLayer.put(newKey, mapBundle.getObstacleLayer().get(key));
				}
			}
			if (!obstacleLayer.containsKey(newKey)) {
				obstacleLayer.put(newKey, mapBundle.getObstacleLayer().get(key));
			}			
		}		
		return mapBundle.getOwner();
	}
	
	public void removeObstacle(RelativeCoordinate obstaclePosition) {
		obstacleLayer.put(obstaclePosition, null);
	}
	
	/**
	 * Adapts position to a new coordinate system
	 * 
	 * @param x Difference x-axis
	 * @param y Difference y-axis
	 */
	public void updateLastPosition(int x, int y) {
		lastPosition = new RelativeCoordinate(lastPosition.getX() + x, lastPosition.getY() + y);
	}

	public HashMap<String, RelativeCoordinate> analyzeMapDimensions() {
		// Most northern cell that has been analyzed
		RelativeCoordinate north = new RelativeCoordinate(0, 0);
		// Most eastern cell that has been analyzed
		RelativeCoordinate east = new RelativeCoordinate(0, 0);
		// Most southern cell that has been analyzed
		RelativeCoordinate south = new RelativeCoordinate(0, 0);
		// Most western cell that has been analyzed
		RelativeCoordinate west = new RelativeCoordinate(0, 0);
		for (RelativeCoordinate relativeCoordinate : knownArea.keySet()) {
			if (relativeCoordinate.getX() > east.getX()) {
				east = relativeCoordinate;
			}
			if (relativeCoordinate.getX() < west.getX()) {
				west = relativeCoordinate;
			}
			if (relativeCoordinate.getY() > south.getY()) {
				south = relativeCoordinate;
			}
			if (relativeCoordinate.getY() < north.getY()) {
				north = relativeCoordinate;
			}
		}
		HashMap<String, RelativeCoordinate> map = new HashMap<>();
		map.put("north", north);
		map.put("east", east);
		map.put("south", south);
		map.put("west", west);
		return map;
	}
	
	public boolean containsRolezone() {
		return containsRolezone;
	}
	
	public boolean containsGoalzone() {
		return containsGoalzone;
	}
	
	public boolean containsDispenser() {
		return containsDispenser;
	}
	

	public Set<RelativeCoordinate> getOnlyRoleZoneCoords() {
		return onlyRoleZoneCoords;
	}
	
}
