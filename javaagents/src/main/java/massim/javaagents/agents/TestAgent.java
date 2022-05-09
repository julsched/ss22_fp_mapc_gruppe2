package massim.javaagents.agents;

import java.util.Random;
import java.util.ArrayList;
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

	}

	@Override
	public Action step() {
		ArrayList<String> possibleDirs = getPossibleDirs();
		//System.out.println(possibleDirs);
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
		return null;/**
					 * if (getName().contains("B")) { List<Percept> percepts = getPercepts(); for
					 * (Percept percept : percepts) { if (percept.getName().equals("thing")) {
					 * Parameter x = percept.getParameters().get(0); Parameter y =
					 * percept.getParameters().get(1); if (x instanceof Numeral && y instanceof
					 * Numeral) { int xInt = ((Numeral) param).getValue().intValue(); int yInt =
					 * ((Numeral) param).getValue().intValue();
					 * 
					 * System.out.println("THIS IS THE PERCEPT!"+getName()+getName().contains("B")+
					 * " "+percept.getParameters().get(0)+ "== 0? "+ (num==0)); }}}}/** if
					 * (percept.getName().equals("actionID")) { Parameter param =
					 * percept.getParameters().get(0); if (param instanceof Numeral) { int id =
					 * ((Numeral) param).getValue().intValue(); if (id > lastID) { lastID = id;
					 * return new Action("move", new Identifier("n")); } }
					 **/
		/**
		 * } } Random r = new Random(); int rand = r.nextInt(3); String dir = "";
		 * switch(rand) { case 0: dir = "n"; break; case 1: dir = "e"; break; case 2:
		 * dir = "s"; break; case 3: dir = "w"; break; } try{Thread.sleep(5000);}
		 * catch(Exception e) {} System.out.println("moving in Direction " +dir); return
		 * new Action("move", new Identifier(dir));
		 **/
		// return null;
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
		//System.out.println("Getting Random number " + randIndex);
		return possibleDirs.get(randIndex);
	}

	private ArrayList<String> getPreferredDirs() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<String> getPossibleDirs() {
		Identifier lastAction = null;
		Identifier lastActionResult = null;
		List<Parameter> lastActionParams = null;
		ArrayList<String> possibleDirs = new ArrayList<String>();
		possibleDirs.add("n");
		possibleDirs.add("e");
		possibleDirs.add("s");
		possibleDirs.add("w");
		//System.out.println("possible Dirs before elimination: " + possibleDirs);
		List<Percept> percepts = getPercepts();
		for (Percept percept : percepts) {
			if (percept.getName().equals("lastAction")) {
				lastAction = (Identifier) percept.getParameters().get(0);
			}
			if (percept.getName().equals("lastActionResult")) {
				lastActionResult = (Identifier) percept.getParameters().get(0);
			}
			if (percept.getName().equals("lastActionParams")) {
				lastActionParams = percept.getParameters();
			}
			if (percept.getParameters().size() >= 3) {
				if (percept.getName().equals("thing")) {
					if ((percept.getParameters().get(2)) instanceof Identifier) {
						String identifier = ((Identifier) (percept.getParameters().get(2))).getValue();
						if (identifier.equals("obstacle")) {
							Parameter x = percept.getParameters().get(0);
							Parameter y = percept.getParameters().get(1);
							if (x instanceof Numeral && y instanceof Numeral) {
								int xInt = ((Numeral) x).getValue().intValue();
								int yInt = ((Numeral) y).getValue().intValue();
								if (xInt == 0) {
									if (yInt > 0) {
										possibleDirs.remove("s");
									}
									if (yInt < 0) {
										possibleDirs.remove("n");
									}
								}
								if (yInt == 0) {
									if (xInt > 0) {
										possibleDirs.remove("e");
									}
									if (xInt < 0) {
										possibleDirs.remove("w");
									}
								}
								//System.out.println("impossible Dirs removed, possible Dirs: " + possibleDirs);

							}
						}
						else if (identifier.equals("entity")) {
						Parameter x = percept.getParameters().get(0);
							Parameter y = percept.getParameters().get(1);
							if (x instanceof Numeral && y instanceof Numeral) {
								int xInt = ((Numeral) x).getValue().intValue();
								int yInt = ((Numeral) y).getValue().intValue();
								if (xInt == 0) {
									if (yInt == 1) {
										possibleDirs.remove("s");
									}
									if (yInt ==-1) {
										possibleDirs.remove("n");
									}
								}
								if (yInt == 0) {
									if (xInt == 1) {
										possibleDirs.remove("e");
									}
									if (xInt == -1) {
										possibleDirs.remove("w");
									}
								}
								//System.out.println("impossible Dirs removed, possible Dirs: " + possibleDirs);

							}
						}
					}
				}
			}
		}
		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
			if (lastAction.getValue().equals("move") && lastActionResult.getValue().equals("success")) {
				String lastParamsString = lastActionParams.get(0) + "";
				lastParamsString = lastParamsString.replace("[", "");
				lastParamsString = lastParamsString.replace("]", "");
				this.lastMoveDir = lastParamsString;
			}
		}
		return possibleDirs;
	}

	@Override
	public void handleMessage(Percept message, String sender) {
		// System.out.println(message);

	}

}
