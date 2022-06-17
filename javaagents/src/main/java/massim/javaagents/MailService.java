package massim.javaagents;

import eis.iilang.Percept;
import massim.javaagents.agents.Agent;
import massim.javaagents.agents.AgentG2;
import massim.javaagents.agents.g2utils.Block;
import massim.javaagents.agents.g2utils.Cell;
import massim.javaagents.agents.g2utils.Dispenser;
import massim.javaagents.agents.g2utils.Goalzone;
import massim.javaagents.agents.g2utils.MapBundle;
import massim.javaagents.agents.g2utils.MapManagement;
import massim.javaagents.agents.g2utils.Obstacle;
import massim.javaagents.agents.g2utils.RelativeCoordinate;
import massim.javaagents.agents.g2utils.Rolezone;

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
    
    public void broadcastMapRequest(int currentStep, String sender) {
        agentsByTeam.get(teamForAgent.get(sender)).stream()
                .map(Agent::getName)
                .filter(ag -> !ag.equals(sender))
                .forEach(ag -> requestMap(ag, sender, currentStep));
    }
    
    public void requestMap(String to, String from, int currentStep){

        Agent recipient = register.get(to);

        if(recipient == null && !(recipient instanceof AgentG2)) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        } else {
        	AgentG2 rec = (AgentG2) recipient;
        	rec.deliverMap(from, currentStep);
        }
    }
    
    public void deliverMap(String to, MapBundle mapBundle) {
    	
    	Agent recipient = register.get(to);
        if (recipient == null && !(recipient instanceof AgentG2)) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        } else {
        	AgentG2 rec = (AgentG2) recipient;
        	rec.handleMap(mapBundle);
        }
    }
    
    // sendet eine geupdatete Map
    public void sendMap(String to, MapManagement mapManager, RelativeCoordinate toPosition) {
    	Agent recipient = register.get(to);
        if (recipient == null && !(recipient instanceof AgentG2)) {
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        } else {
        	AgentG2 rec = (AgentG2) recipient;
        	rec.receiveMap(mapManager, toPosition);
        }
    }
}