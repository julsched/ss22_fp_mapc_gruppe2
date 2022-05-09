package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.agents.g2utils.*;

import java.util.*;

/**
 * A very basic agent.
 */
public class AgentJulia extends Agent {

    private int lastActionID = -1;

    private int currentStep = -1;

    String lastAction = "";
    String lastActionResult = "";
    String blockRequested = "";
    // TODO: fill this with maximum step number of agent's current role
    int maxStepNum = 2;


    List<Percept> attachedThingsPercepts = new ArrayList<>();
    List<Dispenser> dispensers = new ArrayList<>();
    List<Block> blocks = new ArrayList<>();
    List<RelativeCoordinate> goalZoneFields = new ArrayList<>();
    List<Task> tasks = new ArrayList<>();
    List<Block> attachedBlocks = new ArrayList<>();


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
                    if (y != 0) {
                        return new Action("move", new Identifier("n"));
                    } else {
                        return new Action("move", new Identifier("w"));
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
        attachedThingsPercepts = new ArrayList<>();
        dispensers = new ArrayList<>();
        blocks = new ArrayList<>();
        goalZoneFields = new ArrayList<>();
        tasks = new ArrayList<>();
        attachedBlocks = new ArrayList<>();
        
        // Allocate percepts to variables
        for (Percept percept : percepts) {
            if (percept.getName().equals("step")) {
                Parameter param = percept.getParameters().get(0);
                currentStep = ((Numeral) param).getValue().intValue();
            }
            if (percept.getName().equals("actionID")) {
                Parameter param = percept.getParameters().get(0);
                if (param instanceof Numeral) {
                    int id = ((Numeral) param).getValue().intValue();
                    if (id > lastActionID) {
                        lastActionID = id;
                    }
                }
            }
            if (percept.getName().equals("lastAction")) {
                Parameter param = percept.getParameters().get(0);
                lastAction = ((Identifier) param).getValue();
            }
            if (percept.getName().equals("lastActionResult")) {
                Parameter param = percept.getParameters().get(0);
                lastActionResult = ((Identifier) param).getValue();
            }
            if (percept.getName().equals("thing")) {
                Parameter paramThingType = percept.getParameters().get(2);
                if (((Identifier) paramThingType).getValue().equals("dispenser")) {
                    Parameter paramCoordinateX = percept.getParameters().get(0);
                    Parameter paramCoordinateY = percept.getParameters().get(1);
                    Parameter paramType = percept.getParameters().get(3);

                    int x = ((Numeral) paramCoordinateX).getValue().intValue();
                    int y = ((Numeral) paramCoordinateY).getValue().intValue();
                    String type = ((Identifier) paramType).getValue();

                    Dispenser dispenser = new Dispenser(new RelativeCoordinate(x, y), type);
                    dispensers.add(dispenser);
                }
                if (((Identifier) paramThingType).getValue().equals("block")) {
                    Parameter paramCoordinateX = percept.getParameters().get(0);
                    Parameter paramCoordinateY = percept.getParameters().get(1);
                    Parameter paramBlockType = percept.getParameters().get(3);

                    int x = ((Numeral) paramCoordinateX).getValue().intValue();
                    int y = ((Numeral) paramCoordinateY).getValue().intValue();
                    String blockType = ((Identifier) paramBlockType).getValue();

                    Block block = new Block(new RelativeCoordinate(x, y), blockType);
                    blocks.add(block);
                }
            }
            if (percept.getName().equals("task")) {
                Parameter paramName = percept.getParameters().get(0);
                Parameter paramDeadline = percept.getParameters().get(1);
                Parameter paramReward = percept.getParameters().get(2);
                Parameter paramRequirements = percept.getParameters().get(3);

                String name = ((Identifier) paramName).getValue();
                int deadline = ((Numeral) paramDeadline).getValue().intValue();
                int reward = ((Numeral) paramReward).getValue().intValue();

                List<Parameter> params = new ArrayList<>();
                List<TaskRequirement> requirements = new ArrayList<>();
                for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
                    params.add(((ParameterList) paramRequirements).get(i));
                }
                for (var param : params) {
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
            }
            if (percept.getName().equals("attached")) {
                attachedThingsPercepts.add(percept);
            }
            if (percept.getName().equals("goalZone")) {
                Parameter paramCoordinateX = percept.getParameters().get(0);
                Parameter paramCoordinateY = percept.getParameters().get(1);

                int x = ((Numeral) paramCoordinateX).getValue().intValue();
                int y = ((Numeral) paramCoordinateY).getValue().intValue();

                RelativeCoordinate goalZoneField = new RelativeCoordinate(x, y);
                goalZoneFields.add(goalZoneField);
            }
        }
        for (Percept percept : attachedThingsPercepts) {
            Parameter paramCoordinateX = percept.getParameters().get(0);
            Parameter paramCoordinateY = percept.getParameters().get(1);

            int x = ((Numeral) paramCoordinateX).getValue().intValue();
            int y = ((Numeral) paramCoordinateY).getValue().intValue();
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
        List<String> allowedDirections = Arrays.asList("n", "e", "s", "w");
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
                String direction = randomDirections.get(0);
                String direction2 = randomDirections.get(1);
                return new Action("move", new Identifier(direction), new Identifier(direction2));
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
                    if (y != 0) {
                        return "n";
                    } else {
                        return "w";
                    }
                }
            }
        }
        return "x";
    }
}
