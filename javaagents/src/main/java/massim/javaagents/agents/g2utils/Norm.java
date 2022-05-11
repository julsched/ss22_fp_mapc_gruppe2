package massim.javaagents.agents.g2utils;

import java.util.*;

public class Norm {

    private final String name;
    private final int firstStep;
    private final int lastStep;
    private final List<NormRequirement> requirements;
    private final int punishment;

    public Norm(String name, int firstStep, int lastStep, List<NormRequirement> requirements, int punishment) {
        this.name = name;
        this.firstStep = firstStep;
        this.lastStep = lastStep;
        this.requirements = requirements;
        this.punishment = punishment;
    }
}
