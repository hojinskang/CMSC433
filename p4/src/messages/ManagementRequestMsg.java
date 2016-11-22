package messages;

import akka.actor.ActorRef;
import util.ManagementRequest;

/**
 * Class of messages for making management requests to a resource.
 * 
 * @author Rance Cleaveland
 *
 */
public class ManagementRequestMsg {
	private final ManagementRequest request;
	private final ActorRef replyTo;
	
	public ManagementRequestMsg (ManagementRequest request, ActorRef user) {
		this.request = request;
		this.replyTo = user;
	}

	public ManagementRequest getRequest() {
		return request;
	}

	public ActorRef getReplyTo() {
		return replyTo;
	}
	
	@Override 
	public String toString () {
		return request.getType() + " " + request.getResourceName();
	}
}
