package massim.javaagents.agents.g2utils;

import java.util.*;

public class Norm {

    private final String name;
    private final int firstStep;
    private final int lastStep;
    private final NormRequirement requirements;
    private final int punishment;

    public Norm(String name, int firstStep, int lastStep, NormRequirement requirements, int punishment) {
        this.name = name;
        this.firstStep = firstStep;
        this.lastStep = lastStep;
        this.requirements = requirements;
        this.punishment = punishment;
    }
    
    public NormRequirement getRequirements() {
    	return requirements;
    }
    
    public int getPunishment() {
    	return punishment;
    }
}
