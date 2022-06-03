package massim.javaagents.agents.g2utils;

// neue Klasse
public class AgentInformation {
	
	private RelativeCoordinate rc;
	private String name;
	private String role;
	private int energy;
	
	public AgentInformation(RelativeCoordinate position) {
		this.rc = position;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setRole(String role) {
		this.role = role;
	}
	
	public void setEnergy(int energy) {
		this.energy = energy;
	}
	
	public String getName() {
		return name;
	}
	
	public RelativeCoordinate getRelativeCoordinate() {
		return rc;
	}

}
