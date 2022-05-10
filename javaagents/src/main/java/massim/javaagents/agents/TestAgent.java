package massim.javaagents.agents;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.String;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.List;

public class TestAgent extends Agent {
	private String lastMoveDir = "";
	private HashMap<String, ArrayList<int[]>> inSight;

	public TestAgent(String name, MailService mailbox) {
		super(name, mailbox);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handlePercept(Percept percept) {

	}

	// Goes through all percepts and saves interesting information about the current
	// state of the field and Agent
	public void workThroughPercepts() {
		Identifier lastAction = null;
		Identifier lastActionResult = null;
		List<Parameter> lastActionParams = null;
		this.inSight = new HashMap<String, ArrayList<int[]>>();
		inSight.put("obstacle", new ArrayList<>());
		inSight.put("entity", new ArrayList<>());
		List<Percept> percepts = getPercepts();

		for (Percept percept : percepts) {

			// Remember last Action, last Action result and corresponding Parameters
			if (percept.getName().equals("lastAction")) {
				lastAction = (Identifier) percept.getParameters().get(0);
			}
			if (percept.getName().equals("lastActionResult")) {
				lastActionResult = (Identifier) percept.getParameters().get(0);
			}
			if (percept.getName().equals("lastActionParams")) {
				lastActionParams = percept.getParameters();
			}

			// save location of things for further calculations
			if (percept.getParameters().size() >= 3) {
				if (percept.getName().equals("thing")) {
					if ((percept.getParameters().get(2)) instanceof Identifier) {
						String identifier = ((Identifier) (percept.getParameters().get(2))).getValue();

						Parameter x = percept.getParameters().get(0);
						Parameter y = percept.getParameters().get(1);
						if (x instanceof Numeral && y instanceof Numeral) {
							int xInt = ((Numeral) x).getValue().intValue();
							int yInt = ((Numeral) y).getValue().intValue();
							if (identifier.equals("obstacle")) {
								inSight.get("obstacle").add(new int[] { xInt, yInt });
							} else if (identifier.equals("entity")) {
								inSight.get("entity").add(new int[] { xInt, yInt });
							}
						}
					}
				}
			}
		}
		// Set last move direction, if last move was successful
		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
			if (lastAction.getValue().equals("move") && lastActionResult.getValue().equals("success")) {
				String lastParamsString = lastActionParams.get(0) + "";
				lastParamsString = lastParamsString.replace("[", "");
				lastParamsString = lastParamsString.replace("]", "");
				this.lastMoveDir = lastParamsString;
			}
		}

	}

	// returns an arraylist with all 4 directions
	private ArrayList<String> setAllDirs() {
		ArrayList<String> possibleDirs = new ArrayList<String>();
		possibleDirs.add("n");
		possibleDirs.add("e");
		possibleDirs.add("s");
		possibleDirs.add("w");
		return possibleDirs;
	}

	@Override
	public Action step() {
		workThroughPercepts();
		ArrayList<String> possibleDirs = getPossibleDirs();
		ArrayList<String> prefDirs = getPreferredDirs();
		if (possibleDirs.size() != 0) {
			if (prefDirs != null) {
				// flip coin
				// return new Action("move", new Identifier(dir));
				return null;
			}
			if (possibleDirs.size() > 1) { // remove Dir where you came from
				if (oppositeDir(lastMoveDir) != null) {
					possibleDirs.remove(oppositeDir(lastMoveDir));
				}
			}
			String randDir = random(possibleDirs);
			return new Action("move", new Identifier(randDir));
		}
		return null;

	}

	private Object oppositeDir(String dir) {
		switch (dir) {
		case "n":
			return "s";
		case "e":
			return "w";
		case "s":
			return "n";
		case "w":
			return "e";
		default:
			return null;

		}
	}

	private String random(ArrayList<String> possibleDirs) {
		int randIndex = (int) (Math.random() * possibleDirs.size());
		return possibleDirs.get(randIndex);
	}

	private ArrayList<String> getPreferredDirs() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<String> getPossibleDirs() {
		ArrayList<String> possibleDirs = setAllDirs();
		ArrayList<int[]> obstacleLocations = this.inSight.get("obstacle");
		// Don't walk towards obstacle
		for (int i = 0; i < obstacleLocations.size(); i++) {
			int[] obstacle = obstacleLocations.get(i);
			if (obstacle[0] == 0) {
				if (obstacle[1] > 0) {
					possibleDirs.remove("s");
				}
				if (obstacle[1] < 0) {
					possibleDirs.remove("n");
				}
			}
			if (obstacle[1] == 0) {
				if (obstacle[0] > 0) {
					possibleDirs.remove("e");
				}
				if (obstacle[0] < 0) {
					possibleDirs.remove("w");
				}
			}
		}

		ArrayList<int[]> entityLocations = this.inSight.get("entity");
		// Don't walk towards other agent if he's next to you
		for (int i = 0; i < entityLocations.size(); i++) {
			int[] entity = entityLocations.get(i);
			if (entity[0] == 0) {
				if (entity[1] > 0) {
					possibleDirs.remove("s");
				}
				if (entity[1] < 0) {
					possibleDirs.remove("n");
				}
			}
			if (entity[1] == 0) {
				if (entity[0] > 0) {
					possibleDirs.remove("e");
				}
				if (entity[0] < 0) {
					possibleDirs.remove("w");
				}
			}
		}
		return possibleDirs;
	}

	@Override
	public void handleMessage(Percept message, String sender) {
		// System.out.println(message);

	}

}
