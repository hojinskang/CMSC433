package messages;

import util.ManagementRequest;

/**
 * Class of messages resource managers send in response to management requests that
 * can be granted.  The message includes the original request.
 * 
 * @author Rance Cleaveland
 *
 */
public class ManagementRequestGrantedMsg {
	private final ManagementRequest request;	// Request being replied to
	
	public ManagementRequestGrantedMsg (ManagementRequest request) {
		this.request = request;
	}
	
	/**
	 * Version of constructor for building response message directly from request message.
	 * 
	 * @param msg	Request message being responded to
	 */
	public ManagementRequestGrantedMsg (ManagementRequestMsg msg) {
		this.request = msg.getRequest();
	}

	public ManagementRequest getRequest() {
		return request;
	}
	
	@Override 
	public String toString () {
		return request.getType().toString() + " " + request.getResourceName() + " was successful";
	}

}
