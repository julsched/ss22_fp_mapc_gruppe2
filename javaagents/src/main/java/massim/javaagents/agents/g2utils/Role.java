package massim.javaagents.agents.g2utils;

import java.util.*;

public class Role {

	private final String name;
	private final int vision;
	private final List<String> actions;
	private final List<Integer> speeds;
	private final double clearChance;
	private final int clearMaxDist;
	private final List<Block> attachedBlocks;

	public static Role getRole(List<Role> roles, String roleName) {
		for (Role role : roles) {
			if (role.getName().equals(roleName)) {
				return role;
			}
		}
		return null;
	}

	public Role(String name, int vision, List<String> actions, List<Integer> speeds, double clearChance,
			int clearMaxDist, List<Block> attachedBlocks) {
		this.name = name;
		this.vision = vision;
		this.actions = actions;
		this.speeds = speeds;
		this.clearChance = clearChance;
		this.clearMaxDist = clearMaxDist;
		this.attachedBlocks = attachedBlocks; // TODO expand for attached entities/obstacles
	}

	public String getName() {
		return this.name;
	}

	public int getVision() {
		return this.vision;
	}

	public List<String> getActions() {
		return this.actions;
	}

	public List<Integer> getSpeeds() {
		return this.speeds;
	}

	public Integer getCurrentSpeed() {
		if (speeds.size() == 0) {
			return null;
		}
		int numOfAttachedThings = attachedBlocks.size();
		if (speeds.size() > numOfAttachedThings) {
			return speeds.get(numOfAttachedThings);
		} else {
			return speeds.get(speeds.size() - 1);
		}
	}

	public double getClearChance() {
		return this.clearChance;
	}

	public int getClearMaxDist() {
		return this.clearMaxDist;
	}

	public boolean equals(Role role) {
		if (this.name.equals(role.getName())) {
			return true;
		} else {
			return false;
		}
	}
}
