package massim.javaagents.agents.g2utils;

import java.util.*;

public class Task {

    private final String name;
    private final int deadline;
    private final int reward;
    private final List<TaskRequirement> requirements;

    public Task(String name, int deadline, int reward, List<TaskRequirement> requirements) {
        this.name = name;
        this.deadline = deadline;
        this.reward = reward;
        this.requirements = requirements;
    }

    public String getName() {
        return this.name;
    }

    public int getDeadline() {
        return this.deadline;
    }

    public int getReward() {
        return this.reward;
    }

    public List<TaskRequirement> getRequirements() {
        return this.requirements;
    }

    public boolean equals(Task task) {
        if (this.name.equals(task.getName())) {
            return true;
        } else {
            return false;
        }
    }
}
