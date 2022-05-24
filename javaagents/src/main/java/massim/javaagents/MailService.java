package massim.javaagents;

import eis.iilang.Percept;
import massim.javaagents.agents.Agent;
import massim.javaagents.agents.AgentG2;
import massim.javaagents.agents.g2utils.Cell;
import massim.javaagents.agents.g2utils.RelativeCoordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * A simple register for agents that forwards messages.
 */
public class MailService {

    private Map<String, Agent> register = new HashMap<>();
    private Map<String, List<Agent>> agentsByTeam = new HashMap<>();
    private Map<String, String> teamForAgent = new HashMap<>();
    private Logger logger = Logger.getLogger("agents");

    /**
     * Registers an agent with this mail service. The agent will now receive messages.
     * @param agent the agent to register
     * @param team the agent's team (needed for broadcasts)
     */
    void registerAgent(Agent agent, String team){
        register.put(agent.getName(), agent);
        agentsByTeam.putIfAbsent(team, new Vector<>());
        agentsByTeam.get(team).add(agent);
        teamForAgent.put(agent.getName(), team);
    }

    /**
     * Adds a message to this mailbox.
     * @param message the message to add
     * @param to the receiving agent
     * @param from the agent sending the message
     */
    public void sendMessage(Percept message, String to, String from){

        Agent recipient = register.get(to);

        if(recipient == null) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        }
        else{
            recipient.handleMessage(message, from);
        }
    }

    /**
     * Sends a message to all agents of the sender's team (except the sender).
     * @param message the message to broadcast
     * @param sender the sending agent
     */
    public void broadcast(Percept message, String sender) {
        agentsByTeam.get(teamForAgent.get(sender)).stream()
                .map(Agent::getName)
                .filter(ag -> !ag.equals(sender))
                .forEach(ag -> sendMessage(message, ag, sender));
    }
    
    public void requestMap(String to, String from){

        Agent recipient = register.get(to);

        if(recipient == null && !(recipient instanceof AgentG2)) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        }
        else{
        	AgentG2 rec = (AgentG2) recipient;
        	rec.deliverMap(from);
        }
    }
    
    public void deliverMap(String to, String from, HashMap<RelativeCoordinate, Cell> map, RelativeCoordinate currentPosition) {
    	
    	Agent recipient = register.get(to);
        if (recipient == null && !(recipient instanceof AgentG2)) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        } else {
        	AgentG2 rec = (AgentG2) recipient;
        	rec.handleMap(from, map, currentPosition);
        }
    }
}