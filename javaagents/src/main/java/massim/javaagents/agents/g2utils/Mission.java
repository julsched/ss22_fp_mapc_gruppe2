package massim.javaagents.agents.g2utils;

import java.util.*;

public class Mission {
    
    private static int numOfExplorers = 1;
    private static List<String> explorers = new ArrayList<>();
    private static int numOfConstructors = 0; // Set according to turnier config (should equal number of goal zones)
    private static List<String> constructors = new ArrayList<>();


    public static boolean applyForExplorerMission(String agentName) {
        if (explorers.size() < numOfExplorers) {
            explorers.add(agentName);
            return true;
        } else {
            return false;
        }
    }

    public static boolean applyForConstructorMission(String agentName) {
        if (constructors.size() < numOfConstructors) {
            constructors.add(agentName);
            return true;
        } else {
            return false;
        }
    }

    public static void prepareForNextSimulation() {
        explorers.clear();
        constructors.clear();
    }
}
