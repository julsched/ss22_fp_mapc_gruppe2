package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.agents.g2utils.*;

import java.util.*;

/**
 * A very basic agent.
 */
public class AgentJulia extends Agent {

    private String teamName;
    private int teamSize;
    private int stepsOverall;
    private List<Role> roles = new ArrayList<>();

    private int lastActionID = -1;
    private String lastAction = "";
    private List<Object> lastActionParams = new ArrayList<>();
    private String lastActionResult = "";
    private String blockRequested = "";

    private int currentStep = -1;
    private Role currentRole = null;
    // TODO: fill this with maximum step number of agent's current role
    private int maxStepNum = 2;


    private List<Percept> attachedThingsPercepts = new ArrayList<>();
    private List<Dispenser> dispensers = new ArrayList<>();
    private List<Block> blocks = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<RelativeCoordinate> occupiedFields = new ArrayList<>();
    private List<RelativeCoordinate> goalZoneFields = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();
    private List<Block> attachedBlocks = new ArrayList<>();


    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public AgentJulia(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {
        sortPercepts(getPercepts());
        if (lastAction.equals("move") && !lastActionResult.equals("success") && lastActionParams.size() == 1) {
            // Get direction
            String direction = (String) lastActionParams.get(0);
            System.out.println("I AM STUCK!!!!!!!! Trying to walk '" + direction + "'!!!");

            RelativeCoordinate desiredField = RelativeCoordinate.getRelativeCoordinate(direction);
            for (RelativeCoordinate relativeCoordinate : occupiedFields) {
                if (relativeCoordinate.equals(desiredField)) {
                    System.out.println("Field towards direction '" + direction + "' is already occupied!");
                    // This will ensure that agent will try all possible directions if one step after another fails due to occupied fields
                    switch(direction) {
                        case "n": 
                            return new Action("move", new Identifier("e"));

                        case "e":
                            return new Action("move", new Identifier("s"));

                        case "s":
                            return new Action("move", new Identifier("w"));

                        case "w":
                            return new Action("move", new Identifier("n"));
                    }
                }
            }
        }
        // If a block has been requested in the last step, then attach this block
        if (!blockRequested.isEmpty()) {
            String direction = blockRequested;
            blockRequested = "";
            return new Action("attach", new Identifier(direction));
        }
        // This only works for tasks with one block
        // If the agent has a block attached, then either rotate, look for goal zone or submit
        if (!attachedBlocks.isEmpty()) {
            if (!checkIfTaskComplete(tasks.get(0))) {
                // TODO: Add check in which direction rotation is possible (due to obstacles)
                return new Action("rotate", new Identifier("cw"));
            }
            if (taskSubmissionPossible(tasks.get(0))) {
                String taskName = tasks.get(0).getName();
                return new Action("submit", new Identifier(taskName));
            }
            // Walk to goal zone
            if (!goalZoneFields.isEmpty()) {
                List<RelativeCoordinate> adjacentGoalZoneFields = getAdjacentGoalZoneFields();
                if (!agentInGoalZone() && adjacentGoalZoneFields.isEmpty()) {
                    RelativeCoordinate closestGoalZoneField = RelativeCoordinate.getClosestCoordinate(goalZoneFields);
                    int x = closestGoalZoneField.getX();
                    int y = closestGoalZoneField.getY();
                    // TODO: improve this
                    if (y < 0) {
                        return new Action("move", new Identifier("n"));
                    }
                    if (y > 0) {
                        return new Action("move", new Identifier("s"));
                    }
                    if (x < 0) {
                        return new Action("move", new Identifier("w"));
                    }
                    if (x > 0) {
                        return new Action("move", new Identifier("e"));
                    }
                }
                // Agent is at one of the four corners of a goal zone (outside)
                if (!agentInGoalZone() && adjacentGoalZoneFields.size() == 1) {
                    RelativeCoordinate adjacentGoalZoneField = adjacentGoalZoneFields.get(0);
                    String direction = adjacentGoalZoneField.getDirectDirection();
                    return new Action("move", new Identifier(direction));
                }
                // Agent is somewhere along the edges of the goal zone (outside)
                if (!agentInGoalZone() && adjacentGoalZoneFields.size() == 2) {
                    List<String> directions = new ArrayList<>();
                    for (RelativeCoordinate adjacentGoalZoneField : adjacentGoalZoneFields) {
                        directions.add(adjacentGoalZoneField.getDirectDirection());
                    }
                    if (directions.contains("n") && directions.contains("e")) {
                        return new Action("move", new Identifier("e"), new Identifier("n"));
                    }
                    if (directions.contains("n") && directions.contains("w")) {
                        return new Action("move", new Identifier("w"), new Identifier("n"));
                    }
                    if (directions.contains("s") && directions.contains("e")) {
                        return new Action("move", new Identifier("e"), new Identifier("s"));
                    }
                    if (directions.contains("s") && directions.contains("w")) {
                        return new Action("move", new Identifier("w"), new Identifier("s"));
                    }
                }
                // This statement is only reached if the task is complete, but cannot be submitted yet and the agent is already inside the goal zone
                if (agentInGoalZone()) {
                    // TODO: move in such a way that all blocks are inside the goal zone
                    List<String> allowedDirections = new ArrayList<>();
                    for (RelativeCoordinate adjacentGoalZoneField : adjacentGoalZoneFields) {
                        allowedDirections.add(adjacentGoalZoneField.getDirectDirection());
                    }
                    moveRandomly(1, allowedDirections);
                }
            }
            return moveRandomly(maxStepNum);
        }
        // At the moment limited to one type of dispenser/block ('b0')
        String result = checkIfDispenserNext("b0");
        // TODO: add check whether block already lies on top of dispenser
        if (!result.equals("x")) {
            return requestBlock(result);
        }
        String dispenserDirection = lookFor("dispenser", "b0");
        if (!dispenserDirection.equals("x")) {
            return new Action("move", new Identifier(dispenserDirection));
        }
        return moveRandomly(maxStepNum);
    }

    private void sortPercepts(List<Percept> percepts) {
        // Delete previous percepts
        lastActionParams = new ArrayList<>();
        attachedThingsPercepts = new ArrayList<>();
        dispensers = new ArrayList<>();
        blocks = new ArrayList<>();
        entities = new ArrayList<>();
        occupiedFields = new ArrayList<>();
        goalZoneFields = new ArrayList<>();
        tasks = new ArrayList<>();
        attachedBlocks = new ArrayList<>();
        
        // Allocate percepts to variables
        for (Percept percept : percepts) {
            if (percept.getName().equals("step")) {
                currentStep = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                System.out.println("----- Current step: " + currentStep + " -----");
                break;
            }
        }

        for (Percept percept : percepts) {
            // Initial percept information is stored only before the first step
            if (currentStep == -1) {
                if (percept.getName().equals("team")) {
                    teamName = ((Identifier) percept.getParameters().get(0)).getValue();
                    continue;
                }
                if (percept.getName().equals("teamSize")) {
                    teamSize = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    continue;
                }
                if (percept.getName().equals("steps")) {
                    stepsOverall = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    continue;
                }
                
                if (percept.getName().equals("role") && percept.getParameters().size() == 6) {
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
                    roles.add(role);
                    continue;
                }
            }
            
            if (percept.getName().equals("actionID")) {
                int id = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                if (id > lastActionID) {
                    lastActionID = id;
                }
                continue;
            }
            if (percept.getName().equals("lastAction")) {
                lastAction = ((Identifier) percept.getParameters().get(0)).getValue();
                continue;
            }
            if (percept.getName().equals("lastActionResult")) {
                lastActionResult = ((Identifier) percept.getParameters().get(0)).getValue();
                continue;
            }
            if (percept.getName().equals("lastActionParams")) {
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
            }
            if (percept.getName().equals("thing")) {
                String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
                if (thingType.equals("dispenser")) {
                    int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
                    String type = ((Identifier) percept.getParameters().get(3)).getValue();

                    Dispenser dispenser = new Dispenser(new RelativeCoordinate(x, y), type);
                    dispensers.add(dispenser);
                    continue;
                }
                if (thingType.equals("block")) {
                    int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
                    String blockType = ((Identifier) percept.getParameters().get(3)).getValue();

                    RelativeCoordinate relativeCoordinate = new RelativeCoordinate(x, y);
                    Block block = new Block(relativeCoordinate, blockType);
                    blocks.add(block);
                    occupiedFields.add(relativeCoordinate);
                    continue;
                }
                if (thingType.equals("entity")) {
                    int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
                    String teamName = ((Identifier) percept.getParameters().get(3)).getValue();

                    RelativeCoordinate relativeCoordinate = new RelativeCoordinate(x, y);
                    Entity entity = new Entity(relativeCoordinate, teamName);
                    entities.add(entity);
                    occupiedFields.add(relativeCoordinate);
                    continue;
                }
                if (thingType.equals("obstacle")) {
                    int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                    int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
                    RelativeCoordinate relativeCoordinate = new RelativeCoordinate(x, y);
                    occupiedFields.add(relativeCoordinate);
                    continue;
                }
            }
            if (percept.getName().equals("task")) {
                String name = ((Identifier) percept.getParameters().get(0)).getValue();
                int deadline = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
                int reward = ((Numeral) percept.getParameters().get(2)).getValue().intValue();

                Parameter paramRequirements = percept.getParameters().get(3);
                List<Parameter> params = new ArrayList<>();
                for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
                    params.add(((ParameterList) paramRequirements).get(i));
                }
                List<TaskRequirement> requirements = new ArrayList<>();
                for (Parameter param : params) {
                    Parameter paramCoordinateX = ((Function)param).getParameters().get(0);
                    Parameter paramCoordinateY = ((Function)param).getParameters().get(1);
                    Parameter paramBlockType = ((Function)param).getParameters().get(2);

                    int x = ((Numeral) paramCoordinateX).getValue().intValue();
                    int y = ((Numeral) paramCoordinateY).getValue().intValue();
                    String blockType = ((Identifier) paramBlockType).getValue();

                    TaskRequirement requirement = new TaskRequirement(new RelativeCoordinate(x, y), blockType);
                    requirements.add(requirement);
                }
                Task task = new Task(name, deadline, reward, requirements);
                tasks.add(task);
                continue;
            }
            if (percept.getName().equals("attached")) {
                attachedThingsPercepts.add(percept);
                continue;
            }
            if (percept.getName().equals("goalZone")) {
                int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
                int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

                RelativeCoordinate goalZoneField = new RelativeCoordinate(x, y);
                goalZoneFields.add(goalZoneField);
                continue;
            }
            if (percept.getName().equals("role") && percept.getParameters().size() == 1) {
                String roleName = ((Identifier) percept.getParameters().get(0)).getValue();
                if (currentRole == null || !currentRole.getName().equals(roleName)) {
                    Role newRole = Role.getRole(roles, roleName);
                    currentRole = newRole;
                }
                System.out.println("Agent " + getName() + "'s current role: " + currentRole.getName());
                continue;
            }
        }
        for (Percept percept : attachedThingsPercepts) {
            int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
            int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
            RelativeCoordinate relativeCoordinateAttachedThing = new RelativeCoordinate(x, y);

            for (Block block : blocks) {
                RelativeCoordinate relativeCoordinate = block.getRelativeCoordinate();
                if (relativeCoordinate.equals(relativeCoordinateAttachedThing)) {
                    attachedBlocks.add(block);
                }
            }
            // Same should be done with entity and obstacle once variables are implemented
        }
    }

    /**
    Checks in clockwise-sequence if there is a dispenser of the required block type directly next to the agent
    @param blockType block type of dispenser agent is looking for
    @return direction of the dispenser or 'x' if no dispenser of the required block type next to the agent
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

    private List<RelativeCoordinate> getAdjacentGoalZoneFields() {
        List<RelativeCoordinate> adjacentGoalZoneFields = new ArrayList<>();
        for (RelativeCoordinate relativeCoordinate : goalZoneFields) {
            if (relativeCoordinate.distanceFromAgent() == 1) {
                adjacentGoalZoneFields.add(relativeCoordinate);
            }
        }
        return adjacentGoalZoneFields;
    }

    private boolean agentInGoalZone() {
        RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
        for (RelativeCoordinate goalZoneField : goalZoneFields) {
            if (goalZoneField.equals(agentPosition)) {
                return true;
            }
        }
        return false;
    }

    private Action requestBlock(String direction) {
        blockRequested = direction;
        return new Action("request", new Identifier(direction));
    }

    private Action moveRandomly(int stepNum) {
        List<String> allowedDirections = new ArrayList<String>(Arrays.asList("n", "e", "s", "w"));
        return moveRandomly(stepNum, allowedDirections);
    }

    private Action moveRandomly(int stepNum, List<String> allowedDirections) {
        Random rand = new Random();
        List<String> randomDirections = new ArrayList<>();
        for (int i = 1; i <= stepNum; i++) {
            String randomDirection = allowedDirections.get(rand.nextInt(allowedDirections.size()));
            randomDirections.add(randomDirection);
        }
        
        switch(stepNum) {
            case 1 -> {
                String direction = randomDirections.get(0);
                return new Action("move", new Identifier(direction));
            }
            case 2 -> {
                String direction1 = randomDirections.get(0);
                String direction2 = randomDirections.get(1);
                if (oppositeDirections(direction1, direction2)) {
                    allowedDirections.remove(direction2);
                    direction2 = allowedDirections.get(rand.nextInt(allowedDirections.size()));
                }
                return new Action("move", new Identifier(direction1), new Identifier(direction2));
            }
            default -> {
                return null;
            }
        }
    }

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

    private String lookFor(String type, String additionalInfo) {
        switch(type) {
            case "dispenser" -> {
                // Look for dispensers of the required type
                List<Dispenser> dispenserCandidates = new ArrayList<>();
                for (Dispenser dispenser : dispensers) {
                    String dispenserType = dispenser.getType();
                    if (dispenserType.equals(additionalInfo)) {
                        dispenserCandidates.add(dispenser);
                    }
                }
                // Select closest dispenser
                if (dispenserCandidates.isEmpty()) {
                    return "x";
                } else {
                    Dispenser closestDispenser = Dispenser.getClosestDispenser(dispenserCandidates);
                    int x = closestDispenser.getRelativeCoordinate().getX();
                    int y = closestDispenser.getRelativeCoordinate().getY();
                    // TODO: improve this
                    if (y < 0) {
                        return "n";
                    }
                    if (y > 0) {
                        return "s";
                    }
                    if (x < 0) {
                        return "w";
                    }
                    if (x > 0) {
                        return "e";
                    }
                }
            }
        }
        return "x";
    }

    private boolean oppositeDirections(String direction1, String direction2) {
        if ((direction1.equals("n") && direction2.equals("s")) || (direction1.equals("s") && direction2.equals("n"))) {
            return true;
        }
        if ((direction1.equals("e") && direction2.equals("w")) || (direction1.equals("w") && direction2.equals("e"))) {
            return true;
        }
        return false;
    }
}
