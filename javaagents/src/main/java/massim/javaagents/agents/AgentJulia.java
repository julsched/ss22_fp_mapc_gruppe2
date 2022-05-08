package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;

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


    Percept actionIDpercept = null;
    List<Percept> thingPercepts = new ArrayList<>();
    List<Percept> taskPercepts = new ArrayList<>();
    List<Percept> attachedThingsPercepts = new ArrayList<>();
    List<Percept> goalZonePercepts = new ArrayList<>();
    List<Map> dispensers = new ArrayList<>();
    List<Map> blocks = new ArrayList<>();
    List<Map> goalZoneFields = new ArrayList<>();
    List<Map> tasks = new ArrayList<>();
    List<Map> attachedThings = new ArrayList<>();


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
        if (!blockRequested.isEmpty()) {
            String direction = blockRequested;
            blockRequested = "";
            return new Action("attach", new Identifier(direction));
        }
        // This only works for tasks with one block
        if (checkIfBlockAttached()) {
            if (!checkIfTaskComplete(tasks.get(0))) {
                return new Action("rotate", new Identifier("cw"));
            }
            if (taskSubmissionPossible(tasks.get(0))) {
                String taskName = (String) tasks.get(0).get("name");
                return new Action("submit", new Identifier(taskName));
            }
            // Walk to goal zone
            // TODO: look for closest goal zone
            if (!goalZoneFields.isEmpty()) {
                for (Map goalZoneField : goalZoneFields) {
                    int x = (Integer) goalZoneField.get("x");
                    int y = (Integer) goalZoneField.get("y");
                    if (y != 0) {
                        return new Action("move", new Identifier("n"));
                    } else {
                        return new Action("move", new Identifier("w"));
                    }
                } 
            }
            return moveRandomly(2);
        }
        // At the moment limited to one type of dispenser/block ('b0')
        String result = checkIfDispenserNext("b0");
        if (!result.equals("x")) {
            return requestBlock(result);
        }
        String dispenserDirection = lookFor("dispenser", "b0");
        if (!dispenserDirection.equals("x")) {
            return new Action("move", new Identifier(dispenserDirection));
        }
        return moveRandomly(2);
    }

    private void sortPercepts(List<Percept> percepts) {
        // Delete previous percepts
        lastAction = "";
        lastActionResult = "";
        actionIDpercept = null;
        thingPercepts = new ArrayList<>();
        taskPercepts = new ArrayList<>();
        attachedThingsPercepts = new ArrayList<>();
        goalZonePercepts = new ArrayList<>();
        dispensers = new ArrayList<>();
        blocks = new ArrayList<>();
        goalZoneFields = new ArrayList<>();
        tasks = new ArrayList<>();
        attachedThings = new ArrayList<>();
        
        // Allocate percepts to variables
        for (Percept percept : percepts) {
            if (percept.getName().equals("step")) {
                Parameter param = percept.getParameters().get(0);
                currentStep = ((Numeral) param).getValue().intValue();
            }
            if (percept.getName().equals("actionID")) {
                actionIDpercept = percept;
                Parameter param = actionIDpercept.getParameters().get(0);
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
                thingPercepts.add(percept);
                Parameter paramType = percept.getParameters().get(2);
                if (((Identifier) paramType).getValue().equals("dispenser")) {
                    Parameter paramCoordinateX = percept.getParameters().get(0);
                    Parameter paramCoordinateY = percept.getParameters().get(1);
                    Parameter paramBlockType = percept.getParameters().get(3);

                    int coordinateX = ((Numeral) paramCoordinateX).getValue().intValue();
                    int coordinateY = ((Numeral) paramCoordinateY).getValue().intValue();
                    String blockType = ((Identifier) paramBlockType).getValue();

                    Map dispenser = new HashMap();
                    dispenser.put("x", coordinateX);
                    dispenser.put("y", coordinateY);
                    dispenser.put("type", blockType);
                    dispensers.add(dispenser);
                }
                if (((Identifier) paramType).getValue().equals("block")) {
                    Parameter paramCoordinateX = percept.getParameters().get(0);
                    Parameter paramCoordinateY = percept.getParameters().get(1);
                    Parameter paramBlockType = percept.getParameters().get(3);

                    int coordinateX = ((Numeral) paramCoordinateX).getValue().intValue();
                    int coordinateY = ((Numeral) paramCoordinateY).getValue().intValue();
                    String blockType = ((Identifier) paramBlockType).getValue();

                    Map block = new HashMap();
                    block.put("x", coordinateX);
                    block.put("y", coordinateY);
                    block.put("type", blockType);
                    blocks.add(block);
                }
            }
            if (percept.getName().equals("task")) {
                taskPercepts.add(percept);
                Parameter paramName = percept.getParameters().get(0);
                Parameter paramDeadline = percept.getParameters().get(1);
                Parameter paramReward = percept.getParameters().get(2);
                Parameter paramRequirements = percept.getParameters().get(3);

                String name = ((Identifier) paramName).getValue();
                int deadline = ((Numeral) paramDeadline).getValue().intValue();
                int reward = ((Numeral) paramReward).getValue().intValue();
                
                Map task = new HashMap();
                task.put("name", name);
                task.put("deadline", deadline);
                task.put("reward", reward);

                List<Parameter> params = new ArrayList<>();
                List<Map> requirements = new ArrayList<>();
                for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
                    params.add(((ParameterList) paramRequirements).get(i));
                }
                for (var param : params) {
                    Parameter paramCoordinateX = ((Function)param).getParameters().get(0);
                    Parameter paramCoordinateY = ((Function)param).getParameters().get(1);
                    Parameter paramBlockType = ((Function)param).getParameters().get(2);

                    int coordinateX = ((Numeral) paramCoordinateX).getValue().intValue();
                    int coordinateY = ((Numeral) paramCoordinateY).getValue().intValue();
                    String blockType = ((Identifier) paramBlockType).getValue();

                    Map requirement = new HashMap();
                    requirement.put("x", coordinateX);
                    requirement.put("y", coordinateY);
                    requirement.put("type", blockType);
                    requirements.add(requirement);
                }
                task.put("requirements", requirements);
                tasks.add(task);
            }
            if (percept.getName().equals("attached")) {
                attachedThingsPercepts.add(percept);
            }
            if (percept.getName().equals("goalZone")) {
                goalZonePercepts.add(percept);
                Parameter paramCoordinateX = percept.getParameters().get(0);
                Parameter paramCoordinateY = percept.getParameters().get(1);

                int coordinateX = ((Numeral) paramCoordinateX).getValue().intValue();
                int coordinateY = ((Numeral) paramCoordinateY).getValue().intValue();

                Map goalZoneField = new HashMap();
                goalZoneField.put("x", coordinateX);
                goalZoneField.put("y", coordinateY);
                goalZoneFields.add(goalZoneField);
            }
        }
        for (Percept percept : attachedThingsPercepts) {
            Parameter paramCoordinateX = percept.getParameters().get(0);
            Parameter paramCoordinateY = percept.getParameters().get(1);

            int coordinateX = ((Numeral) paramCoordinateX).getValue().intValue();
            int coordinateY = ((Numeral) paramCoordinateY).getValue().intValue();

            Map attachedThing = new HashMap();
            attachedThing.put("x", coordinateX);
            attachedThing.put("y", coordinateY);
            for (Map block : blocks) {
                int x = (Integer) block.get("x");
                int y = (Integer) block.get("y");
                if (x == coordinateX && y == coordinateY) {
                    String blockType = (String) block.get("type");
                    attachedThing.put("type", blockType);
                }
            }
            // Same should be done with entity and obstacle once variables are implemented
            if (!attachedThing.containsKey("type")) {
                attachedThing.put("type", null);
            }
            attachedThings.add(attachedThing);
        }
    }

    /**
    Checks in clockwise-sequence if there is a dispenser of the required block type directly next to the agent
    @param blockType block type of dispenser agent is looking for
    @return direction of the dispenser or 'x' if no dispenser of the required block type next to the agent
     */
    private String checkIfDispenserNext(String blockType) {
        if (!dispensers.isEmpty()) {
            for (Map dispenser : dispensers) {
                String type = (String) dispenser.get("type");
                if (type.equals(blockType)) {
                    int x = (Integer) dispenser.get("x");
                    int y = (Integer) dispenser.get("y");
                    if (x == 0 && y == -1) {
                        return "n";
                    }
                    if (x == 1 && y == 0) {
                        return "e";
                    }
                    if (x == 0 && y == 1) {
                        return "s";
                    }
                    if (x == -1 && y == 0) {
                        return "w";
                    }
                }  
            } 
        }
        return "x";
    }

    private boolean checkIfBlockAttached() {
        for (Map attachedThing : attachedThings) {
            String type = (String) attachedThing.get("type");
            if (type != null) {
                if (type.startsWith("b")) {
                    return true;
                }
            } 
        }
        return false;
    }

    private Action requestBlock(String direction) {
        blockRequested = direction;
        return new Action("request", new Identifier(direction));
    }

    private Action moveRandomly(int stepNum) {
        List<String> directions = Arrays.asList("n", "e", "s", "w");
        Random rand = new Random();
        List<String> randomDirections = new ArrayList<>();
        for (int i = 1; i <= stepNum; i++) {
            String randomDirection = directions.get(rand.nextInt(directions.size()));
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

    private boolean checkIfTaskComplete(Map task) {
        // Check if all required blocks are attached
        List<Map> requirements = (List) task.get("requirements");
        for (Map requirement : requirements) {
            boolean requirementFulfilled = false;
            int xR = (Integer) requirement.get("x");
            int yR = (Integer) requirement.get("y");
            String blockTypeR = (String) requirement.get("type");

            for (Map attachedThing : attachedThings) {
                int xA = (Integer) attachedThing.get("x");
                int yA = (Integer) attachedThing.get("y");
                String blockTypeA = (String) attachedThing.get("type");

                if (xR == xA && yR == yA && blockTypeR.equals(blockTypeA)) {
                    requirementFulfilled = true;
                    break;
                }
            }
            if (requirementFulfilled == false) {
                return false;
            }
        }

        // Check if all attached blocks are really needed
        for (Map attachedThing : attachedThings) {
            boolean thingIsRequired = false;
            int xA = (Integer) attachedThing.get("x");
            int yA = (Integer) attachedThing.get("y");
            String blockTypeA = (String) attachedThing.get("type");

            for (Map requirement : requirements) {
                int xR = (Integer) requirement.get("x");
                int yR = (Integer) requirement.get("y");
                String blockTypeR = (String) requirement.get("type");

                if (xR == xA && yR == yA && blockTypeR.equals(blockTypeA)) {
                    thingIsRequired = true;
                    break;
                }
            }
            if (thingIsRequired == false) {
                return false;
            }
        }
        return true;
    }

    private boolean agentIsLocked() {
        // TODO
        return false;
    }

    private boolean taskSubmissionPossible(Map task) {
        if (!checkIfTaskComplete(task)) {
            return false;
        }
        List<Map> requirements = (List) task.get("requirements");
        for (Map requirement : requirements) {
            int xR = (Integer) requirement.get("x");
            int yR = (Integer) requirement.get("y");

            boolean inGoalZone = false;
            for (Map goalZoneField : goalZoneFields) {
                int xG = (Integer) goalZoneField.get("x");
                int yG = (Integer) goalZoneField.get("y");

                if (xR == xG && yR == yG) {
                    inGoalZone = true;
                    break;
                }
            }
            if (!inGoalZone) {
                return false;
            }
        }
        for (Map goalZoneField : goalZoneFields) {
            int xG = (Integer) goalZoneField.get("x");
            int yG = (Integer) goalZoneField.get("y");
            if (xG == 0 && yG == 0) {
                return true;
            }
        }
        return false;
    }

    private String lookFor(String type, String additionalInfo) {
        switch(type) {
            case "dispenser" -> {
                // TODO: Add functionality to look for CLOSEST dispenser rather than any dispenser
                for (Map dispenser : dispensers) {
                    String dispenserType = (String) dispenser.get("type");
                    if (dispenserType.equals(additionalInfo)) {
                        int x = (Integer) dispenser.get("x");
                        int y = (Integer) dispenser.get("y");
                        if (y != 0) {
                            return "n";
                        } else {
                            return "w";
                        }
                    }
                }
            }
        }
        return "x";
    }
}
