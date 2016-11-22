package messages;

import java.util.ArrayList;

/**
 * Class of messages to start logging.
 * 
 * @author Rance Cleaveland
 *
 */
public class LogResultMsg {
	
	private final ArrayList<Object> log;

	public LogResultMsg(ArrayList<Object> log) {
		this.log = log;
	}

	public ArrayList<Object> getLog() {
		return log;
	}
}
