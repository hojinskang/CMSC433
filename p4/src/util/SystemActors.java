package util;

import java.util.ArrayList;

import akka.actor.ActorRef;

/**
 * Class of different actors in a resource-manage system
 * 
 * @author Rance Cleaveland
 *
 */
public class SystemActors {
	
	private final ArrayList<ActorRef> resourceManagers;
	private final ArrayList<ActorRef> users;
	
	public SystemActors (ArrayList<ActorRef> resourceManagers, ArrayList<ActorRef> users) {
		this.resourceManagers = resourceManagers;
		this.users = users;
	}

	public ArrayList<ActorRef> getResourceManagers() {
		return resourceManagers;
	}

	public ArrayList<ActorRef> getUsers() {
		return users;
	}

}
