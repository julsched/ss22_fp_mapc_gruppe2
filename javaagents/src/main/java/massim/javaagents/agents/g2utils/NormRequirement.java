package massim.javaagents.agents.g2utils;

import java.util.*;

public class NormRequirement {

    private final String type;
    private final String name;
    private final int quantity;
    private final String details;

    public NormRequirement(String type, String name, int quantity, String details) {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
        this.details = details;
    }
}
