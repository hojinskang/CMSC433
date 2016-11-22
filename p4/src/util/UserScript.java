package util;

import java.util.ArrayList;

/**
 * Class of scripts run by user actors.
 * 
 * A script consists of an ArrayList of steps, where each step is an ArrayList of
 * AccessRequest / ManagementRequest objects.  The idea is that each request in step
 * should be sent, then the corresponding response received, before the next step is
 * executed.
 * 
 * @author Rance Cleaveland
 *
 */
public class UserScript {
	
	private ArrayList<ArrayList<Object>> script;  // List of steps
	
	/**
	 * Create empty script.
	 */
	public UserScript() {
		this.script = new ArrayList<ArrayList<Object>>();
	}

	/**
	 * Create script from list of steps.  List is not copied.
	 * 
	 * @param script	List of steps
	 */
	public UserScript(ArrayList<ArrayList<Object>> script) {	
		this.script = script;
	}
	
	/**
	 * Return list of steps in script.
	 * 
	 * @return	List of steps
	 */
	private ArrayList<ArrayList<Object>> getScript() {
		return script;
	}
	
	/**
	 * Given list of requests, return script of sequence of steps, each step
	 * containing a single request.
	 * 
	 * @param msgSeq	List of requests
	 * @return			Script in which each step contains a single request
	 */
	public static UserScript makeSequential (ArrayList<Object> msgSeq) {
		ArrayList<ArrayList<Object>> newScript = new ArrayList<ArrayList<Object>>();
		for (Object m : msgSeq) {
			ArrayList<Object> step = new ArrayList<Object>();
			step.add(m);
			newScript.add(step);
		}
		return new UserScript(newScript);
	}
	
	/**
	 * Give list of requests, return script containing single step of all requests.
	 * @param msgs	List of messages
	 * @return		Script containing single step of (clone of) list of messages.
	 */
	public static UserScript makeConcurrent (ArrayList<Object> msgs) {
		ArrayList<ArrayList<Object>> newScript = new ArrayList<ArrayList<Object>>();
		newScript.add((ArrayList<Object>)msgs.clone());
		return new UserScript (newScript);
	}
	
	/**
	 * Form new script by concatenating second script onto end of first one.
	 * 
	 * @param s1	First script in concatenation
	 * @param s2	Second script in concatenation
	 * @return		Concatenated script
	 */
	public static UserScript concatenate (UserScript s1, UserScript s2) {
		ArrayList<ArrayList<Object>> newScript = new ArrayList<ArrayList<Object>>();
		newScript.addAll(s1.getScript());
		newScript.addAll(s2.getScript());
		return new UserScript(newScript);
	}
	
	/**
	 * Determines is script has no steps in it.
	 * 
	 * @return	Boolean indicating if script is finished
	 */
	public boolean isDone() {
		return script.isEmpty();
	}
	
	/**
	 * Return first step in a script, if script is non-empty.
	 * 
	 * @return	First step
	 * @throws Exception	Thrown if script has no steps
	 */
	public ArrayList<Object> firstStep () throws Exception {
		if (isDone()) {
			throw new Exception ("Empty script");
		}
		else {
			return ((ArrayList<Object>) script.get(0).clone());
		}
	}
	
	/**
	 * Returns script minus first step, if script is non-empty.
	 * @return	Rest of script, minus first step
	 * @throws Exception	Thrown is script has no steps
	 */
	public UserScript rest() throws Exception {
		if (isDone()) {
			throw new Exception ("Empty script");
		}
		else {			
			return new UserScript (new ArrayList<ArrayList<Object>> (script.subList(1, script.size())));
		}
	}
}
