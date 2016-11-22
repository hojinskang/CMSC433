package messages;

import java.util.ArrayList;

import akka.actor.ActorRef;

/**
 * Class of messages requesting addition of remote managers to given resource manager.
 * The list of managers in the message may contain the recipient manager also.  The list
 * also should not be modified.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddRemoteManagersRequestMsg {

	private final ArrayList<ActorRef> managerList;
	
	public AddRemoteManagersRequestMsg (ArrayList<ActorRef> managers) {
		this.managerList = managers;
	}

	public ArrayList<ActorRef> getManagerList() {
		return managerList;
	}
	
}
