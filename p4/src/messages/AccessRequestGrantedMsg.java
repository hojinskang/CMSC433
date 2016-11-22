package messages;

import util.AccessRequest;

/**
 * Class of messages resource managers send in response to access requests that
 * can be granted.  The message includes the original request message.
 * 
 * @author Rance Cleaveland
 *
 */
public class AccessRequestGrantedMsg {
	private final AccessRequest request;	// Access request being replied to
	
	public AccessRequestGrantedMsg (AccessRequest request) {
		this.request = request;
	}
	
	/**
	 * Constructor to make it easier to generate response message from a request message.
	 * 
	 * @param msg	Message conveying original request.
	 */
	public AccessRequestGrantedMsg (AccessRequestMsg msg) {
		this.request = msg.getAccessRequest();
	}

	public AccessRequest getRequest() {
		return request;
	}
	
	@Override 
	public String toString () {
		return request.getType().toString() + " for " + request.getResourceName() + " granted";
	}
}
