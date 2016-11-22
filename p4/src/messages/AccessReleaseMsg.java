package messages;

import util.AccessRelease;
import akka.actor.ActorRef;
import enums.AccessType;

/**
 * Class of messages for releasing access to a resource.
 * 
 * @author Rance Cleaveland
 *
 */
public class AccessReleaseMsg {
	private final AccessRelease access_release;
	private final ActorRef sender;

	public AccessReleaseMsg(AccessRelease access_release, ActorRef sender) {
		this.access_release = access_release;
		this.sender = sender;
	}
	
	public AccessRelease getAccessRelease() {
		return access_release;
	}
	
	public ActorRef getSender() {
		return sender;
	}
	
	@Override 
	public String toString () {
		return "Releasing " + access_release.getType().toString() + " on " + access_release.getResourceName();
	}
}
