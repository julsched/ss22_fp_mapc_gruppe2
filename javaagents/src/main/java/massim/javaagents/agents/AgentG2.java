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

	private List<Percept> attachedThingsPercepts = new ArrayList<>();
	private List<Dispenser> dispensers = new ArrayList<>();
	private List<Block> blocks = new ArrayList<>();
	private List<Entity> entities = new ArrayList<>();
	private List<RelativeCoordinate> occupiedFields = new ArrayList<>();
	private List<RelativeCoordinate> goalZoneFields = new ArrayList<>();
	private List<RelativeCoordinate> roleZoneFields = new ArrayList<>();
	private List<Task> tasks = new ArrayList<>();
	private List<Norm> norms = new ArrayList<>();
	// Blocks that might(!) be directly attached to the agent (right next to agent)
	private List<Block> attachedBlocks = new ArrayList<>();

	private HashMap<String, RelativeCoordinate> seenTeamMembers = new HashMap<String, RelativeCoordinate>();
	private AgentInformation seenAgent = null;
	private boolean mapRequest = false;
	private String requestingExplorer;
	private int stepMap = -1;
	private HashMap<RelativeCoordinate, Cell> externalMap;
	private RelativeCoordinate externalPosition;
	private ArrayList<RelativeCoordinate> friendlyAgents = new ArrayList<RelativeCoordinate>();
	private HashMap<RelativeCoordinate, Cell> map = new HashMap<RelativeCoordinate, Cell>(); //see map
	private RelativeCoordinate currentPos = new RelativeCoordinate(0, 0); // TODO delete if currentAbsolutePos works.
	private Orientation rotated = Orientation.NORTH;
	private HashMap<RelativeCoordinate, Cell> attachedBlocksWithPositions = new HashMap<>();
	private String roleName = ""; // TODO -> automatisch aktualisieren, wenn Rolle geändert wird

	private String lastMoveDir = "";
//	private MapOfAgent map = new MapOfAgent();
//	private RelativeCoordinate currentAbsolutePos = new RelativeCoordinate(0, 0);

	/**
	 * Constructor.
	 * 
	 * @param name    the agent's name
	 * @param mailbox the mail facility
	 */
	public AgentG2(String name, MailService mailbox) {
		super(name, mailbox);
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
		if (simSteps != 0 && currentStep == simSteps -1) {
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

		// must be set first, so agents knows currentStep for sorting Percepts and for having a structured console output
		setCurrentStep(percepts);													
		
		saveStepPercepts(percepts);

		analyzeAttachedThings();
		
		// Auswertung der abgespeicherten Ergebnisse der lastAction
		this.evaluateLastAction();
		
		// nach der Evaluation ist die currentPosition korrekt bestimmt und es können die things der map hinzugefügt werden
		this.updateMap(percepts);
		
		// Einleiten des Austausches der maps
		if (this.seenAgent != null) {
			this.requestMap(this.seenAgent.getName(), this.currentStep);
		}
		
		// Übergeben der aktuellen Map
		if (this.mapRequest) {
			if (this.stepMap == this.currentStep) {
				this.mailbox.deliverMap(requestingExplorer, this.getName(), this.map, this.currentPos, this.currentStep);
			}
			this.stepMap = -1;
			this.mapRequest = false;
		}
		
		// Zusammenführen der Maps und Übergeben der geupdateten Map
		this.mergeMaps();
		this.sendMap(this.seenAgent.getName(), map);
		

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
        dispensers.clear();
        blocks.clear();
        entities.clear();
        occupiedFields.clear();
        goalZoneFields.clear();
        roleZoneFields.clear();
        tasks.clear();
        norms.clear();
        hitFrom = null;

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
					dispensers.add(dispenser);
					break;
				}
				if (thingType.equals("block")) {
					String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
					Block block = new Block(relativeCoordinate, blockType, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, block);
					blocks.add(block);
					occupiedFields.add(relativeCoordinate);
					break;
				}
				if (thingType.equals("entity")) {
					String teamName = ((Identifier) percept.getParameters().get(3)).getValue();
					Entity entity = new Entity(relativeCoordinate, teamName);
					entities.add(entity);
					occupiedFields.add(relativeCoordinate);
					if (this.teamName.equals(teamName)) {
						RelativeCoordinate relCo = new RelativeCoordinate(this.currentPos.getX() + x, this.currentPos.getY() + y);
						this.friendlyAgents.add(relCo);
					}
					break;
				}
				if (thingType.equals("obstacle")) {
					occupiedFields.add(relativeCoordinate);
					Obstacle obstacle = new Obstacle(relativeCoordinate, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, obstacle); //TODO @Carina -> make current AbsolutePos work. Then we can make a Map. 
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
				// Remove this if-statement, once agent can handle tasks with more than one block
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
				goalZoneFields.add(goalZoneField);
				break;
			}
			case "roleZone" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate roleZoneField = new RelativeCoordinate(x, y);
				roleZoneFields.add(roleZoneField);
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
					this.seenAgent.setName(name);
					String role = ((Identifier) percept.getParameters().get(2)).getValue();
					this.seenAgent.setRole(role);
					int energy = ((Numeral) percept.getParameters().get(3)).getValue().intValue();
					this.seenAgent.setEnergy(energy);
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
			if (lastAction.equals("rotate") && lastActionResult.equals("success")) {
				// Works only if exactly one block is attached to agent
				String direction = (String) lastActionParams.get(0);
				Block block = attachedBlocks.get(0);
				attachedBlocks.clear();
				if (direction.equals("cw")) {
					if (block.getRelativeCoordinate().isOneStepEast()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, 1), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepSouth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(-1, 0), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepWest()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, -1), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepNorth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(1, 0), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
				} else if (direction.equals("ccw")) {
					if (block.getRelativeCoordinate().isOneStepEast()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, -1), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepSouth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(1, 0), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepWest()) {
						Block updatedBlock = new Block(new RelativeCoordinate(0, 1), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
					if (block.getRelativeCoordinate().isOneStepNorth()) {
						Block updatedBlock = new Block(new RelativeCoordinate(-1, 0), block.getType(), block.getLastSeen());
						attachedBlocks.add(updatedBlock);
					}
				}
			}
			// TODO: check every step if things are still attached (could have been removed by a clear event?)
		}

		if (lastAction.equals("attach") && lastActionResult.equals("success")) {
			String direction = (String) lastActionParams.get(0);
			switch (direction) {
				case "n" -> {
					//Could be friendly entity/obstacle/block -> check what it is and save in dedicated list
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
					//Could be friendly entity/obstacle/block -> check what it is and save in dedicated list
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
					//Could be friendly entity/obstacle/block -> check what it is and save in dedicated list
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
					//Could be friendly entity/obstacle/block -> check what it is and save in dedicated list
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
            switch(perceptName) {
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
            switch(perceptName) {
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
				Iterator<Object> it = this.lastActionParams.iterator();
				while (it.hasNext()) {
					Parameter dir = (Parameter) it.next();
					if (dir instanceof Identifier) {
						int x = this.currentPos.getX();
						int y = this.currentPos.getY();
						String dirString = ((Identifier) dir).getValue();
						if ((dirString.equals("n") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("w") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x, y + 1));
						} else if ((dirString.equals("s") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("n") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x, y - 1));
						} else if ((dirString.equals("e") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("n") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("w") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x + 1, y));
						} else if ((dirString.equals("w") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("n") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x - 1, y));
						}
					}
				}
			} else if (this.lastActionResult.equals("partial_success")) {
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					int x = this.currentPos.getX();
					int y = this.currentPos.getY();
					String dirString = ((Identifier) dir).getValue();
					if ((dirString.equals("n") && this.rotated.equals(Orientation.NORTH))
							|| (dirString.equals("w") && this.rotated.equals(Orientation.EAST))
							|| (dirString.equals("s") && this.rotated.equals(Orientation.SOUTH))
							|| (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
						this.setCurrentPosition(new RelativeCoordinate(x, y + 1));
					} else if ((dirString.equals("s") && this.rotated.equals(Orientation.NORTH))
							|| (dirString.equals("e") && this.rotated.equals(Orientation.EAST))
							|| (dirString.equals("n") && this.rotated.equals(Orientation.SOUTH))
							|| (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
						this.setCurrentPosition(new RelativeCoordinate(x, y - 1));
					} else if ((dirString.equals("e") && this.rotated.equals(Orientation.NORTH))
							|| (dirString.equals("n") && this.rotated.equals(Orientation.EAST))
							|| (dirString.equals("w") && this.rotated.equals(Orientation.SOUTH))
							|| (dirString.equals("s") && this.rotated.equals(Orientation.WEST))) {
						this.setCurrentPosition(new RelativeCoordinate(x + 1, y));
					} else if ((dirString.equals("w") && this.rotated.equals(Orientation.NORTH))
							|| (dirString.equals("s") && this.rotated.equals(Orientation.EAST))
							|| (dirString.equals("e") && this.rotated.equals(Orientation.SOUTH))
							|| (dirString.equals("n") && this.rotated.equals(Orientation.WEST))) {
						this.setCurrentPosition(new RelativeCoordinate(x - 1, y));
					}
					// Fehlerbehandlung
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
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					RelativeCoordinate pos;
					Cell cell;
					switch (dirString) {
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
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					RelativeCoordinate pos;
					switch (dirString) {
					case "n":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() + 1);
						this.attachedBlocks.remove(pos);
						break;
					case "s":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() - 1);
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
				}
				break;
			case "rotate":
				switch (lastActionResult) {
				case "success":
					Parameter rot = (Parameter) this.lastActionParams.get(0);
					if (rot instanceof Identifier) {
						String rotString = ((Identifier) rot).getValue();
						HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
						if (rotString.equals("cw")) {
							this.rotated = Orientation.changeOrientation(rotated, 1);
							for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
								Cell cell = this.attachedBlocksWithPositions.get(key);
								temp.put(new RelativeCoordinate(key.getY(), -key.getX()), cell);
							}
						} else {
							this.rotated = Orientation.changeOrientation(rotated, -1);
							for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
								Cell cell = this.attachedBlocksWithPositions.get(key);
								temp.put(new RelativeCoordinate(-key.getY(), key.getX()), cell);
							}
						}
						this.attachedBlocksWithPositions = temp;
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
					// FEhlerbehandlung
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
					Parameter xCellA = (Parameter) this.lastActionParams.get(0);
					Parameter yCellA = (Parameter) this.lastActionParams.get(1);
					Parameter xCellB = (Parameter) this.lastActionParams.get(2);
					Parameter yCellB = (Parameter) this.lastActionParams.get(3);
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
					int xCoor = 0;
					int yCoor = 0;
					Parameter xCell = (Parameter) this.lastActionParams.get(0);
					if (xCell instanceof Numeral) {
						xCoor = ((Numeral) xCell).getValue().intValue();
					} else {
						break;
					}
					Parameter yCell = (Parameter) this.lastActionParams.get(1);
					if (yCell instanceof Numeral) {
						yCoor = ((Numeral) yCell).getValue().intValue();
					} else {
						break;
					}
					this.map.remove(new RelativeCoordinate(xCoor, yCoor));
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
					Parameter role = (Parameter) this.lastActionParams.get(0);
					if (role instanceof Identifier) {
						this.roleName = ((Identifier) role).getValue();
					}
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
						say("Moving one step east...");
						return new Action("move", new Identifier("e"));

					case "e":
						say("Moving one step south...");
						return new Action("move", new Identifier("s"));

					case "s":
						say("Moving one step west...");
						return new Action("move", new Identifier("w"));

					case "w":
						say("Moving one step north...");
						return new Action("move", new Identifier("n"));
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
			say("Rotation was not succesfull.");
			return moveRandomly(1);
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
            return new Action("detach", new Identifier(attachedBlocks.get(0).getDirectDirection()));
	}
	
	//worker searches or chooses Goalzone
	private Action workerActionSearchGoalzone(List<Task> correspondingTasks) {
		say("Need to look for goal zone");
        // Identify goal zone field candidates (= goal zone fields which are not occupied and which have enough space around them to submit a task)
		HashMap<RelativeCoordinate, List<Task>> goalZoneFieldCandidates = determineGoalZoneFieldCandidates(correspondingTasks);

        if (!goalZoneFieldCandidates.isEmpty()) {
            say("Suitable goal zone fields identified");
			// Check if agent already on a suitable goal zone field
			if (!goalZoneFieldCandidates.containsKey(new RelativeCoordinate(0, 0))) {
				// Calculate direction agent should move into in order to get as fast as possible to the next suitable goal zone field
				// TODO: check if attached blocks will fit on this path
				Direction dir = PathCalc.calculateShortestPath(currentRole.getVision(), occupiedFields, determineLocations("attachedBlock", null), goalZoneFieldCandidates.keySet());
				if (dir == null) {
					say("No path towards identified goal zone fields.");
					return moveRandomly(currentRole.getSpeedWithoutAttachments());
				}
				say("Path identified. Moving towards next suitable goal zone field...");
				switch(dir) {
					case NORTH -> {
						say("NORTH");
						return new Action("move", new Identifier("n"));
					}
					case EAST -> {
						say("EAST");
						return new Action("move", new Identifier("e"));
					}
					case SOUTH -> {
						say("SOUTH");
						return new Action("move", new Identifier("s"));
					}
					case WEST -> {
						say("WEST");
						return new Action("move", new Identifier("w"));
					}
				}
			} else {
				say("Already on suitable goal zone field");
				Task completedTask = checkIfAnyTaskComplete(correspondingTasks);
				if (completedTask == null) {
					// TODO: select the task which requires the least amount of rotations
					Task selectedTask = goalZoneFieldCandidates.get(new RelativeCoordinate(0, 0)).get(0);
					return executeRotation(attachedBlocks.get(0).getRelativeCoordinate(), selectedTask.getRequirements().get(0).getRelativeCoordinate());
				}
				say("Task '" + completedTask.getName() + "' is complete");
				if (taskSubmissionPossible(completedTask)) {
					String taskName = completedTask.getName();
					say("Submitting task '" + taskName + "'...");
					return new Action("submit", new Identifier(taskName));
				}
			}
        }
        // Move randomly to find a suitable goal zone field
        return moveRandomly(currentRole.getSpeedWithoutAttachments());
	}
	
	
	//worker has block attached and chooses how to handle it
	private Action workerActionHandleBlock() {
        // no tasks with current block available
		List<Task> correspondingTasks = determineCorrespondingTasks();
        if (correspondingTasks.isEmpty()) {
            return this.workerActionDetach();
        }
        // chooses or searches goalzone
        return this.workerActionSearchGoalzone(correspondingTasks);
	}
	
	private Action workerActionSearchDispenser() {
        if (!dispensers.isEmpty()) {
            say("Dispenser(s) identified");
			Set<RelativeCoordinate> dispenserLocations = new HashSet<>();
			for (Dispenser dispenser : dispensers) {
				// Check whether there is a task for this block type
				if (checkForCorrespondingTask(dispenser.getType())) {
					dispenserLocations.add(dispenser.getRelativeCoordinate());
				}
			}
			if (dispenserLocations.isEmpty()) {
				say("No corresponding tasks for identified dispenser(s).");
				// Keep moving randomly to find a different dispenser
                return moveRandomly(currentRole.getSpeedWithoutAttachments());
			} else {
				for (RelativeCoordinate relativeCoordinate : dispenserLocations) {
					if (relativeCoordinate.isNextToAgent()) {
						String direction = relativeCoordinate.getDirectDirection();
                		return requestBlock(direction);
					}
					// If agent is on top of dispenser -> move one step to be able to request a block
					if (relativeCoordinate.getX() == 0 && relativeCoordinate.getY() == 0) {
						return moveRandomly(1);
					}
				}
				// Move towards dispenser
				Direction dir = PathCalc.calculateShortestPath(currentRole.getVision(), occupiedFields, determineLocations("attachedBlock", null), dispenserLocations);
				if (dir == null) {
					say("No path towards goal zone.");
					return moveRandomly(currentRole.getSpeedWithoutAttachments());
				}
				say("Path identified. Moving towards dispenser...");
				switch(dir) {
					case NORTH -> {
						say("NORTH");
						return new Action("move", new Identifier("n"));
					}
					case EAST -> {
						say("EAST");
						return new Action("move", new Identifier("e"));
					}
					case SOUTH -> {
						say("SOUTH");
						return new Action("move", new Identifier("s"));
					}
					case WEST -> {
						say("WEST");
						return new Action("move", new Identifier("w"));
					}
				}
			}
        }
        // Move randomly to find a dispenser
        return moveRandomly(currentRole.getSpeedWithoutAttachments());
	}
	
	// default (main) worker method
	private Action workerStep() {
        // If a block has been requested in the last step, then attach this block
        if (lastAction.equals("request") && lastActionResult.equals("success")) {
            return this.workerActionAttach();
        }
        // This only works for tasks with one block
        // If the agent has a block attached, then either detach from it (if no corresponding task), look for goal zone, rotate or submit
        if (!attachedBlocks.isEmpty()) {
        	return this.workerActionHandleBlock();
        }
        // no block requested in last step or currently attached        
        return this.workerActionSearchDispenser();
    }

	private Action explorerStep() {
		// falls mindestens ein Teammitglied sichtbar, wird dies nach seinem Namen befragt, um einen map-Austausch einzuleiten
		// TODO: Bedingung sollte weiter eingeschränkt, weil sonst nur surveyed und nicht explort wird
		if (!this.friendlyAgents.isEmpty()) {
			Iterator<RelativeCoordinate> it = this.friendlyAgents.iterator();
			while (it.hasNext()) {
				RelativeCoordinate relCo = it.next();
				int x = this.currentPos.getX() + relCo.getX();
				int y = this.currentPos.getY() + relCo.getY();
				if (Math.abs(x) + Math.abs(y) > this.currentRole.getVision() - 1) {
					this.friendlyAgents.remove(relCo);
				}
			}
			if (!this.friendlyAgents.isEmpty()) {
				seenAgent = new AgentInformation(this.currentPos.getX() + this.friendlyAgents.get(0).getX(), this.currentPos.getY() + this.friendlyAgents.get(0).getY());
				return new Action("survey", new Numeral(this.friendlyAgents.get(0).getX()), new Numeral(this.friendlyAgents.get(0).getY()));
			}
		}
		ArrayList<String> possibleDirs = getPossibleDirs();
		ArrayList<String> prefDirs = getPreferredDirs();
		say("this is my map: " + map);
		if (possibleDirs != null && possibleDirs.size() != 0) {
			if (prefDirs != null) {
				// flip coin
				// return new Action("move", new Identifier(dir));
				return null;
			}
			if (possibleDirs.size() > 1) { // remove Dir where you came from
				if (lastMoveDir != null) {
					possibleDirs.remove(getOppositeDirection(lastMoveDir));
				}
			}
			return moveRandomly(1, possibleDirs); // TODO adjust 1 to current step length
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

	private ArrayList<String> getPreferredDirs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
    Checks in clockwise-sequence if there is a dispenser of the required block type directly next to the agent
    @param blockType block type of dispenser agent is looking for
    @return direction of the dispenser or "x" if no dispenser of the required block type next to the agent
     */
    private String checkIfDispenserNext(String blockType) {
        if (!dispensers.isEmpty()) {
            for (Dispenser dispenser : dispensers) {
                String type = dispenser.getType();
                if (type.equals(blockType)) {
                    RelativeCoordinate relativeCoordinate = dispenser.getRelativeCoordinate();

                    if (relativeCoordinate.isOneStepNorth()) {
                        return "n";
                    }
                    if (relativeCoordinate.isOneStepEast()) {
                        return "e";
                    }
                    if (relativeCoordinate.isOneStepSouth()) {
                        return "s";
                    }
                    if (relativeCoordinate.isOneStepWest()) {
                        return "w";
                    }
                }  
            } 
        }
        return "x";
    }

	/**
     Determines which goal zone cells the agent can walk towards for submitting a task, taking into account surrounding obstacles and the list of corresponding tasks provided
    @param correspondingTasks A list of tasks which can be fulfilled considerung the agent's currently attached block
	@return A map of relative coordinates of goal zone cells the agent can walk to to submit a task and the tasks that can be submitted in this cell
     */
	// Works for one-block tasks only
	private HashMap<RelativeCoordinate, List<Task>> determineGoalZoneFieldCandidates(List<Task> correspondingTasks) {
		// First check which goal zone fields are free (meaning no obstacle/block/entity on them)
		RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
		List<RelativeCoordinate> attachedBlockLocations = determineLocations("attachedBlock", null);
		List<RelativeCoordinate> goalZoneFieldsFree = new ArrayList<>();
		for (RelativeCoordinate goalZoneField : goalZoneFields) {
			if (goalZoneField.equals(agentPosition) || attachedBlockLocations.contains(goalZoneField) || !occupiedFields.contains(goalZoneField)) {
				goalZoneFieldsFree.add(goalZoneField);
			}
		}

		// Then check which ones of the free goal zone fields have enough space around them to submit a task
		HashMap<RelativeCoordinate, List<Task>> goalZoneFieldCandidates = new HashMap<>();
		for (RelativeCoordinate goalZoneField : goalZoneFieldsFree) {
			List<Task> tasks = new ArrayList<>();
			for (Task task : correspondingTasks) {
				RelativeCoordinate requirement = task.getRequirements().get(0).getRelativeCoordinate();
				RelativeCoordinate fieldToBeChecked = new RelativeCoordinate(goalZoneField.getX() + requirement.getX(), goalZoneField.getY() + requirement.getY());
				if (goalZoneFieldsFree.contains(fieldToBeChecked)) {
					tasks.add(task);
				}
			}
			if (!tasks.isEmpty()) {
				goalZoneFieldCandidates.put(goalZoneField, tasks);
			}
		}
		return goalZoneFieldCandidates;
	}

	/**
    Gets a list of relative coordinates of free goal zones which are directly next to the agent
    @return A list of relative coordinates of adjacent free goal zone cells
     */
	private List<RelativeCoordinate> getAdjacentGoalZoneFields(List<RelativeCoordinate> goalZoneFieldsFree) {
		List<RelativeCoordinate> adjacentGoalZoneFields = new ArrayList<>();
		for (RelativeCoordinate relativeCoordinate : goalZoneFieldsFree) {
			if (relativeCoordinate.distanceFromAgent() == 1) {
				adjacentGoalZoneFields.add(relativeCoordinate);
			}
		}
		return adjacentGoalZoneFields;
	}

	/**
    Checks whether agent is positioned on a goal zone (does not check for attached blocks)
    @return True if agent is positioned on a goal zone, otherwise false
     */
	private boolean agentInGoalZone() {
		RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
		for (RelativeCoordinate goalZoneField : goalZoneFields) {
			if (goalZoneField.equals(agentPosition)) {
				return true;
			}
		}
		return false;
	}

	/**
    Makes the agent request a block from a dispenser
    @param direction Direction of the dispenser
    @return The request action
     */
	private Action requestBlock(String direction) {
		say("Requesting block...");
		return new Action("request", new Identifier(direction));
	}

	/**
    Executes a rotation action for bringing the attached block to the correct position (if possible, otherwise skip)
    @param direction The coordinate of the attached block
	@param targetBlockPos The coordinate the attached block needs to be rotated to
    @return Rotation action (if possible, otherwise skip Action)
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
				say("Rotating in clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("cw"));
		}
		if ((currentBlockPos.isOneStepNorth() && targetBlockPos.isOneStepWest())
			|| (currentBlockPos.isOneStepEast() && targetBlockPos.isOneStepNorth())
			|| (currentBlockPos.isOneStepSouth() && targetBlockPos.isOneStepEast())
			|| (currentBlockPos.isOneStepWest() && targetBlockPos.isOneStepSouth())) {
				say("Rotating in counter-clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("ccw"));
		}
		if ((currentBlockPos.isOneStepNorth() && targetBlockPos.isOneStepSouth())) {
			if (!occupiedFields.contains(new RelativeCoordinate(1, 0))) {
				say("Rotating in clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("cw"));
			}
			if (!occupiedFields.contains(new RelativeCoordinate(-1, 0))) {
				say("Rotating in counter-clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("ccw"));
			}
		}
		if ((currentBlockPos.isOneStepEast() && targetBlockPos.isOneStepWest())) {
			if (!occupiedFields.contains(new RelativeCoordinate(0, 1))) {
				say("Rotating in clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("cw"));
			}
			if (!occupiedFields.contains(new RelativeCoordinate(0, -1))) {
				say("Rotating in counter-clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("ccw"));
			}
		}
		if ((currentBlockPos.isOneStepSouth() && targetBlockPos.isOneStepNorth())) {
			if (!occupiedFields.contains(new RelativeCoordinate(-1, 0))) {
				say("Rotating in clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("cw"));
			}
			if (!occupiedFields.contains(new RelativeCoordinate(1, 0))) {
				say("Rotating in counter-clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("ccw"));
			}
		}
		if ((currentBlockPos.isOneStepWest() && targetBlockPos.isOneStepEast())) {
			if (!occupiedFields.contains(new RelativeCoordinate(0, -1))) {
				say("Rotating in clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("cw"));
			}
			if (!occupiedFields.contains(new RelativeCoordinate(0, 1))) {
				say("Rotating in counter-clockwise direction to fulfill task...");
				return new Action("rotate", new Identifier("ccw"));
			}
		}
		return new Action("skip");
	}

	/**
    Moves the agent randomly in any directions
    @param stepNum The number of steps the agent should move
    @return The move action
     */
	private Action moveRandomly(int stepNum) {
		List<String> allowedDirections = new ArrayList<String>(Arrays.asList("n", "e", "s", "w"));
		return moveRandomly(stepNum, allowedDirections);
	}

	/**
    Moves the agent randomly in the allowed directions
    @param stepNum The number of steps the agent should move.
	@param allowedDirections The list of allowed directions
    @return The move action
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
			return new Action("move", new Identifier(direction));
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
    Checks from a list of tasks whether any task is complete (required blocks attached and all blocks on correct position)
    @param tasks The list of tasks to be checked against
    @return The task which is complete, otherwise null
     */
	private Task checkIfAnyTaskComplete(List<Task> tasks) {
        for (Task task : tasks) {
            if (checkIfTaskComplete(task)) {
                return task;
            }
        }
        return null;
    }

	/**
    Checks if the task is complete (all blocks attached and on the correct position)
    @param task The task to be checked against
    @return True if task is complete, otherwise false
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
    Checks whether the task can be submitted
	(checks if all requirements of the task are fulfilled and whether agent and attached blocks are positioned on goal zone fields)
    @param task The task to be checked against
    @return True if task can be submitted, otherwise false
     */
	private boolean taskSubmissionPossible(Task task) {
		if (!checkIfTaskComplete(task)) {
			return false;
		}
		List<TaskRequirement> requirements = task.getRequirements();
		for (TaskRequirement requirement : requirements) {
			RelativeCoordinate relativeCoordinateR = requirement.getRelativeCoordinate();

			boolean inGoalZone = false;
			for (RelativeCoordinate goalZoneField : goalZoneFields) {
				if (relativeCoordinateR.equals(goalZoneField)) {
					inGoalZone = true;
					break;
				}
			}
			if (!inGoalZone) {
				return false;
			}
		}
		for (RelativeCoordinate goalZoneField : goalZoneFields) {
			RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
			if (goalZoneField.equals(agentPosition)) {
				return true;
			}
		}
		return false;
	}

	/**
    Determines the closest cell of the required type
    @param type The cell type, e.g. dispenser
	@param additionalInfo Additional info regarding the required cell, e.g. for dispenser the block type. If null, type is ignored
    @return The closest cell which is of the required type and which fulfills the additional info requirements (if not null)
     */
	private Cell determineClosest(String type, String additionalInfo) {
        switch(type) {
            case "dispenser" -> {
                if (dispensers.isEmpty()) {
                    return null;
                }
                if (additionalInfo != null) {
                    // Look for dispensers of the required type
                    List<Dispenser> dispenserCandidates = new ArrayList<>();
                    for (Dispenser dispenser : dispensers) {
                        if (dispenser.getType().equals(additionalInfo)) {
                            dispenserCandidates.add(dispenser);
                        }
                    }
                    // Select closest dispenser (ignoring obstacles)
                    if (dispenserCandidates.isEmpty()) {
                        return null;
                    } else {
                        Dispenser closestDispenser = Dispenser.getClosestDispenser(dispenserCandidates);
                        return closestDispenser;
                    }
                } else {
                    // Select closest dispenser
                    Dispenser closestDispenser = Dispenser.getClosestDispenser(dispensers);
                    return closestDispenser;
                }
            }
        }
        return null;
    }

	/**
    Determines a list of coordinates where the required thing can be found
    @param type The cell type of the required thing, e.g. dispenser
	@param additionalInfo Additional info regarding the required thing, e.g. for dispenser the block type. If null, type is ignored
    @return A list of coordinates where instances of the required thing can be found
     */
	private List<RelativeCoordinate> determineLocations(String type, String additionalInfo) {
		List<RelativeCoordinate> locations = new ArrayList<>();

		switch(type) {
			case "dispenser" -> {
				if (dispensers.isEmpty()) {
					break;
				}
				if (additionalInfo != null) {
					// Look for dispensers of the required type
                    for (Dispenser dispenser : dispensers) {
                        if (dispenser.getType().equals(additionalInfo)) {
                            locations.add(dispenser.getRelativeCoordinate());
                        }
                    }
				} else {
					for (Dispenser dispenser : dispensers) {
						locations.add(dispenser.getRelativeCoordinate());
					}
				}
			}
			case "attachedBlock" -> {
				if (attachedBlocks.isEmpty()) {
					break;
				}
				if (additionalInfo != null) {
					// TODO
					break;
				} else {
					for (Block block : attachedBlocks) {
						locations.add(block.getRelativeCoordinate());
					}
				}
			}
		}
		return locations;
	}

	/**
    Analyzes the currently attached blocks and determines tasks which require these blocks
    @return A list of the tasks which require the currently attached blocks
     */
	private List<Task> determineCorrespondingTasks() {
        List<Task> correspondingTasks = new ArrayList<>();

        // At the moment this logic only works when the agent has only one block attached and there are only one-block-tasks.
        String blockType = attachedBlocks.get(0).getType();
        for (Task task : tasks) {
            if (task.getRequirements().get(0).getBlockType().equals(blockType)) {
                correspondingTasks.add(task);
            }
        }
        return correspondingTasks;
    }

	/**
    Checks if there is task which requires the specific block type
    @param blockType The block type which is analyzed
    @return True if there is a task that requires the block type, otherwise false
     */
    private boolean checkForCorrespondingTask(String blockType) {
        // At the moment this method only checks if there is a task for the given block type. And it only works for one-block-tasks.
        // TODO: also keep in mind the deadline and rewards of tasks
        for (Task task : tasks) {
            if (task.getRequirements().get(0).getBlockType().equals(blockType)) {
                return true;
            }
        }
        return false;
    }

	/**
    Checks if two directions are opposite directions
    @param direction1 First direction
	@param direction2 Second direction
    @return True if directions are opposite, otherwise false
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
    Returns the opposite direction
    @param direction Direction for which opposite direction is required
    @return The opposite direction
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
        // Empty/overwrite data structures which are not emptied/overwritten inside the methods for saving new percepts
		// TODO: add all data structures which need to be emptied before the next simulation
        currentStep = -1;
        actionID = -1;
        roles.clear();
		attachedBlocks.clear();
		simStartPerceptsSaved = false;
    }
	
    public void deliverMap(String to, int step) {
    	this.requestingExplorer = to;
    	this.stepMap = step;
    	this.mapRequest = true;
    }
	
	public void handleMap(String from, HashMap<RelativeCoordinate, Cell> map, RelativeCoordinate currentPos, int step) {
		this.stepMap = step;
		this.externalMap = map;
		this.externalPosition = currentPos;
	}
	
	private void mergeMaps() {
		if (this.currentStep == this.stepMap) {
			RelativeCoordinate rc = this.seenAgent.getRelativeCoordinate();
			ArrayList<RelativeCoordinate> agents = new ArrayList<RelativeCoordinate>();
			
			Iterator<Entity> it = this.entities.iterator();
			while (it.hasNext()) {
				Entity ent = it.next();
				int xCoor = ent.getRelativeCoordinate().getX() + this.currentPos.getX();
				int yCoor = ent.getRelativeCoordinate().getY() + this.currentPos.getY();
				if (Math.abs(rc.getX() - xCoor) < 2 && Math.abs(rc.getY() - yCoor) < 2) {
					agents.add(new RelativeCoordinate(xCoor, yCoor));
				}
			}
			if (agents.size() < 1 || agents.size() > 1) {
				return;
			}
			RelativeCoordinate pos = agents.get(0);
			int xDiff = pos.getX() - this.externalPosition.getX();
			int yDiff = pos.getY() - this.externalPosition.getY();
			for (RelativeCoordinate key : this.externalMap.keySet()) {
				RelativeCoordinate newKey = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
				if (!this.map.containsKey(newKey) || this.map.get(newKey).getLastSeen() < externalMap.get(key).getLastSeen()) {
					this.map.put(newKey, this.externalMap.get(key));
				}			
			}
		}
	}
	
	private void updateMap(List<Percept> percepts) {
		if (percepts == null) { // Error handling if no percepts are available
			return;
		} else {
			Iterator it = percepts.iterator();
			Percept percept = percepts.get(0);
			while (it.hasNext()) {
				if (!((Percept) it.next()).getName().equals("things")) {
					percept = (Percept) it.next();
				} else {
					percept = (Percept) it.next();
					String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
					// Maybe Check if x and y are Numeral first
					int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
					int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
					RelativeCoordinate absolutePosition = new RelativeCoordinate(this.currentPos.getX() + x, this.currentPos.getY() + y);
					if (thingType.equals("dispenser")) {
						String type = ((Identifier) percept.getParameters().get(3)).getValue();
						Dispenser dispenser = new Dispenser(absolutePosition, type, currentStep);
						map.put(absolutePosition, dispenser);
						break;
					}
					if (thingType.equals("block")) {
						String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
						Block block = new Block(absolutePosition, blockType, currentStep);
						map.put(absolutePosition, block);
						break;
					}
					if (thingType.equals("obstacle")) {
						Obstacle obstacle = new Obstacle(absolutePosition, currentStep);
						map.put(absolutePosition, obstacle);
						break;
					}
				}
								
			}
				
		}
		
	}
	
	public void receiveMap(HashMap<RelativeCoordinate, Cell> map) {
		this.map = map;
	}

}
