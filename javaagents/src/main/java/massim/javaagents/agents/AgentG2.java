package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.agents.g2utils.*;
import massim.javaagents.agents.g2pathcalc.*;

import java.util.*;

public class AgentG2 extends Agent {

	private String teamName;
	private int teamSize;
	private int simSteps;
	private List<Role> roles = new ArrayList<>();
	private String explorerAgent;
	boolean simStartPerceptsSaved;

	private int currentStep = -1;
	private long currentScore;
	private int energyLevel;
	private boolean currentlyActive;
	private Role currentRole;

	private long actionID = -1;
	private long timestamp;
	private long deadline;
	private String lastAction;
	private List<Object> lastActionParams = new ArrayList<>();
	private String lastActionResult;
	private RelativeCoordinate hitFrom;
	private List<Norm> violations = new ArrayList<>();

	private HashMap<RelativeCoordinate, List<Cell>> tempMap = new HashMap<RelativeCoordinate, List<Cell>>();

	private List<Percept> attachedThingsPercepts = new ArrayList<>();
	private List<Block> blocks = new ArrayList<>();
	private List<RelativeCoordinate> occupiedFields = new ArrayList<>();
	private List<Task> tasks = new ArrayList<>();
	private List<Norm> norms = new ArrayList<>();
	// Blocks that might(!) be directly attached to the agent (right next to agent)
	private List<Block> attachedBlocks = new ArrayList<>();

	private MapManagement mapManager;
	private PathCalc pathCalc;
	private boolean initiateMapExchange = false;
	private int counterMapExchange = 0;
	private AgentInformation seenAgent = null;
	private boolean requestForMap = false;
	private String requestingExplorer;
	private int stepOfRequest = -3;
	private int exchangeCounter = 0;
	private int stepOfSentMap;
	private HashMap<RelativeCoordinate, Block> receivedBlocks;
	private HashMap<RelativeCoordinate, Dispenser> receivedDispensers;
	private HashMap<RelativeCoordinate, Goalzone> receivedGoalzones;
	private HashMap<RelativeCoordinate, Rolezone> receivedRolezones;
	private HashMap<RelativeCoordinate, Obstacle> receivedObstacles;
	private RelativeCoordinate receivedPosition;

	private ArrayList<RelativeCoordinate> friendlyAgents = new ArrayList<RelativeCoordinate>();
	private HashMap<RelativeCoordinate, Cell> map = new HashMap<RelativeCoordinate, Cell>(); // see map
	private RelativeCoordinate currentPos = new RelativeCoordinate(0, 0); // TODO delete if currentAbsolutePos works.
	private Orientation orientation = Orientation.NORTH;
	private HashMap<RelativeCoordinate, Cell> attachedBlocksWithPositions = new HashMap<>();
	private String roleName = ""; // TODO -> automatisch aktualisieren, wenn Rolle geändert wird

	private String lastMoveDir = "";
	private String dirToFindBorder = "";
	private List<RelativeCoordinate> obstaclesInSight = new ArrayList<>();
	private ArrayList<String> dirOfBorders = new ArrayList<>();
	private String dirOfFocusBorder = "";
	private boolean wasNextToObstacleLastStep = false;
	private String moveDir2StepsAgo = "";
	private String moveDir3StepsAgo = "";
	private boolean comesFromDeadEnd = false;
	private String preferredDir = "";
	private int preferredDirTimer = -1;
	private List<RelativeCoordinate> occupiedFieldsWithoutBlocks = new ArrayList<>();

//	private MapOfAgent map = new MapOfAgent();
//	private RelativeCoordinate currentAbsolutePos = new RelativeCoordinate(0, 0);

	private HashMap<String, Integer> attachedBlockTypeMap;
	private Task currentTask;

	/**
	 * Constructor.
	 * 
	 * @param name    the agent's name
	 * @param mailbox the mail facility
	 */
	public AgentG2(String name, MailService mailbox) {
		super(name, mailbox);
		this.mapManager = new MapManagement(this.currentStep, this.currentPos);
		this.pathCalc = new PathCalc(mapManager, attachedBlocks);
	}

	@Override
	public void handlePercept(Percept percept) {
	}

	@Override
	public void handleMessage(Percept message, String sender) {
	}

	@Override
	public Action step() {
		List<Percept> percepts = getPercepts();
		if (simSteps != 0 && currentStep == simSteps - 1) {
			saveSimEndPercepts(percepts);
			say("Bye! See you in the next simulation!");
			prepareForNextSimulation();
			return new Action("skip");
		}
		if (!simStartPerceptsSaved) {
			saveSimStartPercepts(percepts);
			if (teamSize > 1) {
				explorerAgent = Mission.applyForExplorerMission(getName());
			} else {
				explorerAgent = "None";
			}
			return new Action("skip");
		}

		// must be set first, so agents knows currentStep for sorting Percepts and for
		// having a structured console output
		setCurrentStep(percepts);

		saveStepPercepts(percepts);

		mapManager.setTeamMembers(friendlyAgents);

		analyzeAttachedThings();
		setAttachedBlockTypeMap();

		// Auswertung der abgespeicherten Ergebnisse der lastAction
		evaluateLastAction();

		// nach der Evaluation ist die currentPosition korrekt bestimmt und es können
		// die things der map hinzugefügt werden
		mapManager.updateMap(tempMap, currentRole.getVision());
		tempMap = new HashMap<RelativeCoordinate, List<Cell>>();

		// Einleiten des Austausches der maps
		if (initiateMapExchange) {
			say("I want your map, " + mapManager.getExchangePartner().getName());
			requestMap(mapManager.getExchangePartner().getName(), currentStep);
		}

		// Übergeben der aktuellen Map
		if (requestForMap) {
			say("I give you my map");
			if (stepOfRequest == currentStep) {
				mailbox.deliverMap(requestingExplorer, getName(), mapManager.getBlockLayer(),
						mapManager.getDispenserLayer(), mapManager.getGoalzoneLayer(), mapManager.getRolezoneLayer(),
						mapManager.getObstacleLayer(), currentPos, currentStep);
				stepOfRequest = -3;
			}
			if (stepOfRequest == currentStep - 1) {
				mailbox.deliverMap(requestingExplorer, getName(), mapManager.getBlockLayer(),
						mapManager.getDispenserLayer(), mapManager.getGoalzoneLayer(), mapManager.getRolezoneLayer(),
						mapManager.getObstacleLayer(), mapManager.getLastPosition(), currentStep - 1);
				stepOfRequest = -3;
			}
			exchangeCounter = exchangeCounter + 1;
			if (exchangeCounter > 1) {
				stepOfRequest = -3;
				requestForMap = false;
			}
		}

		// Zusammenführen der Maps und Übergeben der geupdateten Map
		if (!(receivedBlocks == null)) {
			say("I merge the maps");
			mapManager.mergeMaps(receivedBlocks, receivedDispensers, receivedGoalzones, receivedRolezones,
					receivedObstacles, receivedPosition, stepOfSentMap);
			sendMap(seenAgent.getName(), mapManager, seenAgent.getRelativeCoordinate());
			receivedBlocks = null;
			receivedDispensers = null;
			receivedGoalzones = null;
			receivedRolezones = null;
			receivedObstacles = null;
		}
		initiateMapExchange = false;

		if (explorerAgent.equals(getName())) {
			say("My mission: I am the explorer of the team!");
			if (!lastActionResult.equals("success")) {
				return handleError();
			}
			return explorerStep();
		} else {
			say("My mission: I am just a normal worker :(");
			if (!lastActionResult.equals("success")) {
				return handleError();
			}
			return workerStep();
		}
	}

	private void setCurrentStep(List<Percept> percepts) {
		for (Percept percept : percepts) {
			if (percept.getName().equals("step")) {
				currentStep = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				mapManager.setCurrentStep(currentStep);
				say("------------------ Current step: " + currentStep + " ------------------");
				return;
			}
		}
	}

	private void saveStepPercepts(List<Percept> percepts) {
		if (percepts == null) { // Error handling if no percepts are available
			return;
		}

		// Delete previous step percepts
		lastActionParams.clear();
		attachedThingsPercepts.clear();
		blocks.clear();
		friendlyAgents.clear();
		occupiedFields.clear();
		tasks.clear();
		norms.clear();
		hitFrom = null;
		obstaclesInSight.clear();
		occupiedFieldsWithoutBlocks.clear();

		// Save new step percepts
		for (Percept percept : percepts) {
			String perceptName = percept.getName();
			switch (perceptName) {
			case "actionID" -> {
				long id = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				if (id > actionID) {
					actionID = id;
				}
				break;
			}
			case "timestamp" -> {
				timestamp = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}
			case "deadline" -> {
				deadline = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}
			case "score" -> {
				currentScore = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}
			case "energy" -> {
				energyLevel = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				break;
			}
			case "deactivated" -> {
				String value = ((Identifier) percept.getParameters().get(0)).getValue();
				if (value.equals("true")) {
					currentlyActive = false;
				} else {
					currentlyActive = true;
				}
				break;
			}
			case "lastAction" -> {
				lastAction = ((Identifier) percept.getParameters().get(0)).getValue();
				break;
			}
			case "lastActionResult" -> {
				lastActionResult = ((Identifier) percept.getParameters().get(0)).getValue();
				break;
			}
			case "lastActionParams" -> {
				Parameter lastParams = percept.getParameters().get(0);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) lastParams).size(); i++) {
					params.add(((ParameterList) lastParams).get(i));
				}
				for (Parameter param : params) {
					if (param instanceof Identifier) {
						String stringParam = ((Identifier) param).getValue();
						lastActionParams.add(stringParam);
						continue;
					}
					if (param instanceof Numeral) {
						Integer intParam = ((Numeral) param).getValue().intValue();
						lastActionParams.add(intParam);
					}
				}
				break;
			}
			case "violations" -> {
				// TODO
				break;
			}
			case "thing" -> {
				String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
				// Maybe Check if x and y are Numeral first
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				RelativeCoordinate relativeCoordinate = new RelativeCoordinate(x, y);
				if (thingType.equals("dispenser")) {
					String type = ((Identifier) percept.getParameters().get(3)).getValue();
					Dispenser dispenser = new Dispenser(relativeCoordinate, type, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, dispenser);
					if (!tempMap.containsKey(relativeCoordinate)) {
						ArrayList<Cell> cellList = new ArrayList<Cell>();
						cellList.add(dispenser);
						tempMap.put(relativeCoordinate, cellList);
					} else {
						List<Cell> cellList = tempMap.get(relativeCoordinate);
						cellList.add(dispenser);
					}
					break;
				}
				if (thingType.equals("block")) {
					String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
					Block block = new Block(relativeCoordinate, blockType, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, block);
					blocks.add(new Block(relativeCoordinate, blockType, currentStep)); 
					// Should not have same block object as in map since coordinates differ
					occupiedFields.add(relativeCoordinate);
					if (!tempMap.containsKey(relativeCoordinate)) {
						ArrayList<Cell> cellList = new ArrayList<Cell>();
						cellList.add(block);
						tempMap.put(relativeCoordinate, cellList);
					} else {
						List<Cell> cellList = tempMap.get(relativeCoordinate);
						cellList.add(block);
					}
					break;
				}
				if (thingType.equals("entity")) {
					String team = ((Identifier) percept.getParameters().get(3)).getValue();
					Entity entity = new Entity(relativeCoordinate, team, currentStep);
					// Only add entity to map if it's not the agent itself
					if (!(x == 0 && y == 0)) {
						if (!tempMap.containsKey(relativeCoordinate)) {
							ArrayList<Cell> cellList = new ArrayList<Cell>();
							cellList.add(entity);
							tempMap.put(relativeCoordinate, cellList);
						} else {
							List<Cell> cellList = tempMap.get(relativeCoordinate);
							cellList.add(entity);
						}
					}
					occupiedFields.add(relativeCoordinate);
					occupiedFieldsWithoutBlocks.add(relativeCoordinate);
					RelativeCoordinate ownPosition = new RelativeCoordinate(0, 0);
					if (teamName.equals(team) && (!relativeCoordinate.equals(ownPosition))) {
						RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPos.getX() + x,
								this.currentPos.getY() + y);
						this.friendlyAgents.add(absolutePosition);
					}
					break;
				}
				if (thingType.equals("obstacle")) {
					occupiedFields.add(relativeCoordinate);
					occupiedFieldsWithoutBlocks.add(relativeCoordinate);
					Obstacle obstacle = new Obstacle(relativeCoordinate, currentStep);
					if (!tempMap.containsKey(relativeCoordinate)) {
						ArrayList<Cell> cellList = new ArrayList<Cell>();
						cellList.add(obstacle);
						tempMap.put(relativeCoordinate, cellList);
					} else {
						List<Cell> cellList = tempMap.get(relativeCoordinate);
						cellList.add(obstacle);
					}
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, obstacle); //TODO @Carina -> make current AbsolutePos work. Then we can make a Map. 
					obstaclesInSight.add(relativeCoordinate);
					break;
				}
				if (thingType.equals("marker")) {
					String info = ((Identifier) percept.getParameters().get(3)).getValue();
					// TODO
					break;
				}
				break;
			}
			case "task" -> {
				String name = ((Identifier) percept.getParameters().get(0)).getValue();
				int deadline = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				int reward = ((Numeral) percept.getParameters().get(2)).getValue().intValue();

				// Ignore tasks which have expired
				if (deadline < currentStep) {
					break;
				}

				Parameter paramRequirements = percept.getParameters().get(3);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
					params.add(((ParameterList) paramRequirements).get(i));
				}
				/*
				// Remove if-statement once agent can handle multi-block tasks
				if (params.size() > 1) {
					say("Task " + name + " has more than one block. Ignore.");
					break;
				}
				**/
				
				List<TaskRequirement> requirements = new ArrayList<>();
				for (Parameter param : params) {
					Parameter paramCoordinateX = ((Function) param).getParameters().get(0);
					Parameter paramCoordinateY = ((Function) param).getParameters().get(1);
					Parameter paramBlockType = ((Function) param).getParameters().get(2);

					int x = ((Numeral) paramCoordinateX).getValue().intValue();
					int y = ((Numeral) paramCoordinateY).getValue().intValue();
					String blockType = ((Identifier) paramBlockType).getValue();

					TaskRequirement requirement = new TaskRequirement(new RelativeCoordinate(x, y), blockType);
					requirements.add(requirement);
				}
				Task task = new Task(name, deadline, reward, requirements);
				tasks.add(task);
				break;
			}
			case "norm" -> {
				String normName = ((Identifier) percept.getParameters().get(0)).getValue();
				int firstStep = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				int lastStep = ((Numeral) percept.getParameters().get(2)).getValue().intValue();
				int punishment = ((Numeral) percept.getParameters().get(4)).getValue().intValue();

				Parameter paramRequirements = percept.getParameters().get(3);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
					params.add(((ParameterList) paramRequirements).get(i));
				}
				List<NormRequirement> requirements = new ArrayList<>();
				for (Parameter param : params) {
					Parameter paramType = ((Function) param).getParameters().get(0);
					Parameter paramName = ((Function) param).getParameters().get(1);
					Parameter paramQuantity = ((Function) param).getParameters().get(2);
					Parameter paramDetails = ((Function) param).getParameters().get(3);

					String type = ((Identifier) paramType).getValue();
					String name = ((Identifier) paramName).getValue();
					int quantity = ((Numeral) paramQuantity).getValue().intValue();
					String details = ((Identifier) paramDetails).getValue();

					NormRequirement requirement = new NormRequirement(type, name, quantity, details);
					requirements.add(requirement);
				}
				Norm norm = new Norm(normName, firstStep, lastStep, requirements, punishment);
				norms.add(norm);
				break;
			}
			case "attached" -> {
				attachedThingsPercepts.add(percept);
				break;
			}
			case "goalZone" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate goalZoneField = new RelativeCoordinate(x, y);
				if (!tempMap.containsKey(goalZoneField)) {
					ArrayList<Cell> cellList = new ArrayList<Cell>();
					cellList.add(new Goalzone(goalZoneField, currentStep));
					tempMap.put(goalZoneField, cellList);
				} else {
					List<Cell> cellList = tempMap.get(goalZoneField);
					cellList.add(new Goalzone(goalZoneField, currentStep));
				}
				break;
			}
			case "roleZone" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate roleZoneField = new RelativeCoordinate(x, y);
				if (!tempMap.containsKey(roleZoneField)) {
					ArrayList<Cell> cellList = new ArrayList<Cell>();
					cellList.add(new Rolezone(roleZoneField, currentStep));
					tempMap.put(roleZoneField, cellList);
				} else {
					List<Cell> cellList = tempMap.get(roleZoneField);
					cellList.add(new Rolezone(roleZoneField, currentStep));
				}
				break;
			}
			case "role" -> {
				if (percept.getParameters().size() == 1) {
					String roleName = ((Identifier) percept.getParameters().get(0)).getValue();
					if (currentRole == null || !currentRole.getName().equals(roleName)) {
						currentRole = Role.getRole(roles, roleName);
					}
					if (currentRole != null) {
						say("My current role: " + currentRole.getName());
					}
				}
				break;
			}
			case "surveyed" -> {
				String target = ((Identifier) percept.getParameters().get(0)).getValue();
				switch (target) {
				case "agent" -> {
					String name = ((Identifier) percept.getParameters().get(1)).getValue();
					String role = ((Identifier) percept.getParameters().get(2)).getValue();
					int energy = ((Numeral) percept.getParameters().get(3)).getValue().intValue();
					say("Partner: " + name);
					mapManager.setExchangePartner(name, role, energy);
					this.initiateMapExchange = true;
					break;
				}
				case "goal" -> {
					// TODO
				}
				case "role" -> {
					// TODO
				}
				case "dispenser" -> {
					// TODO
				}
				}
				break;
			}
			case "hit" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate origin = new RelativeCoordinate(x, y);
				hitFrom = origin;
				break;
			}
			}
		}

		// Saves the last Direction where the Agent successfully walked towards
		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
			if (lastAction.equals("move") && lastActionResult.equals("success")) {
				String lastParamsString = lastActionParams.get(0) + "";
				lastParamsString = lastParamsString.replace("[", "");
				lastParamsString = lastParamsString.replace("]", "");
				this.moveDir3StepsAgo = moveDir2StepsAgo;
				this.moveDir2StepsAgo = lastMoveDir;
				this.lastMoveDir = lastParamsString;
			}
		}
//		setCurrentAbsolutePos();
	}

	// Analyzes and saves things that are attached to the agent
	private void analyzeAttachedThings() {
		if (lastAction.equals("submit") && lastActionResult.equals("success")) {
			attachedBlocks.clear();
			return;
		}

		if (!attachedBlocks.isEmpty()) {
			if (lastAction.equals("detach") && lastActionResult.equals("success")) {
				String direction = (String) lastActionParams.get(0);
				Block blockToBeRemoved = null;
				switch(direction) {
					case "n" -> {
						for (Block block : attachedBlocks) {
							if (block.getRelativeCoordinate().equals(new RelativeCoordinate(0, -1))) {
								blockToBeRemoved = block;
								break;
							}
						}
					}
					case "e" -> {
						for (Block block : attachedBlocks) {
							if (block.getRelativeCoordinate().equals(new RelativeCoordinate(1, 0))) {
								blockToBeRemoved = block;
								break;
							}
						}
					}
					case "s" -> {
						for (Block block : attachedBlocks) {
							if (block.getRelativeCoordinate().equals(new RelativeCoordinate(0, 1))) {
								blockToBeRemoved = block;
								break;
							}
						}
					}
					case "w" -> {
						for (Block block : attachedBlocks) {
							if (block.getRelativeCoordinate().equals(new RelativeCoordinate(-1, 0))) {
								blockToBeRemoved = block;
								break;
							}
						}
					}
				}
				if (blockToBeRemoved != null) {
					attachedBlocks.remove(blockToBeRemoved);
				}
			}
			if (lastAction.equals("rotate") && lastActionResult.equals("success")) {
				// Works only if exactly one block is attached to agent
				String direction = (String) lastActionParams.get(0);
				Block block = attachedBlocks.get(0);
				attachedBlocks.clear();
				if (direction.equals("cw")) {
					if (block.getRelativeCoordinate().isOneStepEast()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, 1), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepSouth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(-1, 0), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepWest()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, -1), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepNorth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(1, 0), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
				} else if (direction.equals("ccw")) {
					if (block.getRelativeCoordinate().isOneStepEast()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, -1), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepSouth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(1, 0), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepWest()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, 1), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepNorth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(-1, 0), block.getType(),
								block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
				}
			}
			// TODO: check every step if things are still attached (could have been removed
			// by a clear event?)
		}

		if (lastAction.equals("attach") && lastActionResult.equals("success")) {
			String direction = (String) lastActionParams.get(0);
			switch (direction) {
			case "n" -> {
				// Could be friendly entity/obstacle/block -> check what it is and save in
				// dedicated list
				for (Block block : blocks) {
					if (block.getRelativeCoordinate().equals(new RelativeCoordinate(0, -1))) {
						attachedBlocks.add(block);
						break;
					}
				}
				// TODO: Same should be done with entity and obstacle
				break;
			}
			case "e" -> {
				// Could be friendly entity/obstacle/block -> check what it is and save in
				// dedicated list
				for (Block block : blocks) {
					if (block.getRelativeCoordinate().equals(new RelativeCoordinate(1, 0))) {
						attachedBlocks.add(block);
						break;
					}
				}
				// TODO: Same should be done with entity and obstacle
				break;
			}
			case "s" -> {
				// Could be friendly entity/obstacle/block -> check what it is and save in
				// dedicated list
				for (Block block : blocks) {
					if (block.getRelativeCoordinate().equals(new RelativeCoordinate(0, 1))) {
						attachedBlocks.add(block);
						break;
					}
				}
				// TODO: Same should be done with entity and obstacle
				break;
			}
			case "w" -> {
				// Could be friendly entity/obstacle/block -> check what it is and save in
				// dedicated list
				for (Block block : blocks) {
					if (block.getRelativeCoordinate().equals(new RelativeCoordinate(-1, 0))) {
						attachedBlocks.add(block);
						break;
					}
				}
				// TODO: Same should be done with entity and obstacle
				break;
			}
			}
		}

		say("Attached blocks: ");
		for (Block block : attachedBlocks) {
			say("(" + block.getRelativeCoordinate().getX() + "|" + block.getRelativeCoordinate().getY() + ")");
		}
	}

//	We only need this, if we want to use currentAbsolutePos.
	// updates the current absolute Position of the Agent.
//	private void setCurrentAbsolutePos() {
//		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
//			if (lastAction.equals("move") && lastActionResult.equals("success")) {
//				int x = 0;
//				int y = 0;
//				switch (lastMoveDir) {
//				case "n": {
//					y = 1;
//					break;
//				}
//				case "e": {
//					x = 1;
//					break;
//				}
//				case "s": {
//					y = -1;
//					break;
//				}
//				case "w": {
//					x = -1;
//					break;
//				}
//				}
//				currentAbsolutePos = new RelativeCoordinate(currentAbsolutePos.getX() + x,
//						currentAbsolutePos.getY() + y);
//			}
//		}
//
//	}

	private void saveSimStartPercepts(List<Percept> percepts) {
		for (Percept percept : percepts) {
			String perceptName = percept.getName();
			switch (perceptName) {
			case "team" -> {
				say("Saving team name...");
				teamName = ((Identifier) percept.getParameters().get(0)).getValue();
				break;
			}
			case "teamSize" -> {
				say("Saving team size...");
				teamSize = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				break;
			}
			case "steps" -> {
				say("Saving simulation step number...");
				simSteps = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				break;
			}
			case "role" -> {
				if (percept.getParameters().size() == 6) {
					String roleName = ((Identifier) percept.getParameters().get(0)).getValue();
					int roleVision = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					double clearChance = ((Numeral) percept.getParameters().get(4)).getValue().doubleValue();
					int clearMaxDist = ((Numeral) percept.getParameters().get(5)).getValue().intValue();

					Parameter paramActions = percept.getParameters().get(2);
					List<Parameter> params = new ArrayList<>();
					for (int i = 0; i < ((ParameterList) paramActions).size(); i++) {
						params.add(((ParameterList) paramActions).get(i));
					}
					List<String> actions = new ArrayList<>();
					for (Parameter param : params) {
						String action = ((Identifier) param).getValue();
						actions.add(action);
					}

					Parameter paramSpeeds = percept.getParameters().get(3);
					params = new ArrayList<>();
					for (int i = 0; i < ((ParameterList) paramSpeeds).size(); i++) {
						params.add(((ParameterList) paramSpeeds).get(i));
					}
					List<Integer> speeds = new ArrayList<>();
					for (Parameter param : params) {
						int speed = ((Numeral) param).getValue().intValue();
						speeds.add(speed);
					}
					Role role = new Role(roleName, roleVision, actions, speeds, clearChance, clearMaxDist);
					say("Saving information for role '" + roleName + "'...");
					roles.add(role);
				}
				break;
			}
			}
		}
		simStartPerceptsSaved = true;
	}

	private void saveSimEndPercepts(List<Percept> percepts) {
		for (Percept percept : percepts) {
			String perceptName = percept.getName();
			switch (perceptName) {
			case "ranking" -> {
				// TODO
				say("Now analyzing simulation end percept 'ranking'");
				break;
			}
			case "score" -> {
				// TODO
				say("Now analyzing simulation end percept 'score'");
				break;
			}
			}
		}
	}

	// Verarbeitet die Informationen zur letzten Aktion und passt die Annahmen des
	// Agenten an
	// TODO: eigentliche Evaluation
	private void evaluateLastAction() {
		switch (lastAction) {
		case "skip":
			break;
		case "move":
			if (this.lastActionResult.equals("success")) {
				Iterator<Object> it = lastActionParams.iterator();
				int counter = 1;
				while (it.hasNext()) {
					Object temp = it.next();
					String dir = temp.toString();
					say("Direction is: " + dir);
					int x = currentPos.getX();
					int y = currentPos.getY();
					mapManager.updatePosition(x, y, dir, counter);
					counter = counter + 1;
				}
			} else if (this.lastActionResult.equals("partial_success")) {
				String dir = (String) this.lastActionParams.get(0);
				int x = currentPos.getX();
				int y = currentPos.getY();
				mapManager.updatePosition(x, y, dir, 1);
				// TODO: Fehlerbehandlung, falls Agent mehr als zwei Schritte laufen kann
			} else if (this.lastActionResult.equals("failed_parameter")) {
				// Fehlerbehandlung
			} else {
				// Fehlerbehandlung
			}
			break;
		case "attach":
			switch (lastActionResult) {
			case "success":
				String direction = (String) this.lastActionParams.get(0);
				RelativeCoordinate pos;
				Cell cell;
				switch (direction) {
				case "n":
					pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() + 1);
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "s":
					pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() - 1);
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "e":
					pos = new RelativeCoordinate(this.currentPos.getX() + 1, this.currentPos.getY());
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "w":
					pos = new RelativeCoordinate(this.currentPos.getX() - 1, this.currentPos.getY());
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				}
			case "failed_parameter":
				break;
			case "failed_target":
				break;
			case "failed_blocked":
				break;
			case "failed":
				break;
			default:
				break;
			}
			break;
		case "detach":
			switch (lastActionResult) {
			case "success":
				String direction = (String) this.lastActionParams.get(0);
				RelativeCoordinate pos;
				switch (direction) {
				case "n":
					pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() - 1);
					this.attachedBlocks.remove(pos);
					break;
				case "s":
					pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() + 1);
					this.attachedBlocks.remove(pos);
					break;
				case "e":
					pos = new RelativeCoordinate(this.currentPos.getX() + 1, this.currentPos.getY());
					this.attachedBlocks.remove(pos);
					break;
				case "w":
					pos = new RelativeCoordinate(this.currentPos.getX() - 1, this.currentPos.getY());
					this.attachedBlocks.remove(pos);
					break;
				default:
					break;
				}
				break;
			case "rotate":
				switch (lastActionResult) {
				case "success":
					String rot = (String) this.lastActionParams.get(0);
					HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
					if (rot.equals("cw")) {
						Orientation.changeOrientation(orientation, true);
						for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
							Cell cell = this.attachedBlocksWithPositions.get(key);
							temp.put(new RelativeCoordinate(key.getY(), -key.getX()), cell);
						}
					} else {
						Orientation.changeOrientation(orientation, false);
						for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
							Cell cell = this.attachedBlocksWithPositions.get(key);
							temp.put(new RelativeCoordinate(-key.getY(), key.getX()), cell);
						}
					}
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "connect":
				switch (lastActionResult) {
				case "success":
					// Behandlung
					break;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_partner":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				case "failed":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "disconnect":
				switch (lastActionResult) {
				case "success":
					int xCellA = (int) lastActionParams.get(0);
					int yCellA = (int) lastActionParams.get(1);
					int xCellB = (int) lastActionParams.get(2);
					int yCellB = (int) lastActionParams.get(3);
					// Bestimmung, ob Blöcke aus der attached-Liste entfernt werden müssen
					break;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "request":
				break;
			case "submit":
				break;
			case "clear":
				switch (lastActionResult) {
				case "success":
					int xCoordinate = (int) lastActionParams.get(0);
					int yCoordinate = (int) lastActionParams.get(1);
					mapManager.removeObstacle(new RelativeCoordinate(xCoordinate, yCoordinate));
					break;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				case "failed_resources":
					// Fehlerbehandlung
					break;
				case "failed_location":
					// Fehlerbehandlung
					break;
				case "failed_random":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "adopt":
				switch (lastActionResult) {
				case "success":
					String role = (String) lastActionParams.get(0);
					roleName = role;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_location":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "survey":
				break;
			default:
				break;
			}
		}

	}

	private void setCurrentPosition(RelativeCoordinate relativeCoordinate) {
		this.currentPos = relativeCoordinate;
	}

	private Action handleError() {
		if (lastAction.equals("move") && !lastActionResult.equals("success") && lastActionParams.size() == 1) {
			// Get direction
			String direction = (String) lastActionParams.get(0);
			say("I got stuck when trying to walk '" + direction + "'");

			RelativeCoordinate desiredField = RelativeCoordinate.getRelativeCoordinate(direction);
			for (RelativeCoordinate relativeCoordinate : occupiedFields) {
				if (relativeCoordinate.equals(desiredField)) {
					say("Reason: field towards direction '" + direction + "' is already occupied");
					// This will ensure that agent will try all possible directions if one step
					// after another fails due to occupied fields
					switch (direction) {
					case "n":
						return move("e");
					case "e":
						return move("s");
					case "s":
						return move("w");
					case "w":
						return move("n");
					}
				}
			}
		}
		if (lastAction.equals("attach") && !lastActionResult.equals("success")) {
			String direction = (String) lastActionParams.get(0);
			say("Last attempt to attach failed. Trying to attach...");
			return new Action("attach", new Identifier(direction));
		}
		if (lastAction.equals("rotate") && !lastActionResult.equals("success")) {
			say("Rotation was not succesful.");
			return moveRandomly(1);

		}
		if (lastAction.equals("clear") && !lastActionResult.equals("success")) {
			say("Last attempt to clear failed.");
			return new Action("skip"); // TODO: improve
		}
		// TODO: expand error handling
		return moveRandomly(currentRole.getSpeedWithoutAttachments());
	}

	private Action workerActionAttach() {
		String direction = (String) lastActionParams.get(0);
		say("Block had been successfully requested. Trying to attach...");
		return new Action("attach", new Identifier(direction));
	}

	private Action workerActionDetach() {
		say("Block attached, but no corresponding task(s).");
		say("Detaching from block...");
		this.setCurrentTask(null);
		return new Action("detach", new Identifier(attachedBlocks.get(0).getDirectDirection()));
	}

	private Action workerActionSearchGoalzone(Task taskToFinish) {
		say("Need to look for goal zone");
		// Identify goal zone field candidates (= goal zone fields which are not
		// occupied and which have enough space around them to submit a task)
		Set<RelativeCoordinate> goalZoneFieldCandidates = pathCalc.determineGoalZoneFieldCandidates(taskToFinish);

		if (!goalZoneFieldCandidates.isEmpty()) {
			say("Suitable goal zone fields identified");
			// Check if agent already on a suitable goal zone field
			if (!goalZoneFieldCandidates.contains(mapManager.getCurrentPosition())) {
				// Calculate direction agent should move into in order to get as fast as
				// possible to the next suitable goal zone field
				String dir = pathCalc.calculateShortestPathMap(goalZoneFieldCandidates);
				if (dir == null) {
					say("No path towards identified goal zone fields.");
					return explorerStep();
				} else {
					say("Path identified. Moving towards next suitable goal zone field...");
					return move(dir);
				}
			} else {
				say("Already on suitable goal zone field");
				if (!checkIfTaskComplete(taskToFinish)) {
					// At the moment only for one-block tasks
					return executeRotation(attachedBlocks.get(0).getRelativeCoordinate(),
							taskToFinish.getRequirements().get(0).getRelativeCoordinate());
				}
				say("Task '" + taskToFinish.getName() + "' is complete");
				this.setCurrentTask(null);
				return submit(taskToFinish);
			}
		}
		// Explore to find a suitable goal zone field
		return explorerStep();
	}

	/**
	 * editor: michael
	 * 
	 * decides: worker without block -> searches dispenser worker with set task
	 * aborts task if no time left / decides if it is multi- or oneblocktask worker
	 * without a task sets the fastest task fitting his block(s)
	 *
	 * @return
	 */
	private Action workerActionHandleBlock() {
		
		// maybe first check if working on task, if not choose task and work on it
		if (this.attachedBlocks.size() == 0) {
			return this.workerActionSearchDispenser();
		}

		// worker is working on a task
		if (workerIsWorkingOnTask()) {
			// check if task is still available or achievable
			if (this.currentStep > this.getCurrentTask().getDeadline()) {
				workerAbortTask();
				say("aborted my task!");
			}
			// handle single or multiBlockTask
			else if (this.getCurrentTask().isOneBlockTask()) {
				return this.workerActionHandleOneBlockTask();
			} else if (this.getCurrentTask().isMultiBlockTask()) {
				return this.workerActionHandleMultiBlockTask();
			} else {
				say("cannot work on my task!");
			}
		}
		// check if no tasks with current block available
		List<Task> correspondingTasks = determineCorrespondingTasks();
		if (correspondingTasks.isEmpty()) {
			return this.workerActionDetach();
		}
		// not on task and chooses task
		setCurrentTask(determineCurrentTask(correspondingTasks));
		say("my new task is: "+ this.getCurrentTask().getName());

		if (this.getCurrentTask() != null) {
			if (this.getCurrentTask().isOneBlockTask()) {
				return this.workerActionHandleOneBlockTask();
			}
			if (this.getCurrentTask().isMultiBlockTask()) {
				return this.workerActionHandleMultiBlockTask();
			}
		}
		return this.workerActionSearchDispenser();
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private Action workerActionHandleMultiBlockTask() {
		if (gotAllBlocks(currentTask)) {
			// chooses or searches goalzone
			return this.workerActionSearchGoalzone(currentTask);
		}
		
		
		// worker needs blocks and should search for dispensers
		// TODO
		return this.workerActionSearchDispenser();
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private Action workerActionHandleOneBlockTask() {
		return this.workerActionSearchGoalzone(currentTask);
	}

	/**
	 * editor: michael
	 *
	 */
	private void workerAbortTask() {
		this.setCurrentTask(null);
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private boolean workerIsWorkingOnTask() {
		if (this.getCurrentTask() == null) {
			return false;
		}
		return true;
	}

	/**
	 * editor: michael
	 *
	 * @param newTask
	 */
	private void setCurrentTask(Task newTask) {
		this.currentTask = newTask;
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private Task getCurrentTask() {
		return this.currentTask;
	}

	/**
	 * editor: michael
	 * 
	 * decides which task to fulfill returns the fastest to complete task
	 * 
	 * todo: fastest task 
	 *
	 * @return
	 */
	private Task determineCurrentTask(List<Task> correspondingTasks) {
		// check if agent has all blocks for a specific task
		for (Task task : tasks) {
			if (gotAllBlocks(task))
				return task;
		}
		// find fastest task
		Task fastestTask = null;
		
		//Task fastestTask = findFastestTask();
		
		int blocksMissingForTask = 10;
		for (Task task : tasks) {
			if (this.numberOfBlocksMissingForTask(task) < blocksMissingForTask) {
				fastestTask = task;
				blocksMissingForTask = this.numberOfBlocksMissingForTask(task);
			}

		}
		return fastestTask;
	}

	/**
	 * editor: michael
	 *
	 * finds best task, to complete in shortest time
	 *
	 * @return
	 */
	private Task findFastestTask() {
		// TODO Auto-generated method stub
		// distance to dispenser, dispenser -> goalzone, end of task
				
		return null;
	}
	
	/**
	 * editor: michael
	 *
	 * finds best task, to get most points in shortest time
	 *
	 * @return
	 */
	private Task findMostEfficientTask() {
		// TODO Auto-generated method stub
		// distance to dispenser, dispenser -> goalzone, end of task -> how often could it be completed -> possible reward
				
		return null;
	}

	/**
	 * editor: michael
	 * 
	 * creates HashMap to connect blockName and amount needed for task sets the
	 * field attachedBlockTypeMap
	 *
	 * @return
	 */
	private void setAttachedBlockTypeMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("b0", 0);
		map.put("b1", 0);
		map.put("b2", 0);
		map.put("b3", 0);

		for (int i = 0; i < this.attachedBlocks.size(); i++) {
			switch ((String) this.attachedBlocks.get(i).getType()) {
			case "b0":
				map.put("b0", map.get("b0") + 1);
				break;
			case "b1":
				map.put("b1", map.get("b1") + 1);
				break;
			case "b2":
				map.put("b2", map.get("b2") + 1);
				break;
			case "b3":
				map.put("b3", map.get("b3") + 1);
				break;
			default:
				System.out.println("createBlockTypeMap cannot handle: " + this.attachedBlocks.get(i).getType() + "!");
				break;
			}
		}
		this.attachedBlockTypeMap = map;
	}

	/**
	 * editor: michael
	 * 
	 * returns true if agent has all blocks to fulfill current Task
	 *
	 * @return
	 */
	private boolean gotAllBlocks(Task task) {
		int b0task = task.getBlockTypeMap().get("b0");
		int b1task = task.getBlockTypeMap().get("b1");
		int b2task = task.getBlockTypeMap().get("b2");
		int b3task = task.getBlockTypeMap().get("b3");
		int b0attached = this.getAttachedBlockTypeMap().get("b0");
		int b1attached = this.getAttachedBlockTypeMap().get("b1");
		int b2attached = this.getAttachedBlockTypeMap().get("b2");
		int b3attached = this.getAttachedBlockTypeMap().get("b3");

		if ((b0task == b0attached) && (b1task == b1attached) && (b2task == b2attached) && (b3task == b3attached)) {
			return true;
		}

		return false;
	}

	private int numberOfBlocksMissingForTask(Task task) {
		int b0task = task.getBlockTypeMap().get("b0");
		int b1task = task.getBlockTypeMap().get("b1");
		int b2task = task.getBlockTypeMap().get("b2");
		int b3task = task.getBlockTypeMap().get("b3");
		int b0attached = this.getAttachedBlockTypeMap().get("b0");
		int b1attached = this.getAttachedBlockTypeMap().get("b1");
		int b2attached = this.getAttachedBlockTypeMap().get("b2");
		int b3attached = this.getAttachedBlockTypeMap().get("b3");

		int result = b0task + b1task + b2task + b3task - (b0attached + b1attached + b2attached + b3attached);

		return result;
	}
	
	private HashMap<String, Integer> missingBlocksForTaskHash(){
		if (this.attachedBlocks.isEmpty()) {
			return this.currentTask.getBlockTypeMap();
		}
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put("b0", this.getCurrentTask().getBlockTypeMap().get("b0") - this.getAttachedBlockTypeMap().get("b0"));
		map.put("b1", this.getCurrentTask().getBlockTypeMap().get("b1") - this.getAttachedBlockTypeMap().get("b1"));
		map.put("b2", this.getCurrentTask().getBlockTypeMap().get("b2") - this.getAttachedBlockTypeMap().get("b2"));
		map.put("b3", this.getCurrentTask().getBlockTypeMap().get("b3") - this.getAttachedBlockTypeMap().get("b3"));
				
		return map;
	}
	
	private List<String> missingBlockTypesList(){
		List<String> list = new ArrayList<>();
		
		if (this.missingBlocksForTaskHash().get("b0")>0) list.add("b0");
		if (this.missingBlocksForTaskHash().get("b1")>0) list.add("b1");
		if (this.missingBlocksForTaskHash().get("b2")>0) list.add("b2");
		if (this.missingBlocksForTaskHash().get("b3")>0) list.add("b3");
		
		return list;
	}

	private HashMap<String, Integer> getAttachedBlockTypeMap() {
		return this.attachedBlockTypeMap;
	}
	
	private HashMap<String, Integer> nextDispenserTypeHashMap(){
		// todo get distance from pathcalc
		int b0distance = 0;
		int b1distance = 0;
		int b2distance = 0;
		int b3distance = 0;
		
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		
		map.put("b0", b0distance);
		map.put("b1", b1distance);
		map.put("b2", b2distance);
		map.put("b3", b3distance);
		return map;
	}
	
	private List<String> nextDispenserTypeList(){		
		return sortHashMapKeysToList(nextDispenserTypeHashMap());
	}
	
	private List<String> sortHashMapKeysToList(HashMap<String, Integer> nextDispenserTypeHashMap) {
		List<String> list = new ArrayList<>();
		
		for (Map.Entry<String, Integer> set : nextDispenserTypeHashMap.entrySet()) {
			System.out.println(set.getKey());
			if (list.isEmpty()) {
				System.out.println("list empty");
				list.add(set.getKey());
				System.out.println("add "+set.getKey()+" to empty list");
			}
			for (int i = 0; i < list.size(); i++) {
				if (list.contains(set.getKey())) break;
				if (nextDispenserTypeHashMap.get(list.get(i)) >= set.getValue()) {
					list.add(i, set.getKey());
					System.out.println("add "+set.getKey()+" to "+ i);
					break;
				}
			}
			if (!list.contains(set.getKey())) list.add(set.getKey());
		}
		return list;
	}
	
	// todo
	private Dispenser getNextDispenser(String type) {
		
		
		return null;
	}

	/**
	 * editor: michael
	 * 
	 * if worker knows of dispensers fitting to the required blocktypes they will
	 * go to the closest
	 * 
	 * @return go to dispenser or explorerstep
	 */
	private Action workerActionSearchDispenser() {
		Dispenser disp = null;
		
		if (!this.nextDispenserTypeList().isEmpty()) {
			for (String dispensertype : this.nextDispenserTypeList()) {
				if (this.missingBlockTypesList().contains(dispensertype)) {
					disp = getNextDispenser(dispensertype);
					say("Suitable dispenser(s) identified");
					break;
					// eventuell String disp?
				}
			}
		}
		
		if (disp == null) {
			return explorerStep();
		}
		
		return this.goToDispenser(disp);
	}

	/**
	 * editor: michael
	 *
	 * goes to certain dispenser if there is a path
	 *
	 * @param disp
	 * @return move (dir) to next dispenser or explorerstep
	 */
	private Action goToDispenser(Dispenser disp) {
		// agent is next to Dispenser
		if (disp.getRelativeCoordinate().isNextToAgent(mapManager.getCurrentPosition())) {
			String direction = disp.getRelativeCoordinate().getDirectDirection(mapManager.getCurrentPosition());
			return requestBlock(direction);
		}
		// If agent is on top of dispenser -> move one step to be able to request a block
		if (disp.getRelativeCoordinate().getX() == mapManager.getCurrentPosition().getX() 
				&& disp.getRelativeCoordinate().getY() == mapManager.getCurrentPosition().getY()) {
			say("I am on a dispenser. Stepping aside.");
			return moveRandomly(1); // TODO: check for obstacles or blocks
		}
		// Move towards dispenser
		String dir = pathCalc.calculateShortestPathMap(disp.getRelativeCoordinate());
		if (dir == null) {
			say("No path towards dispenser.");
			return explorerStep();
		} else {
			say("Path identified. Moving towards dispenser...");
			return move(dir);
		}
	}

	// default (main) worker method
	private Action workerStep() {
		// If a block has been requested in the last step, then attach this block
		if (lastAction.equals("request") && lastActionResult.equals("success")) {
			return this.workerActionAttach();
		}
		// This only works for tasks with one block
		// If the agent has a block attached, then either detach from it (if no
		// corresponding task), look for goal zone, rotate or submit
		if (!attachedBlocks.isEmpty()) {
			return this.workerActionHandleBlock();
		}
		// no block requested in last step or currently attached
		return this.workerActionSearchDispenser();
	}

	private Action borderExplorerStep() {
		say("I was next to obstacle last step: " + wasNextToObstacleLastStep);
		if (nextToObstacle()) {
			say("IAM NEXTTO BORDER");
			wasNextToObstacleLastStep = true;
			dirToFindBorder = ""; // resets dir to find border

			say("walking alongside Border");
//			try {
//				Thread.sleep(4000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return walkAlongsideBorder();

		} else if (wasNextToObstacleLastStep) {
			wasNextToObstacleLastStep = false;
			return walkAlongsideBorder();
		}
//		else if (obstaclesInSight.size() > 0) {
//			say("Walking to obstacle");
//			try {
//				Thread.sleep(4000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return walkToObstacle();
//		} 
		else if (dirToFindBorder.equals("")) {
			ArrayList<String> possibleDirs = getPossibleDirs();
			Random rand = new Random();
			dirToFindBorder = possibleDirs.get(rand.nextInt(possibleDirs.size()));
			say("Set new Dir to find border + move there");
//			try {
//				Thread.sleep(4000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return new Action("move", new Identifier(dirToFindBorder));
		} else {
			say("dir to find Border was set, moving there");
//			try {
//				Thread.sleep(4000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return move(dirToFindBorder);
		}
	}

	private boolean nextToObstacle() {
		for (RelativeCoordinate obstacle : obstaclesInSight) {
			if (obstacle.isNextToAgent()) {
				return true;
			}
		}
		return false;
	}


	private Action walkToObstacle() {
		if (dirOfBorders.size() == 0) {
			String dir = pathCalc.calculateShortestPathVision(currentRole.getVision(), occupiedFields,
					new HashSet(obstaclesInSight));
			return move(dir);
		} else {
			if (dirOfFocusBorder.equals("")) {
				Random rand = new Random();
				dirOfFocusBorder = dirOfBorders.get(rand.nextInt(dirOfBorders.size()));
			}
			return move(dirOfFocusBorder);
		}
	}

	private Action walkAlongsideBorder() {
		findBorders();
		say("Borders: " + dirOfBorders);
		if (dirOfBorders.size() == 0) {
			if (comesFromDeadEnd && !moveDir3StepsAgo.equals("")) {
				return move(moveDir3StepsAgo);
			}
			comesFromDeadEnd = false;
			return move(dirOfFocusBorder);
		}
		if (dirOfBorders.size() == 1) {
			dirOfFocusBorder = dirOfBorders.get(0);
			comesFromDeadEnd = false;
		}
		if (dirOfBorders.size() > 1) { // TODO: Fallunterscheidung Borders n+w, s+w, etc..
			say("More than 1 Border");
			if (dirOfBorders.size() == 2) {
				say("LAST " + lastMoveDir + " != FOCUS " + dirOfFocusBorder + " : "
						+ (!lastMoveDir.equals(dirOfFocusBorder)));
				int newBorder = dirOfBorders.indexOf(dirOfFocusBorder) == 0 ? 1 : 0;
				if (dirOfBorders.get(newBorder) != getOppositeDirection(dirOfFocusBorder)) { // Other Border is adjacent
																								// to current Border
					if (!lastMoveDir.equals(dirOfFocusBorder)) { // makes sure agent doesn't walk back and forth
						dirOfFocusBorder = dirOfBorders.get(newBorder);
					}
				}
				comesFromDeadEnd = false;
			}
			if (dirOfBorders.size() == 3) {
				say("!!!!!!!!!!!!!!!!!!!!!3 BORDERS!!!!!!!!!!!!!!1");
				boolean containsOppositeToCurrentFocusBorderDir = false;
				String opposite = getOppositeDirection(dirOfFocusBorder);
				ArrayList<String> notInFocusDir = new ArrayList<>();
//				String notInFocusDir = "";
				for (String border : dirOfBorders) {
					if (border.equals(opposite)) {
						containsOppositeToCurrentFocusBorderDir = true;
					}
					if (!border.equals(dirOfFocusBorder)) {
						notInFocusDir.add(border);
//						notInFocusDir = border;
					}
				}
				if (containsOppositeToCurrentFocusBorderDir) {
					dirOfFocusBorder = opposite;
				} else { // focus border is exactly the dead end
					Random rand = new Random();
					dirOfFocusBorder = notInFocusDir.get(rand.nextInt(notInFocusDir.size()));
//					dirOfFocusBorder = notInFocusDir;
				}
				comesFromDeadEnd = true;
			}

		}
		say("dir of Focus " + dirOfFocusBorder);
		ArrayList<String> possibleDirs = getAllPossibleDirs();
		if (possibleDirs.contains(lastMoveDir)) {
			return move(lastMoveDir);
		}
		if (possibleDirs.size() > 1) {
			possibleDirs.remove(getOppositeDirection(lastMoveDir));
		}
		if (possibleDirs.size() > 1) {
			possibleDirs.remove(getOppositeDirection(dirOfFocusBorder));
		}

		return moveRandomly(1, possibleDirs); // TODO: adjust steplength
	}

	private ArrayList<String> getAllPossibleDirs() {
		ArrayList<String> possibleDirs = new ArrayList<String>();
		boolean blockedWayNorth = false;
		boolean blockedWayEast = false;
		boolean blockedWaySouth = false;
		boolean blockedWayWest = false;
		for (RelativeCoordinate pos : occupiedFieldsWithoutBlocks) {
			String dir = pos.getDirectDirection();

			switch (dir) {
			case ("n"): {
				if (pos.distanceFromAgent() == 1) {
					blockedWayNorth = true;
				}
				break;
			}
			case ("e"): {
				if (pos.distanceFromAgent() == 1) {
					blockedWayEast = true;
				}
				break;
			}
			case ("s"): {
				if (pos.distanceFromAgent() == 1) {
					blockedWaySouth = true;
				}
				break;
			}
			case ("w"): {
				if (pos.distanceFromAgent() == 1) {
					blockedWayWest = true;
				}
				break;
			}

			}
		}
		if (!blockedWayNorth) {
			possibleDirs.add("n");
		}
		if (!blockedWayEast) {
			possibleDirs.add("e");
		}
		if (!blockedWaySouth) {
			possibleDirs.add("s");
		}
		if (!blockedWayWest) {
			possibleDirs.add("w");
		}
		say("get all possibleDirs: " + possibleDirs);
		return possibleDirs;
	}

	private void findBorders() {
		dirOfBorders.clear();
		for (RelativeCoordinate obstacle : obstaclesInSight) {
			if (obstacle.getX() == 0) {
				if (obstacle.getY() == 1) {
					dirOfBorders.add("s");
				}
				if (obstacle.getY() == -1) {
					dirOfBorders.add("n");
				}
			}
			if (obstacle.getY() == 0) {
				if (obstacle.getX() == 1) {
					dirOfBorders.add("e");
				}
				if (obstacle.getX() == -1) {
					dirOfBorders.add("w");
				}
			}
		}
	}

	private Action explorerStep() {
	// falls mindestens ein Teammitglied sichtbar, wird dies nach seinem Namen
		// befragt, um einen map-Austausch einzuleiten
		// TODO: Bedingung sollte weiter eingeschränkt, weil sonst nur surveyed und
		// nicht explort wird

		say("Friendly Agents: " + friendlyAgents.size() + " und Counter: " + counterMapExchange);

		if (!this.friendlyAgents.isEmpty() && counterMapExchange > 7) {
			counterMapExchange = 0;
			Iterator<RelativeCoordinate> it = this.friendlyAgents.iterator();
			while (it.hasNext()) {
				RelativeCoordinate relCo = it.next();
				int x = this.currentPos.getX() + relCo.getX();
				int y = this.currentPos.getY() + relCo.getY();
				RelativeCoordinate pos = new RelativeCoordinate(x, y);
				if ((Math.abs(x) + Math.abs(y) > this.currentRole.getVision() - 1)
						|| (x == currentPos.getX() && y == currentPos.getY())) {
					it.remove();
				}
			}
			if (!this.friendlyAgents.isEmpty()) {
				say("I try to exchange my map");
				this.mapManager.createExchangePartner(
						new RelativeCoordinate(this.currentPos.getX() + this.friendlyAgents.get(0).getX(),
								this.currentPos.getY() + this.friendlyAgents.get(0).getY()));
				return new Action("survey", new Numeral(this.friendlyAgents.get(0).getX()),
						new Numeral(this.friendlyAgents.get(0).getY()));
			}
		}
		counterMapExchange = counterMapExchange + 1;
		ArrayList<String> possibleDirs = getAllPossibleDirs();
		String prefDir = getPreferredDir();
		if (possibleDirs != null && possibleDirs.size() != 0) {
			if (!prefDir.equals("")) {
				if (possibleDirs.contains(prefDir)) {
//					if(!(attachedBlocks.size() == 1)) {
//						ArrayList<String> attachedBlocksDirs = getAttachedBlocksDirs();
//						return rotateAccordingToAttachedBlock(prefDir, attachedBlocksDirs);
//					}
					return move(prefDir);
				} else {
//					if (attachedBlocks.size() ==0) {
					return clear(prefDir);
				}
//				}

			}
			// What to do when there is no preferred Direction:
			if (possibleDirs.size() > 1) { // remove Dir where you came from
				if (lastMoveDir != null) {
					say("removing Dir where I came from: " + getOppositeDirection(lastMoveDir));
					possibleDirs.remove(getOppositeDirection(lastMoveDir));
				}
			}
			if (possibleDirs.size() > 1) {
				say("Removing opposite dir from pref: " + getOppositeDirection(prefDir));
				possibleDirs.remove(getOppositeDirection(prefDir));
			}
			say("possible Dirs after: " + possibleDirs);
			return moveRandomly(1, possibleDirs); // TODO adjust 1 to current step length

		}
		return new Action("skip");
	}



//	private ArrayList<String> getAttachedBlocksDirs() {
//		ArrayList<String> attachedBlocksDirs = new ArrayList<>();
//		for(Block b : attachedBlocks) {
//			if(b.distanceFromAgent()==1) {
//				attachedBlocksDirs.add(b.getDirectDirection());
//			}
//		}
//		return attachedBlocksDirs;
//	}

	private Action clear(String prefDir) {
		say("clearing " + prefDir);
		int x = 0;
		int y = 0;
		switch (prefDir) {
		case ("n"): {
			x = 0;
			y = -1;
			break;
		}
		case ("e"): {
			x = 1;
			y = 0;
			break;
		}
		case ("s"): {
			x = 0;
			y = 1;
			break;
		}
		case ("w"): {
			x = -1;
			y = 0;
			break;
		}
//		default: {
//			return new Action("skip");
//		}
		}
		return new Action("clear", new Numeral(x), new Numeral(y));
	}

	private ArrayList<String> getPossibleDirs() {
		ArrayList<String> possibleDirs = new ArrayList<String>();
		boolean blockedWayNorth = false;
		boolean blockedWayEast = false;
		boolean blockedWaySouth = false;
		boolean blockedWayWest = false;
		int minDistance = 1000; // TODO set higher than possible sight
		for (RelativeCoordinate pos : occupiedFields) {
			int distance = pos.distanceFromAgent();
			if (distance < minDistance) {
				minDistance = distance;
			}
			String dir = pos.getDirectDirection();
			switch (dir) {
			case ("n"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayNorth = true;
				}
				break;
			}
			case ("e"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayEast = true;
				}
				break;
			}
			case ("s"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWaySouth = true;
				}
				break;
			}
			case ("w"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayWest = true;
				}
				break;
			}
			}
		}
		if (!blockedWayNorth) {
			possibleDirs.add("n");
		}
		if (!blockedWayEast) {
			possibleDirs.add("e");
		}
		if (!blockedWaySouth) {
			possibleDirs.add("s");
		}
		if (!blockedWayWest) {
			possibleDirs.add("w");
		}

		return possibleDirs;
	}

	private String getPreferredDir() {

		if (preferredDir.equals("") || preferredDirTimer < 1) {

			String newDir = getRandomDir();
			while (newDir.equals(getOppositeDirection(preferredDir))) {
				newDir = getRandomDir();
			}
			preferredDir = newDir;
			if (preferredDirTimer < 1) {
				int min = simSteps / 40; // TODO: @Carina Zahlen anpassen
				int max = simSteps / 20;
				Random rand = new Random();
				preferredDirTimer = rand.nextInt(max - min + 1) + min;
			}
		} else {
			preferredDirTimer--;
		}
		say("get Preferred Dir: " + preferredDir);
		return preferredDir;
	}

	private String getRandomDir() {
		Random rand = new Random();
		String[] dirs = new String[] { "n", "e", "s", "w" };
		return dirs[rand.nextInt(dirs.length)];
	}

	/**
	 * Makes the agent request a block from a dispenser
	 * 
	 * @param direction Direction of the dispenser
	 * @return The request action
	 */
	private Action requestBlock(String direction) {
		say("Requesting block...");
		return new Action("request", new Identifier(direction));
	}

	/**
	 * Executes a rotation action for bringing the attached block to the correct
	 * position (if possible, otherwise skip)
	 * 
	 * @param direction      The coordinate of the attached block
	 * @param targetBlockPos The coordinate the attached block needs to be rotated
	 *                       to
	 * @return Rotation action (if possible, otherwise skip Action)
	 */
	// Works only when agent has one block attached
	private Action executeRotation(RelativeCoordinate currentBlockPos, RelativeCoordinate targetBlockPos) {
		if (currentBlockPos.equals(targetBlockPos)) {
			return new Action("skip");
		}
		if ((currentBlockPos.isOneStepNorth() && targetBlockPos.isOneStepEast())
				|| (currentBlockPos.isOneStepEast() && targetBlockPos.isOneStepSouth())
				|| (currentBlockPos.isOneStepSouth() && targetBlockPos.isOneStepWest())
				|| (currentBlockPos.isOneStepWest() && targetBlockPos.isOneStepNorth())) {
			return rotate("cw");
		}
		if ((currentBlockPos.isOneStepNorth() && targetBlockPos.isOneStepWest())
				|| (currentBlockPos.isOneStepEast() && targetBlockPos.isOneStepNorth())
				|| (currentBlockPos.isOneStepSouth() && targetBlockPos.isOneStepEast())
				|| (currentBlockPos.isOneStepWest() && targetBlockPos.isOneStepSouth())) {
			return rotate("ccw");
		}
		if ((currentBlockPos.isOneStepNorth() && targetBlockPos.isOneStepSouth())) {
			if (!occupiedFields.contains(new RelativeCoordinate(1, 0))) {
				return rotate("cw");
			}
			if (!occupiedFields.contains(new RelativeCoordinate(-1, 0))) {
				return rotate("ccw");
			}
		}
		if ((currentBlockPos.isOneStepEast() && targetBlockPos.isOneStepWest())) {
			if (!occupiedFields.contains(new RelativeCoordinate(0, 1))) {
				return rotate("cw");
			}
			if (!occupiedFields.contains(new RelativeCoordinate(0, -1))) {
				return rotate("ccw");
			}
		}
		if ((currentBlockPos.isOneStepSouth() && targetBlockPos.isOneStepNorth())) {
			if (!occupiedFields.contains(new RelativeCoordinate(-1, 0))) {
				return rotate("cw");
			}
			if (!occupiedFields.contains(new RelativeCoordinate(1, 0))) {
				return rotate("ccw");
			}
		}
		if ((currentBlockPos.isOneStepWest() && targetBlockPos.isOneStepEast())) {
			if (!occupiedFields.contains(new RelativeCoordinate(0, -1))) {
				return rotate("cw");
			}
			if (!occupiedFields.contains(new RelativeCoordinate(0, 1))) {
				return rotate("ccw");
			}
		}
		return new Action("skip");
	}

	/**
	 * Moves the agent in a given direction
	 * 
	 * @param dir The direction to move
	 * @return The move action
	 */
	private Action move(String dir) {
		say("moving " + dir);
		return new Action("move", new Identifier(dir));
	}

	/**
	 * Moves the agent randomly in any directions
	 * 
	 * @param stepNum The number of steps the agent should move
	 * @return The move action
	 */
	private Action moveRandomly(int stepNum) {
		List<String> allowedDirections = new ArrayList<String>(Arrays.asList("n", "e", "s", "w"));
		return moveRandomly(stepNum, allowedDirections);
	}

	/**
	 * Moves the agent randomly in the allowed directions
	 * 
	 * @param stepNum           The number of steps the agent should move.
	 * @param allowedDirections The list of allowed directions
	 * @return The move action
	 */
	private Action moveRandomly(int stepNum, List<String> allowedDirections) {
		if (allowedDirections == null || allowedDirections.isEmpty()) {
			return new Action("skip");
		}
		Random rand = new Random();
		List<String> randomDirections = new ArrayList<>();
		for (int i = 1; i <= stepNum; i++) {
			String randomDirection = allowedDirections.get(rand.nextInt(allowedDirections.size()));
			randomDirections.add(randomDirection);
		}

		switch (stepNum) {
		case 1 -> {
			String direction = randomDirections.get(0);
			say("Moving one step randomly...");
			return move(direction);
		}
		case 2 -> {
			String direction1 = randomDirections.get(0);
			String direction2 = randomDirections.get(1);
			if (oppositeDirections(direction1, direction2)) {
				allowedDirections.remove(direction2);
				direction2 = allowedDirections.get(rand.nextInt(allowedDirections.size()));
			}
			say("Moving two steps randomly...");
			return new Action("move", new Identifier(direction1), new Identifier(direction2));
		}
		default -> {
			return new Action("skip");
		}
		}
	}

	/**
	 * Submits the given task
	 * 
	 * @param task The task to be submitted
	 * @return The submit action
	 */
	private Action submit(Task task) {
		say("Submitting task '" + task.getName() + "'...");
		return new Action("submit", new Identifier(task.getName()));
	}

	/**
	 * Rotates agent in given direction
	 * 
	 * @param dir The direction of rotation
	 * @return The rotate action
	 */
	private Action rotate(String dir) {
		say("Rotating in " + dir + " direction...");
		return new Action("rotate", new Identifier(dir));
	}

	/**
	 * Checks if the task is complete (all blocks attached and on the correct
	 * position)
	 * 
	 * @param task The task to be checked against
	 * @return True if task is complete, otherwise false
	 */
	private boolean checkIfTaskComplete(Task task) {
		// Check if all required blocks are attached
		List<TaskRequirement> requirements = task.getRequirements();
		for (TaskRequirement requirement : requirements) {
			boolean requirementFulfilled = false;
			for (Block attachedBlock : attachedBlocks) {
				if (requirement.isFulfilledBy(attachedBlock)) {
					requirementFulfilled = true;
					break;
				}
			}
			if (requirementFulfilled == false) {
				return false;
			}
		}

		// Check if all attached blocks are really needed
		for (Block attachedBlock : attachedBlocks) {
			boolean blockIsRequired = false;
			for (TaskRequirement requirement : requirements) {
				if (requirement.isFulfilledBy(attachedBlock)) {
					blockIsRequired = true;
					break;
				}
			}
			if (blockIsRequired == false) {
				return false;
			}
		}
		return true;
	}

	private boolean agentIsLocked() {
		// TODO
		return false;
	}

	/**
	 * Analyzes the currently attached blocks and determines tasks which require
	 * these blocks
	 * 
	 * @return A list of the tasks which require the currently attached blocks todo
	 */
	private List<Task> determineCorrespondingTasks() {
		System.out.println("determineCorrespondingTasks:");
		List<Task> correspondingTasks = new ArrayList<>();

		// check first if there are oneBlockTasks to solve
		if (attachedBlocks.size() == 1) {
			for (Task task : tasks) {
				if (task.getRequirements().get(0).getBlockType().equals(attachedBlocks.get(0).getType())
						&& task.isOneBlockTask()) {
					correspondingTasks.add(task);
				}
			}
		}
		// check if any attached block fits to a multiBlockTask
		// todo
		for (Task task : tasks) {
			//System.out.println("Checking Tasks");
			for (int i = 0; i < task.getRequirements().size(); i++) {
				//System.out.println("iterating tasks");
				for (int j = 0; j < attachedBlocks.size(); j++) {
					//System.out.println("iterating attachedblocks");
					if (task.getRequirements().get(i).getBlockType().equals(attachedBlocks.get(j).getType())) {
						System.out.println("Task added: " + task.getName());
						correspondingTasks.add(task);
						break;
					}
				}
			}
		}
		return correspondingTasks;
	}

	/**
	 * Checks if two directions are opposite directions
	 * 
	 * @param direction1 First direction
	 * @param direction2 Second direction
	 * @return True if directions are opposite, otherwise false
	 */
	private boolean oppositeDirections(String direction1, String direction2) {
		if ((direction1.equals("n") && direction2.equals("s")) || (direction1.equals("s") && direction2.equals("n"))) {
			return true;
		}
		if ((direction1.equals("e") && direction2.equals("w")) || (direction1.equals("w") && direction2.equals("e"))) {
			return true;
		}
		if ((direction1.equals("cw") && direction2.equals("ccw"))
				|| (direction1.equals("ccw") && direction2.equals("cw"))) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the opposite direction
	 * 
	 * @param direction Direction for which opposite direction is required
	 * @return The opposite direction
	 */
	private String getOppositeDirection(String direction) {
		switch (direction) {
		case "n":
			return "s";
		case "e":
			return "w";
		case "s":
			return "n";
		case "w":
			return "e";
		case "cw":
			return "ccw";
		case "ccw":
			return "cw";
		default:
			return null;
		}
	}

	// Prepare for next simulation since agents are 're-used'
	private void prepareForNextSimulation() {
		// Empty/overwrite data structures which are not emptied/overwritten inside the
		// methods for saving new percepts
		// TODO: add all data structures which need to be emptied before the next
		// simulation
		currentStep = -1;
		actionID = -1;
		currentPos = new RelativeCoordinate(0, 0);
		mapManager = new MapManagement(this.currentStep, this.currentPos);
		pathCalc = new PathCalc(mapManager, attachedBlocks);
		roles.clear();
		attachedBlocks.clear();
		simStartPerceptsSaved = false;
	}

	public void deliverMap(String to, int step) {
		this.requestingExplorer = to;
		this.stepOfRequest = step;
		this.requestForMap = true;
		this.exchangeCounter = 0;
	}

	public void handleMap(String from, HashMap<RelativeCoordinate, Block> sentBlocks,
			HashMap<RelativeCoordinate, Dispenser> sentDispensers, HashMap<RelativeCoordinate, Goalzone> sentGoalzones,
			HashMap<RelativeCoordinate, Rolezone> sentRolezones, HashMap<RelativeCoordinate, Obstacle> sentObstacles,
			RelativeCoordinate sentPosition, int step) {
		this.stepOfSentMap = step;
		receivedBlocks = sentBlocks;
		receivedDispensers = sentDispensers;
		receivedGoalzones = sentGoalzones;
		receivedRolezones = sentRolezones;
		receivedObstacles = sentObstacles;
		receivedPosition = sentPosition;
	}

	public void receiveMap(MapManagement mapManager, RelativeCoordinate newPosition) {
		int xDiff = currentPos.getX() - newPosition.getX();
		int yDiff = currentPos.getY() - newPosition.getY();
		this.currentPos = newPosition;
		this.mapManager = mapManager;
		mapManager.updateLastPosition(xDiff, yDiff);
	}

	public void sendMap(String addressee, MapManagement mapManager, RelativeCoordinate addresseePosition) {
		mailbox.sendMap(addressee, mapManager, addresseePosition);
	}
}
