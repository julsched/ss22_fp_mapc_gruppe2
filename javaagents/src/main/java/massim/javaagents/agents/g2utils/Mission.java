package massim.javaagents.agents.g2utils;

import java.util.*;

public class Mission {
    
    private static String explorer = "";

    public static String applyForExplorerMission(String agentName) {
        if (explorer.length() == 0) {
            explorer = agentName;
        }
        return agentName;
    }
}
