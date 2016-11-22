package messages;

import java.util.ArrayList;

import akka.actor.ActorRef;

/**
 * Message requesting addition of local users to a resource manager.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddLocalUsersRequestMsg {
	private final ArrayList<ActorRef> localUsers;
	
	public AddLocalUsersRequestMsg (ArrayList<ActorRef> localUsers) {
		this.localUsers = localUsers;
	}

	public ArrayList<ActorRef> getLocalUsers() {
		return localUsers;
	}
	
}
