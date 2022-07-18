package massim.javaagents.agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import eis.iilang.Action;
import eis.iilang.Function;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import massim.javaagents.MailService;
import massim.javaagents.agents.g2pathcalc.Direction;
import massim.javaagents.agents.g2pathcalc.PathCalc;
import massim.javaagents.agents.g2utils.Block;
import massim.javaagents.agents.g2utils.Cell;
import massim.javaagents.agents.g2utils.ClearMarker;
import massim.javaagents.agents.g2utils.Dispenser;
import massim.javaagents.agents.g2utils.Entity;
import massim.javaagents.agents.g2utils.Goalzone;
import massim.javaagents.agents.g2utils.MapBundle;
import massim.javaagents.agents.g2utils.MapManagement;
import massim.javaagents.agents.g2utils.Mission;
import massim.javaagents.agents.g2utils.Norm;
import massim.javaagents.agents.g2utils.NormRequirement;
import massim.javaagents.agents.g2utils.Obstacle;
import massim.javaagents.agents.g2utils.Orientation;
import massim.javaagents.agents.g2utils.RelativeCoordinate;
import massim.javaagents.agents.g2utils.Role;
import massim.javaagents.agents.g2utils.Rolezone;
import massim.javaagents.agents.g2utils.Task;
import massim.javaagents.agents.g2utils.TaskRequirement;

public class AgentG2 extends Agent {

	private String teamName;
	private int teamSize;
	private int simSteps;
	private List<Role> roles = new ArrayList<>();
	private boolean isExplorer;
	private boolean isConstructor;
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
	private List<Parameter> params = new ArrayList<>();
	private String lastActionResult;
	private RelativeCoordinate hitFrom;
	private List<String> violations = new ArrayList<>();

	private HashMap<RelativeCoordinate, List<Cell>> tempMap = new HashMap<RelativeCoordinate, List<Cell>>();

	private List<RelativeCoordinate> attachedThings = new ArrayList<>();
	private List<Block> blocks = new ArrayList<>();
	private List<RelativeCoordinate> occupiedFields = new ArrayList<>();
	private List<Task> tasks = new ArrayList<>();
	private List<Norm> norms = new ArrayList<>();
	// Blocks that might(!) be directly attached to the agent (right next to agent)
	private List<Block> attachedBlocks = new ArrayList<>();

	private MapManagement mapManager;
	private PathCalc pathCalc;
	private int counterMapExchange = 0;
	private RelativeCoordinate exchangePartner; // speichert relative Koordinate Austauschpartners für die Map
	private boolean requestForMap = false;
	private String requestingExplorer;
	private int stepOfRequest = -3;
	private int exchangeCounter = 0;
	private ArrayList<MapBundle> mapBundleList = new ArrayList<MapBundle>();

	private HashMap<String, Role> rolesOfAgents = new HashMap<String, Role>();

	private ArrayList<RelativeCoordinate> friendlyAgents = new ArrayList<RelativeCoordinate>();
	private HashSet<String> knownAgents = new HashSet<String>();
	private HashMap<RelativeCoordinate, Cell> map = new HashMap<RelativeCoordinate, Cell>(); // see map
//	private RelativeCoordinate currentPos = new RelativeCoordinate(0, 0); // TODO delete if currentAbsolutePos works.
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

	private String[] phases = new String[] { "Search Role Zone", "Go to Role Zone", "Explore", "Go to Role Zone",
			"Work" };
	private int phase = 0;

	private HashMap<String, Integer> attachedBlockTypeMap;
	private Task currentTask;
	private String[][] assembledBlockMatrix;

	/**
	 * Constructor.
	 * 
	 * @param name    the agent's name
	 * @param mailbox the mail facility
	 */
	public AgentG2(String name, MailService mailbox) {
		super(name, mailbox);
		this.mapManager = new MapManagement(currentStep);
		this.pathCalc = new PathCalc(mapManager, attachedBlocks);
	}

	@Override
	public void handlePercept(Percept percept) {
	}

	@Override
	public void handleMessage(Percept message, String sender) {
		if (message.equals(new Percept("Revoke friendship"))) {
			knownAgents.remove(sender);
		}
		if (message.equals(new Percept("worker")) || message.equals(new Percept("constructor"))
				|| message.equals(new Percept("explorer")) || message.equals(new Percept("digger"))) {
			if (message.getParameters().size() > 0) {
				String str = ((Identifier) message.getParameters().get(0)).getValue(); // TODO @Daniel --> wenn
																						// "explorer" --> Array out of
																						// bounds Exception
				if (rolesOfAgents.containsKey(sender)) {
					rolesOfAgents.remove(sender);
				}
				rolesOfAgents.put(getName(), Role.getRole(roles, str));
			}
		}
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
			isExplorer = Mission.applyForExplorerMission(getName());
			if (!isExplorer) {
				isConstructor = Mission.applyForConstructorMission(getName());
			}
			return new Action("skip");
		}

		// must be set first, so agents knows currentStep for sorting Percepts and for
		// having a structured console output
		setCurrentStep(percepts);
		if (isExplorer) {
			say("My mission: Explorer");
		} else if (isConstructor) {
			say("My mission: Constructor");
		} else {
			say("My mission: Worker");
		}

		saveStepPercepts(percepts);

		analyzeAttachedThings();
		setAttachedBlockTypeMap();

		// Auswertung der abgespeicherten Ergebnisse der lastAction
		evaluateLastAction();
		// double lifespan = analyzeNorms();

		// Nach der Evaluation ist die currentPosition korrekt bestimmt und es können
		// die things der map hinzugefügt werden
		mapManager.updateMap(tempMap, currentRole.getVision());
		say("Map updated: version step " + currentStep);
		tempMap = new HashMap<RelativeCoordinate, List<Cell>>();

		// Übergeben der gewünschten Map
		if (requestForMap && !(knownAgents.contains(requestingExplorer))) {
			answerRequestForMap();
		} else {
			requestForMap = false;
			requestingExplorer = null;
		}

		// Zusammenführen der Maps und Übergeben der geupdateten Map
		if (exchangeCounter == 2) {
			mergeMaps();
		}

		// Fortschritt Map-Austausch verfolgen
		if (exchangeCounter > 0) {
			exchangeCounter = exchangeCounter + 1;
		}

		if ((currentStep % 15) == 0) {
			updateMapsOfKnownAgents();
		}

		if (explorerAgent.equals(getName())) {
			say("My mission: I am the explorer of the team!");

		} else {
			say("I am just a normal Worker :(");
		}
		say("phase: " + phase);
		if (currentTask != null) {
			say("!!!!!!!!!!!!!!TASK: " + currentTask.getName() + currentTask.isOneBlockTask());
		} else {
			say("NO TASK!!!!!!!!!!!100");
		}
		if (!lastActionResult.equals("success") && !lastActionResult.equals("partial_success")) {
			return handleError();
		}
		if (!mapManager.containsRolezone() && phase == 0) {
			return explorerStep();
		} else if (mapManager.containsRolezone() && phase == 0) {
			phase++;
		}
		if (!currentRole.getName().equals("explorer") && phase == 1) {
			say("PHASE 1 --> becoming explorer; current Role " + currentRole.getName());
			return searchRolezone("explorer");
		} else if (currentRole.getName().equals("explorer") && phase == 1) {
			phase++;
		}

		if (!mapManager.containsGoalzone() && phase == 2) {
			say("PHASE 2 --> exploring now!");
			return explorerStep();
		} else if (mapManager.containsGoalzone() && phase == 2) {
			phase++;
		}
		if (explorerAgent.equals(getName())) {
			say("My mission: I am the explorer of the team!");
//			if (!lastActionResult.equals("success")) {
//				return handleError();
//			}
			return explorerStep();
		} else {
			say("I am just a normal Worker :(");
		}

		// TODO: Add explorer?
		if (isConstructor) {
			if (!currentRole.getName().equals("constructor") && phase == 3) {
				return searchRolezone("constructor");
			} else if (currentRole.getName().equals("constructor") && phase == 3) {
				return constructorStep();
			}
		} else {
			if (!currentRole.getName().equals("worker") && phase == 3) {
				return searchRolezone("worker");
			} else if (currentRole.getName().equals("worker") && phase == 3) {
				return workerStep();
			}
		}
		return workerStep();
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

	private void setCurrentRole(Role role) {
		currentRole = role;
		pathCalc.setCurrentRole(role);
	}

	private void saveStepPercepts(List<Percept> percepts) {
		if (percepts == null) { // Error handling if no percepts are available
			return;
		}

		// Delete previous step percepts
		lastActionParams.clear();
		params.clear();
		attachedThings.clear();
		blocks.clear();
		friendlyAgents.clear();
		occupiedFields.clear();
		tasks.clear();
		norms.clear();
		violations.clear();
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
				String violation = ((Identifier) percept.getParameters().get(0)).getValue();
				violations.add(violation);
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
						friendlyAgents.add(relativeCoordinate);
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
					ClearMarker clearMarker = null;
					switch (info) {
					case ("ci"):
						clearMarker = new ClearMarker(relativeCoordinate, currentStep, true);
						break;
					case ("clear"):
						clearMarker = new ClearMarker(relativeCoordinate, currentStep, false);
						break;
					default:
						break;
					}
					if (!tempMap.containsKey(relativeCoordinate)) {
						ArrayList<Cell> cellList = new ArrayList<Cell>();
						cellList.add(clearMarker);
						tempMap.put(relativeCoordinate, cellList);
					} else {
						List<Cell> cellList = tempMap.get(relativeCoordinate);
						cellList.add(clearMarker);
					}
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

				// Remove if-statement once agent can handle multi-block tasks
				if (params.size() > 1) {
					say("Task " + name + " has more than one block. Ignore.");
					break;
				}

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
				NormRequirement requirement = null;
				for (Parameter param : params) {
					Parameter paramType = ((Function) param).getParameters().get(0);
					Parameter paramName = ((Function) param).getParameters().get(1);
					Parameter paramQuantity = ((Function) param).getParameters().get(2);
					Parameter paramDetails = ((Function) param).getParameters().get(3);

					String type = ((Identifier) paramType).getValue();
					String name = ((Identifier) paramName).getValue();
					int quantity = ((Numeral) paramQuantity).getValue().intValue();
					String details = ((Identifier) paramDetails).getValue();

					requirement = new NormRequirement(type, name, quantity, details);
				}
				Norm norm = new Norm(normName, firstStep, lastStep, requirement, punishment);
				norms.add(norm);
				break;
			}
			case "attached" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate attachedThing = new RelativeCoordinate(x, y);
				attachedThings.add(attachedThing);
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
						mailbox.broadcast(new Percept(roleName), getName());
						if (rolesOfAgents.containsKey(getName())) {
							rolesOfAgents.remove(getName());
						}
						rolesOfAgents.put(getName(), Role.getRole(roles, roleName));
						setCurrentRole(Role.getRole(roles, roleName));
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
					// TODO
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
				switch (direction) {
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
					Role role = new Role(roleName, roleVision, actions, speeds, clearChance, clearMaxDist,
							attachedBlocks);
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
			if (lastActionResult.equals("success")) {
				Iterator<Object> it = lastActionParams.iterator();
				boolean lastPosition = true;
				while (it.hasNext()) {
					Object temp = it.next();
					String dir = temp.toString();
					mapManager.updatePosition(dir, lastPosition);
					lastPosition = false;
				}
				// Fehlerbehandlung für "partial_success"
			} else if (lastActionResult.equals("partial_success")) {
				// erster Schritt muss gelungen sein
				String dir = (String) lastActionParams.get(0);
				mapManager.updatePosition(dir, true);
				// bei mehr als zwei Schritten muss überprüft werden, ob weitere Schritte
				// gelungen sind
				if (lastActionParams.size() > 2) {
					// Bestimmung, welche Obstacles gesehen
					ArrayList<RelativeCoordinate> obstacleList = new ArrayList<RelativeCoordinate>();
					for (RelativeCoordinate rc : tempMap.keySet()) {
						List<Cell> cellList = tempMap.get(rc);
						for (Cell cell : cellList) {
							if (cell instanceof Obstacle) {
								obstacleList.add(rc);
							}
						}
					}
					// Abgleich mit der Map für jeden Schritt
					boolean correctEnvironment = false;
					Iterator<Object> it = lastActionParams.iterator();
					while (it.hasNext() && correctEnvironment == false) {
						correctEnvironment = true;
						Object o = it.next();
						if (!(o == lastActionParams.get(0))) {
							for (RelativeCoordinate rc : obstacleList) {
								int x = mapManager.getPosition().getX();
								int y = mapManager.getPosition().getY();
								if (mapManager.getObstacleLayer()
										.get(new RelativeCoordinate(x + rc.getX(), y + rc.getY())) == null) {
									correctEnvironment = false;
									String newDir = (String) o;
									mapManager.updatePosition(newDir, false);
								}
							}
						}
					}
					// Position konnte nicht wiederhegestellt werden: Karte muss neu konstruiert
					// werden
					if (correctEnvironment == false) {
						say("I am lost!");
						for (String str : knownAgents) {
							mailbox.sendMessage(new Percept("Revoke friendship"), str, getName());
						}
						knownAgents = new HashSet<String>();
						mapManager = new MapManagement(currentStep);
					}

				}
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
					pos = new RelativeCoordinate(mapManager.getPosition().getX(), mapManager.getPosition().getY() + 1);
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "s":
					pos = new RelativeCoordinate(mapManager.getPosition().getX(), mapManager.getPosition().getY() - 1);
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "e":
					pos = new RelativeCoordinate(mapManager.getPosition().getX() + 1, mapManager.getPosition().getY());
					cell = this.map.get(pos);
					this.attachedBlocksWithPositions.put(pos, cell);
					break;
				case "w":
					pos = new RelativeCoordinate(mapManager.getPosition().getX() - 1, mapManager.getPosition().getY());
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
					pos = new RelativeCoordinate(mapManager.getPosition().getX(), mapManager.getPosition().getY() - 1);
					// this.attachedBlocks.remove(pos); // Does not work atm because attachedBlocks
					// contains relative coordinates
					break;
				case "s":
					pos = new RelativeCoordinate(mapManager.getPosition().getX(), mapManager.getPosition().getY() + 1);
					// this.attachedBlocks.remove(pos); // Does not work atm because attachedBlocks
					// contains relative coordinates
					break;
				case "e":
					pos = new RelativeCoordinate(mapManager.getPosition().getX() + 1, mapManager.getPosition().getY());
					// this.attachedBlocks.remove(pos); // Does not work atm because attachedBlocks
					// contains relative coordinates
					break;
				case "w":
					pos = new RelativeCoordinate(mapManager.getPosition().getX() - 1, mapManager.getPosition().getY());
					// this.attachedBlocks.remove(pos); // Does not work atm because attachedBlocks
					// contains relative coordinates
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

	private Action handleError() {
		say("Handle Error: " + lastAction + " - " + lastActionResult);
		if (lastActionResult.equals("failed_random")) {
			return new Action(lastAction, params);
		}
		if (lastAction.equals("move") && lastActionResult.equals("failed_path")) {
			return moveRandomly(1);
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
			int lastX = Integer.parseInt((String) lastActionParams.get(0));
			int lastY = Integer.parseInt((String) lastActionParams.get(1));

			Direction lastClearDirection = Direction.getDirectionFromInts(lastX, lastY);
			if (lastClearDirection != null) {
				String lastClearDir = lastClearDirection.toString();
				RelativeCoordinate posToClear = mapManager.getPosition().getCoordAfterWalkingInDir(lastClearDir);
				if (lastClearDir != null && mapManager.getObstacleLayer().containsKey(posToClear)
						&& mapManager.getObstacleLayer().get(posToClear) != null) {
					return new Action("clear", new Numeral(lastX), new Numeral(lastY));
				}
			} else {
				return moveRandomly(1);
			}
		}
		// TODO: expand error handling
		return moveRandomly(1);
	}

	private Action workerActionAttach() {
		String direction = (String) lastActionParams.get(0);
		say("Block had been successfully requested. Trying to attach...");
		return new Action("attach", new Identifier(direction));
	}

	private Action workerActionDetach() {
		say("Block attached, but no corresponding task(s).");
		say("Detaching from block...");
		return new Action("detach", new Identifier(attachedBlocks.get(0).getDirectDirection()));
	}

	private Action workerActionSearchGoalzone() {
		say("Need to look for goal zone");
		// Identify goal zone field candidates (= goal zone fields which are not
		// occupied and which have enough space around them to submit a task)
		Set<RelativeCoordinate> goalZoneFieldCandidates = pathCalc
				.determineGoalZoneFieldCandidates(this.getCurrentTask());

		if (!goalZoneFieldCandidates.isEmpty()) {
			say("Suitable goal zone fields identified");
			// Check if agent already on a suitable goal zone field
			if (!goalZoneFieldCandidates.contains(mapManager.getPosition())) {
				// Calculate direction agent should move into in order to get as fast as
				// possible to the next suitable goal zone field
				Action action = pathCalc.calculateShortestPathMap(goalZoneFieldCandidates);
//				Action action = pathCalc.calculateShortestPathManhattan(goalZoneFieldCandidates);
				if (action == null) {
					say("No path towards identified goal zone fields.");
					return explorerStep();
				} else {
					say("Path identified. Moving towards next suitable goal zone field...");
					return action;
				}
			} else {
				say("Already on suitable goal zone field");
				return this.workerActionSubmitTask();
			}
		}
		// Explore to find a suitable goal zone field
		return explorerStep();
	}

	private Action searchRolezone(String role) {
		say("Need to look for role zone");
		// Identify role zone field candidates (= role zone fields which are not
		// occupied and which have enough space around them to submit a task)
		Set<RelativeCoordinate> roleZoneFieldCandidates = pathCalc.determineRoleZoneFieldCandidates();

		if (!roleZoneFieldCandidates.isEmpty()) {
			say("Suitable role zone fields identified");
			// Check if agent already on a suitable role zone field
			if (!roleZoneFieldCandidates.contains(mapManager.getPosition())) {
				// Calculate direction agent should move into in order to get as fast as
				// possible to the next suitable role zone field
				Action action = pathCalc.calculateShortestPathMap(roleZoneFieldCandidates);
//				Action action = pathCalc.calculateShortestPathManhattan(roleZoneFieldCandidates);
				if (action == null) {
					say("No path towards identified role zone fields.");
					return explorerStep();
				} else {
					say("Path identified. Moving towards next suitable role zone field...");
					return action;
				}
			} else {
				say("Already on suitable role zone field");
				return new Action("adopt", new Identifier(role));
			}
		}
		// Explore to find a suitable role zone field
		return explorerStep();
	}

	private Action searchGoalZone() {
		say("Need to look for goal zone");
		// Identify free goal zone fields
		Set<RelativeCoordinate> goalZoneFields = pathCalc.determineGoalZoneFields();

		if (!goalZoneFields.isEmpty()) {
			say("Free goal zone fields identified");
			// Check if agent already on a goal zone field
			if (!goalZoneFields.contains(mapManager.getPosition())) {
				// Calculate direction agent should move into in order to get as fast as
				// possible to the next free goal zone field
				Action action = pathCalc.calculateShortestPathMap(goalZoneFields);
				if (action == null) {
					say("No path towards identified goal zone fields.");
					return explorerStep();
				} else {
					say("Path identified. Moving towards next free goal zone field...");
					return action;
				}
			} else {
				say("Already on goal zone field");
				return null; // Wait there
			}
		}
		// Explore to find a free goal zone field
		return explorerStep();
	}

	private Action workerActionSubmitTask() {
		if (this.getCurrentTask().isOneBlockTask())
			return this.workerActionSubmitOneBlockTask();
		if (this.getCurrentTask().isMultiBlockTask())
			return this.workerActionSubmitMultiBlockTask();
		say("cannot submit my task!");
		return null;
	}

	// todo - not working yet
	private Action workerActionSubmitMultiBlockTask() {

		if (!checkIfTaskComplete(this.getCurrentTask())) {
			// todo
		}
		say("Task '" + this.getCurrentTask().getName() + "' is complete");
		return submit(this.getCurrentTask());

	}

	private Action workerActionSubmitOneBlockTask() {
		if (!checkIfTaskComplete(this.getCurrentTask())) {
			return executeRotation(attachedBlocks.get(0).getRelativeCoordinate(),
					this.getCurrentTask().getRequirements().get(0).getRelativeCoordinate());
		}
		say("Task '" + this.getCurrentTask().getName() + "' is complete");
		return submit(this.getCurrentTask());
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
		// check if no tasks with current block available
		List<Task> correspondingTasks = determineCorrespondingTasks();
		if (correspondingTasks.isEmpty()) {
			return this.workerActionDetach();
		}
		// not on task and chooses task
		setCurrentTask(determineCurrentTask(correspondingTasks));
		say("my new task is: " + this.getCurrentTask().getName());

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
//		// check if no tasks with current block available
//		List<Task> correspondingTasks = determineCorrespondingTasks();
//		if (correspondingTasks.isEmpty()) {
//			return this.workerActionDetach();
//		}
//		// not on task and chooses task
//		setCurrentTask(determineCurrentTask(correspondingTasks));
//		say("my new task is: " + this.getCurrentTask().getName());

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
			return this.workerActionSearchGoalzone();
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
		return this.workerActionSearchGoalzone();
	}

	// todo
	private Action workerActionAssembleBlocks() {

		return null;
	}

	private boolean blocksAssembled() {
		if (deepEquals(this.getAssembledBlockMatrix(), this.getCurrentTask().getBlockMatrix())) {
			return true;
		}

		return false;
	}

	/**
	 * editor: michael
	 * 
	 * overrides deepEquals(Object a, Object b)
	 *
	 * @param basematrix
	 * @param testmatrix
	 * @return true if matrices are equal
	 */
	private static boolean deepEquals(String[][] basematrix, String[][] testmatrix) {
		if (basematrix.length == testmatrix.length && basematrix[0].length == testmatrix[0].length)
			return false;

		for (int i = 0; i < basematrix.length; i++) {
			if (!(basematrix[i].equals(testmatrix[i])))
				return false;
		}

		return true;
	}

	/**
	 * editor: michael
	 * 
	 * only works for blocks attached to the south of the agent
	 *
	 * @return s matrix of connected blocks
	 */
	private String[][] createAssembledBlockMatrix() {
		String[][] matrix = new String[5][5];

		if (this.attachedBlocks.isEmpty()) {
			return matrix;
		}

		// should return the matrix of each blockmatrix of each block
		String[][] help = this.getAttachedBlockSouth().getBlockMatrix();

		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				matrix[i][j] = help[i][j];
			}
		}
		return matrix;
	}

	private Block getAttachedBlockSouth() {
		// TODO Auto-generated method stub
		return null;
	}

	private String[][] getAssembledBlockMatrix() {
		return this.assembledBlockMatrix;
	}

	private void setAssembledBlockMatrix(String[][] matrix) {
		this.assembledBlockMatrix = matrix;
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

	private List<Task> getOneBlockTasks() {
		List<Task> oneBlockTasks = new ArrayList<>();
		for (Task t : tasks) {
			if (t.isOneBlockTask()) {
				oneBlockTasks.add(t);
			}
		}
		return oneBlockTasks;
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

		// Task fastestTask = findFastestTask();

		int blocksMissingForTask = 10; // @Carina einkommentieren, wenn multiblock tasks gehen
		for (Task task : tasks) {
			if (this.numberOfBlocksMissingForTask(task) < blocksMissingForTask) {
				fastestTask = task;
				blocksMissingForTask = this.numberOfBlocksMissingForTask(task);
			}

		}

//		for (Task task : tasks) {
//			if (task.isOneBlockTask()) {
//				fastestTask = task;
//				break;
//			}
//
//		}
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
		// distance to dispenser, dispenser -> goalzone, end of task -> how often could
		// it be completed -> possible reward

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
			say("Got all blocks for task: " + task.getName());
			return true;
		}

		return false;
	}

	/**
	 * editor: michael
	 *
	 * @param task
	 * @return
	 */
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

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private HashMap<String, Integer> missingBlocksForTaskHash() {
		if (this.attachedBlocks.isEmpty()) {
			return this.currentTask.getBlockTypeMap();
		}
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		if (currentTask == null) {
			return map;
		}

		map.put("b0", this.getCurrentTask().getBlockTypeMap().get("b0") - this.getAttachedBlockTypeMap().get("b0"));
		map.put("b1", this.getCurrentTask().getBlockTypeMap().get("b1") - this.getAttachedBlockTypeMap().get("b1"));
		map.put("b2", this.getCurrentTask().getBlockTypeMap().get("b2") - this.getAttachedBlockTypeMap().get("b2"));
		map.put("b3", this.getCurrentTask().getBlockTypeMap().get("b3") - this.getAttachedBlockTypeMap().get("b3"));

		return map;
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private List<String> missingBlockTypesList() {
		List<String> list = new ArrayList<>();

		if (this.missingBlocksForTaskHash().get("b0") > 0)
			list.add("b0");
		if (this.missingBlocksForTaskHash().get("b1") > 0)
			list.add("b1");
		if (this.missingBlocksForTaskHash().get("b2") > 0)
			list.add("b2");
		if (this.missingBlocksForTaskHash().get("b3") > 0)
			list.add("b3");

		return list;
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private HashMap<String, Integer> getAttachedBlockTypeMap() {
		return this.attachedBlockTypeMap;
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private HashMap<String, Integer> nextDispenserTypeHashMap() {
		// todo get distance from pathcalc
		int b0distance = this.pathCalc.calcStepsToNextDispenser("b0");
		int b1distance = this.pathCalc.calcStepsToNextDispenser("b1");
		int b2distance = this.pathCalc.calcStepsToNextDispenser("b2");
		int b3distance = this.pathCalc.calcStepsToNextDispenser("b3");

		// not found / not in config -> -1/-2 => +9999 steps

		int maxValue = 9999;

		if (b0distance == -1 || b0distance == -2)
			b0distance = maxValue;
		if (b1distance == -1 || b1distance == -2)
			b1distance = maxValue;
		if (b2distance == -1 || b2distance == -2)
			b2distance = maxValue;
		if (b3distance == -1 || b3distance == -2)
			b3distance = maxValue;

		HashMap<String, Integer> map = new HashMap<String, Integer>();

		map.put("b0", b0distance);
		map.put("b1", b1distance);
		map.put("b2", b2distance);
		map.put("b3", b3distance);

		return map;
	}

	/**
	 * editor: michael
	 *
	 * @return
	 */
	private List<String> nextDispenserTypeList() {
		return sortHashMapKeysToList(nextDispenserTypeHashMap());
	}

	private List<String> sortHashMapKeysToList(HashMap<String, Integer> nextDispenserTypeHashMap) {
		List<String> list = new ArrayList<>();

		for (Map.Entry<String, Integer> set : nextDispenserTypeHashMap.entrySet()) {
			// System.out.println(set.getKey());
			if (list.isEmpty()) {
				// System.out.println("list empty");
				list.add(set.getKey());
				// System.out.println("add "+set.getKey()+" to empty list");
			}
			for (int i = 0; i < list.size(); i++) {
				if (list.contains(set.getKey()))
					break;
				if (nextDispenserTypeHashMap.get(list.get(i)) >= set.getValue()) {
					list.add(i, set.getKey());
					// System.out.println("add "+set.getKey()+" to "+ i);
					break;
				}
			}
			if (!list.contains(set.getKey()))
				list.add(set.getKey());
		}
		return list;
	}

	/**
	 * editor: michael
	 * 
	 * returns Dispenser from certain type
	 *
	 * @param type
	 * @return Dispenser
	 */
	private Dispenser getNextDispenserFromType(String dispenserType) {
		Dispenser dispenser = pathCalc.getClosestDispenser(dispenserType);

		if (dispenser == null) {
			say("could not find a dispenser with type: " + dispenserType);
			return null;
		}

		return dispenser;
	}

	/**
	 * editor: michael
	 * 
	 * if worker knows of dispensers fitting to the required blocktypes they will go
	 * to the closest
	 * 
	 * @return go to dispenser or explorerstep
	 */
	private Action workerActionSearchDispenser() {
		Dispenser disp = null;
		List<String> nextDispenserTypeList = this.nextDispenserTypeList();

		System.out.println("workerActionSearchDispenser");
		System.out.println(nextDispenserTypeList);

		// no current task
		if (this.getCurrentTask() == null) { // @Carina
			setCurrentTask(determineCurrentTask(getOneBlockTasks()));
		}
		if (this.getCurrentTask() == null && !nextDispenserTypeList.isEmpty()) {
			System.out.println("no current task");
//			say("looking for other dispensers"); //@Carina
			disp = getNextDispenserFromType(nextDispenserTypeList.get(0));
			say("Going to next dispenser");
		} else if (this.getCurrentTask() != null && !nextDispenserTypeList.isEmpty()) {
			System.out.println("with current task");
			for (String dispensertype : nextDispenserTypeList) {
//				if(getCurrentTask().isOneBlockTask()) {
				String taskBlockType = getCurrentTask().getRequirements().get(0).getBlockType();
				if (taskBlockType.equals(dispensertype)) {
					say("getting next dispenser from type " + dispensertype);
					disp = getNextDispenserFromType(dispensertype);
					break;
				}
//				}
//				else if (this.missingBlockTypesList().contains(dispensertype)) { //@Carina einkommentieren, wenn multiblock geht
//					disp = getNextDispenserFromType(dispensertype);
//					say("Suitable dispenser(s) identified");
//					break;
				// eventuell String disp?
//				}
			}
		}

		System.out.println(disp);

		if (disp == null)
			return explorerStep();

		System.out.println("Next Dispenser is " + disp.getType());

		return this.goToDispenser(disp);
	}

	/**
	 * editor: michael
	 *
	 * goes to certain dispenser if there is a path
	 *
	 * @param disp
	 * @return move(dir) to next dispenser or explorerstep
	 */
	private Action goToDispenser(Dispenser disp) {

		// agent is next to Dispenser
		say("AM I NEXT TO DISPENSER (GO TO DISPENSER)? "+ disp.getRelativeCoordinate().isNextToAgent(mapManager.getPosition()));
		if (disp.getRelativeCoordinate().isNextToAgent(mapManager.getPosition())) {
			RelativeCoordinate dispenserCoord = disp.getRelativeCoordinate();
			String direction = dispenserCoord.getDirectDirection(mapManager.getPosition());
//			// if block is directly on dispenser, attach it //@Carina
//			if (mapManager.getBlockLayer().containsKey(dispenserCoord)
//					&& mapManager.getBlockLayer().get(dispenserCoord) != null) {
//				say("attaching Block next to me");
//				List<Task> correspondingTasks = determineCorrespondingTasks();
//				if (correspondingTasks.isEmpty()) { // clear block blocking dispenser
//					return clear(direction);
//				}
//				return new Action("attach", new Identifier(direction));
//			}
			if (determineCorrespondingTasks(disp.getType()).size() != 0) {
				return requestBlock(direction);
			}
		}
		// If agent is on top of dispenser -> move one step to be able to request a
		// block
		if (disp.getRelativeCoordinate().getX() == mapManager.getPosition().getX()
				&& disp.getRelativeCoordinate().getY() == mapManager.getPosition().getY()) {
			say("I am on a dispenser. Stepping aside.");
			return moveRandomly(1); // TODO: check for obstacles or blocks
		}
		// Move towards dispenser
		Action action = pathCalc.calculateShortestPathMap(disp);
//		Action action = pathCalc.calculateShortestPathManhattan(disp);
		if (action == null) {
			say("No path towards dispenser.");
			return explorerStep();
		} else {
			say("Path identified. Moving towards dispenser...");
			return action;
		}
	}

	/**
	 * Provides direction to the next reachable loose block of the required type (in
	 * agent's vision range), if available
	 * 
	 * @param requiredType The required block type
	 * 
	 * @return Move action if path identified, otherwise null
	 */
	private Action checkForLooseBlocks(String requiredType) {
		Set<RelativeCoordinate> destinations = new HashSet<>();
		for (Block block : blocks) {
			if (block.getType().equals(requiredType)) {
				RelativeCoordinate coordinate = block.getRelativeCoordinate();
				if (!attachedThings.contains(coordinate)) {
					if (coordinate.isNextToAgent()) {
						String direction = coordinate.getDirectDirection();
						say("Loose block identified. Attaching...");
						return new Action("attach", new Identifier(direction));
					} else {
						// Suitable loose block identified, add its surrounding cells as destinations
						RelativeCoordinate currentPosition = mapManager.getPosition();
						RelativeCoordinate absoluteCoordinate = new RelativeCoordinate(
								currentPosition.getX() + coordinate.getX(), currentPosition.getY() + coordinate.getY());
						RelativeCoordinate north = new RelativeCoordinate(absoluteCoordinate.getX(),
								absoluteCoordinate.getY() - 1);
						RelativeCoordinate east = new RelativeCoordinate(absoluteCoordinate.getX() + 1,
								absoluteCoordinate.getY());
						RelativeCoordinate south = new RelativeCoordinate(absoluteCoordinate.getX(),
								absoluteCoordinate.getY() + 1);
						RelativeCoordinate west = new RelativeCoordinate(absoluteCoordinate.getX() - 1,
								absoluteCoordinate.getY());
						if (!pathCalc.checkIfOccupied(north)) {
							destinations.add(north);
						}
						if (!pathCalc.checkIfOccupied(east)) {
							destinations.add(east);
						}
						if (!pathCalc.checkIfOccupied(south)) {
							destinations.add(south);
						}
						if (!pathCalc.checkIfOccupied(west)) {
							destinations.add(west);
						}
					}
				}
			}
		}
		return pathCalc.calculateShortestPathMap(destinations);
//		return pathCalc.calculateShortestPathManhattan(destinations);
	}

	// default (main) worker method
	private Action workerStep() {
		// If a block has been requested in the last step, then attach this block
		if (lastAction.equals("request") && lastActionResult.equals("success")) {
			say("Attaching Block");
			return this.workerActionAttach();
		}
		// This only works for tasks with one block
		// If the agent has a block attached, then either detach from it (if no
		// corresponding task), look for goal zone, rotate or submit
		if (!attachedBlocks.isEmpty()) {
			say("handle Block");
			return this.workerActionHandleBlock();
		}
		// no block requested in last step or currently attached
		say("search dispenser");
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

	private Action constructorStep() {
		Action action = searchGoalZone();
		if (action != null) {
			return action;
		}
		// TODO: add constructor logic
		return new Action("skip");
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
			Action action = pathCalc.calculateShortestPathVision(currentRole.getVision(), occupiedFields,
					new HashSet(obstaclesInSight));
			if (action != null) {
				return action;
			} else {
				return new Action("skip");
			}
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
		// falls mindestens in Teammitglied sichtbar, wird dies nach seinem Namen
		// befragt, um einen map-Austausch einzuleiten

		if ((friendlyAgents.size() == 1) && (counterMapExchange > 10) && (exchangePartner == null)
				&& (isExplorer)) {
			say("I start map exchange process");
			exchangePartner = new RelativeCoordinate(friendlyAgents.get(0).getX(), friendlyAgents.get(0).getY());
			mailbox.broadcastMapRequest(currentStep, getName());
			exchangeCounter = 1;
			counterMapExchange = 8;
		}
		counterMapExchange = counterMapExchange + 1;

		ArrayList<String> possibleDirs = getAllPossibleDirs();
		String prefDir = getPreferredDir();
		if (possibleDirs != null && possibleDirs.size() != 0) {
			if (!prefDir.equals("")) {
				if (possibleDirs.contains(prefDir)) {
					if ((attachedBlocks.size() == 1)) {
						String attachedBlockDir = getBlockDir(attachedBlocks.get(0));
						return rotateAccordingToAttachedBlock(prefDir, attachedBlockDir);
					}
					return move(currentRole.getCurrentSpeed(), prefDir);
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
			say("Exchange partner exists end explorer step: " + !(exchangePartner == null));
			return moveRandomly(1, possibleDirs); // TODO adjust 1 to current step length

		}
		return new Action("skip");
	}
	
	private Action clear(String dir) { //dupliziert in Pathcalc
		int x = 0;
		int y = 0;
		switch (dir) {
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

	private String getBlockDir(Block b) { // maybe put in pathcalc
		if ((attachedBlocks.size() == 1)) {
			if (b.distanceFromAgent() == 1) {
				say("attachedBlock direction " + b.getDirectDirection());
				return b.getDirectDirection();
			}
		}
		return "";
	}

	private Action rotateAccordingToAttachedBlock(String prefDir, String attachedBlockDir) { //dupliziert in Pathcalc
		if (getOppositeDirection(attachedBlockDir) == prefDir) { // no Rotation necessary
			return move(prefDir);
		} else if (attachedBlockDir.equals(prefDir)) { // Rotation direction irrelevant
			return new Action("rotate", new Identifier("cw"));
		} else {
			switch (prefDir) {
			case ("n"): {
				if (attachedBlockDir.equals("e")) {
					return new Action("rotate", new Identifier("cw"));
				} else {
					return new Action("rotate", new Identifier("ccw"));
				}
			}
			case ("e"): {
				if (attachedBlockDir.equals("s")) {
					return new Action("rotate", new Identifier("cw"));
				} else {
					return new Action("rotate", new Identifier("ccw"));
				}
			}
			case ("s"): {
				if (attachedBlockDir.equals("w")) {
					return new Action("rotate", new Identifier("cw"));
				} else {
					return new Action("rotate", new Identifier("ccw"));
				}
			}
			case ("w"): {
				if (attachedBlockDir.equals("n")) {
					return new Action("rotate", new Identifier("cw"));
				} else {
					return new Action("rotate", new Identifier("ccw"));
				}
			}
			}
		}
		return null;
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
//		say("get Preferred Dir: " + preferredDir);
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
	 * Moves the agent in a given direction
	 * 
	 * @param dir The direction to move
	 * @return The move action
	 */
	private Action move(int stepNum, String dir) {
		say("moving " + dir);
		switch (stepNum) {// TODO mehrere Schritte in dieselbe richtung, wenn wir die Schrittlänge
							// gespeichert haben.
		case (2): {
			return new Action("move", new Identifier(dir), new Identifier(dir));
		}
		case (3): {
			return new Action("move", new Identifier(dir), new Identifier(dir), new Identifier(dir));
		}
		default: {
			return new Action("move", new Identifier(dir));
		}
		}

	}

	/**
	 * Moves the agent randomly in any direction that is not occupied and has enough space for the agent's attached blocks
	 * 
	 * @param stepNum The number of steps the agent should move
	 * @return The move action
	 */
	private Action moveRandomly(int stepNum) {
		RelativeCoordinate currentPosition = mapManager.getPosition();
		List<String> allowedDirections = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			int x = dir.getDx();
			int y = dir.getDy();
			RelativeCoordinate coordinate = new RelativeCoordinate(currentPosition.getX() + x, currentPosition.getY() + y);
			// Check if the cell is occupied
			boolean occupied = pathCalc.checkIfOccupied(coordinate);
			if (occupied) {
				continue;
			}
			// Check if attachedBlocks of agent fit into the cell's surrounding cells
			for (Block attachedBlock : attachedBlocks) {
				RelativeCoordinate absolutePosition = new RelativeCoordinate(coordinate.getX() + attachedBlock.getRelativeCoordinate().getX(),
						coordinate.getY() + attachedBlock.getRelativeCoordinate().getY());
				occupied = pathCalc.checkIfOccupied(absolutePosition);
				if (occupied) {
					break;
				}
			}
			if (!occupied) {
				allowedDirections.add(dir.toString());
			}
		}
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
			Direction obstacleDir = mapManager.getAnyAdjacentObstacle();
			if (obstacleDir != null) {
				return new Action("clear", new Numeral(obstacleDir.getDx()), new Numeral(obstacleDir.getDy()));
			} else {
				return new Action("skip");
			}
		}
		Random rand = new Random();
		List<String> randomDirections = new ArrayList<>();
		for (int i = 1; i <= stepNum; i++) {
			String randomDirection = allowedDirections.get(rand.nextInt(allowedDirections.size()));
			randomDirections.add(randomDirection);
		}

		if (stepNum > 2) { // TODO: expand
			say("Warning: moveRandomly() does not yet support steps > 2");
			stepNum = 2;
		}

		switch (stepNum) { // TODO: expand
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
				if (allowedDirections.size() > 0) {
					direction2 = allowedDirections.get(rand.nextInt(allowedDirections.size()));
				} else {
					direction2 = null;
				}
			}
			if (direction2 != null) {
				say("Moving two steps randomly...");
				return new Action("move", new Identifier(direction1), new Identifier(direction2));
			} else {
				say("Moving one step randomly...");
				return new Action("move", new Identifier(direction1));
			}
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
		List<Task> correspondingTasks = new ArrayList<>();

		// check first if there are oneBlockTasks to solve
		if (attachedBlocks.size() == 1) {
			for (Task task : tasks) {
				if (task.getRequirements().get(0).getBlockType().equals(attachedBlocks.get(0).getType())
						&& task.isOneBlockTask()) {
					correspondingTasks.add(task);
				}
			}
			return correspondingTasks;
		}
//		// check if any attached block fits to a multiBlockTask // TODO @Carina-->
//		// include if agent can handle multi block tasks
//		for (Task task : tasks) {
//			for (int i = 0; i < task.getRequirements().size(); i++) {
//				for (int j = 0; j < attachedBlocks.size(); j++) {
//					if (task.getRequirements().get(i).getBlockType().equals(attachedBlocks.get(j).getType())) {
//						correspondingTasks.add(task);
//						break;
//					}
//				}
//			}
//		}
		return correspondingTasks;
	}

	private List<Task> determineCorrespondingTasks(String dispenserType) {
		List<Task> correspondingTasks = new ArrayList<>();

		// check first if there are oneBlockTasks to solve

		for (Task task : tasks) {
			if (task.getRequirements().get(0).getBlockType().equals(dispenserType) && task.isOneBlockTask()) {
				correspondingTasks.add(task);
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
		phase = 0;
		mapManager = new MapManagement(this.currentStep);
		pathCalc = new PathCalc(mapManager, attachedBlocks);
		roles.clear();
		attachedBlocks.clear();
		simStartPerceptsSaved = false;
		isExplorer = false;
		isConstructor = false;
		Mission.prepareForNextSimulation();
	}

	/**
	 * Enables agent to send his map later to requesting agent
	 * 
	 * @param to   Requesting agent
	 * @param step Step of request for map
	 */
	public void deliverMap(String to, int step) {
		say("I will send my map to " + to);
		requestingExplorer = to;
		stepOfRequest = step;
		requestForMap = true;
	}

	/**
	 * Saves a received map for subsequent use in a list
	 * 
	 * @param mapBundle Received map
	 */
	public void handleMap(MapBundle mapBundle) {
		if (exchangeCounter > 0) {
			say("I add the map to my map list");
			mapBundleList.add(mapBundle);
		}
	}

	/**
	 * Replaces own map with received map
	 * 
	 * @param mapManagement Received map
	 * @param change        Difference between coordinate system of own map and
	 *                      received map
	 * @param agents        List auf agents with the same coordinate system
	 */
	public void receiveMap(MapManagement mapManagement, RelativeCoordinate change, HashSet<String> agents) {
		int xDiff = change.getX();
		int yDiff = change.getY();
		RelativeCoordinate newPosition = new RelativeCoordinate(mapManager.getPosition().getX() + xDiff,
				mapManager.getPosition().getY() + yDiff);
		mapManagement.setPosition(newPosition);
		this.mapManager = mapManagement;
		mapManager.updateLastPosition(xDiff, yDiff);
		for (String str : agents) {
			knownAgents.add(str);
		}
	}

	/**
	 * Sends own map to another agent to replace its map
	 * 
	 * @param addressee         Receiving agent
	 * @param mapManager        Own map
	 * @param addresseePosition Position of receiving agent in the sender'S
	 *                          coordinate System
	 * @param knownAgents       List of agent with the same coordinate system
	 */
	private void sendMap(String addressee, MapManagement mapManager, RelativeCoordinate addresseePosition,
			HashSet<String> knownAgents) {
		mailbox.sendMap(addressee, mapManager, addresseePosition, knownAgents);
	}

	/**
	 * Merges two maps using the coordinates of both map owners in their coordinate
	 * system
	 */
	private void mergeMaps() {
		exchangeCounter = 0;
		if (!(mapBundleList.size() == 0)) {
			say("I try to find the correct map");
			ArrayList<MapBundle> candidates = new ArrayList<MapBundle>();
			int xDistance = exchangePartner.getX();
			int yDistance = exchangePartner.getY();
			Iterator<MapBundle> it = mapBundleList.iterator();
			while (it.hasNext()) {
				MapBundle analyzedMap = it.next();
				ArrayList<RelativeCoordinate> seenAgents = analyzedMap.getTeamMembers();
				Iterator<RelativeCoordinate> itAgents = seenAgents.iterator();
				while (itAgents.hasNext()) {
					RelativeCoordinate agentPos = itAgents.next();
					if ((agentPos.getX() == -xDistance) && (agentPos.getY() == -yDistance)) {
						candidates.add(analyzedMap);
					}
				}
			}
			say("I found a candidate for map exchange: " + (candidates.size() == 1));
			if (candidates.size() == 1 && !(knownAgents.contains(candidates.get(0).getOwner()))) {
				say("Map merging...");
				String newAgent = mapManager.mergeMaps(candidates.get(0), exchangePartner);
				say("I send the updated map to my partner " + newAgent);
				sendMap(newAgent, mapManager, exchangePartner, knownAgents);
				knownAgents.add(newAgent);
				counterMapExchange = 0;
			}
			exchangePartner = null;
			mapBundleList = new ArrayList<MapBundle>();
		}
	}

	/**
	 * Sends the own map to requesting agent. Based on step of request, the agent's
	 * in the last or the actual step ist sent.
	 */
	private void answerRequestForMap() {
		say("I give you my map in step " + currentStep);
		if (stepOfRequest == currentStep) {
			mailbox.deliverMap(requestingExplorer,
					new MapBundle(getName(), currentStep, mapManager.getBlockLayer(), mapManager.getDispenserLayer(),
							mapManager.getGoalzoneLayer(), mapManager.getObstacleLayer(), mapManager.getRolezoneLayer(),
							mapManager.copyTeamMembers(), mapManager.getPosition(), currentStep));
			stepOfRequest = -3;
		}
		if (stepOfRequest == currentStep - 1) {
			mailbox.deliverMap(requestingExplorer,
					new MapBundle(getName(), currentStep - 1, mapManager.getBlockLayer(),
							mapManager.getDispenserLayer(), mapManager.getGoalzoneLayer(),
							mapManager.getObstacleLayer(), mapManager.getRolezoneLayer(),
							mapManager.copyLastTeamMembers(), mapManager.getLastPosition(), currentStep - 1));
			stepOfRequest = -3;
		}
		requestForMap = false;
	}

	/**
	 * SEnds own map to all agents with the same coordinate system
	 */
	private void updateMapsOfKnownAgents() {
		for (String name : knownAgents) {
			mailbox.updateMap(name, mapManager, knownAgents);
		}
	}

	/**
	 * Merges own map and received map of an agent with the same coordinate system
	 * 
	 * @param mapManagement Received map
	 * @param newAgents     List of agents with the same coordinate system
	 */
	public void updateMap(MapManagement mapManagement, HashSet<String> newAgents) {
		for (String str : newAgents) {
			knownAgents.add(str);
		}
		mapManager.updateMap(mapManagement);
	}

	/**
	 * Analyzes, how long agent can violate norms without getting inactivated
	 * 
	 * @return Number of steps, until agent's energy sinks below 1
	 */
	private double analyzeNorms() {
		double energy = energyLevel;
		int lifespan = 0;
		say("Normanalyse startet");

		for (int i = currentStep; i < simSteps + 1; i++) {
			lifespan = lifespan + 1;
			for (Norm norm : norms) {
				NormRequirement requirements = norm.getRequirements();
				if (requirements.getName().equals("Carry")) {
					if (!(i < norm.getFirstStep()) && attachedBlocks.size() > requirements.getQuantity()) {
						energy = energy - norm.getPunishment();
					}
				} else if (requirements.getName().equals("Adopt")) {
					int number = 0;
					for (String str : rolesOfAgents.keySet()) {
						if (requirements.getName().equals(rolesOfAgents.get(str).getName())) {
							number = number + 1;
						}
					}
					if (!(i < norm.getFirstStep()) && currentRole.getName().equals(requirements.getName())
							&& number > requirements.getQuantity()) {
						energy = energy - norm.getPunishment();
					}
				}
				if (energy < 1) {
					return lifespan;
				}
			}
		}

		return lifespan;
	}
}
