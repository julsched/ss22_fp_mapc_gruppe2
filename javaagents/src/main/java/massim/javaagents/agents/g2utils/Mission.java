package massim.javaagents.agents.g2utils;

import java.util.*;

public class Mission {
    
    private static String explorer = "";
    
 //   private static int counter = 0;

    public static String applyForExplorerMission(String agentName) {
        if (explorer.length() == 0) {
            explorer = agentName;
        }
        /*
        if (counter > 3) {
        	counter = counter + 1;
        	return agentName;
        }
        */
        return explorer;
    }
}
