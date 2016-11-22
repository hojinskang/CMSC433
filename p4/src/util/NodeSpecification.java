package util;

import java.util.ArrayList;

/**
 * Class of specifications of nodes in resource-management system.
 * 
 * A node consists of a list of resource local to the node, and scripts to be run
 * by users local to the node.  The idea is that each node will have a single
 * resource manager that manages the resources and processes requests from users,
 * each of which will be running one script.
 * 
 * @author Rance Cleaveland
 *
 */
public class NodeSpecification {
	private final ArrayList<Resource> resources;
	private final ArrayList<UserScript> userScripts;
	
	public NodeSpecification (ArrayList<Resource> resources, ArrayList<UserScript> userScripts) {
		this.resources = resources;
		this.userScripts = userScripts;
	}

	public ArrayList<Resource> getResources() {
		return resources;
	}

	public ArrayList<UserScript> getUserScripts() {
		return userScripts;
	}
}
