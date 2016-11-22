package messages;

import java.util.ArrayList;

/**
 * Message class used by simulation managers to return result of simulation (i.e. log).
 * 
 * @author Rance Cleaveland
 *
 */
public class SimulationFinishMsg {
	
	private final ArrayList<Object> log;

	public SimulationFinishMsg(ArrayList<Object> log) {
		this.log = log;
	}

	public ArrayList<Object> getLog() {
		return log;
	}
}
