package messages;

import enums.AccessRequestDenialReason;
import util.AccessRequest;

/**
 * Class of messages resource managers send in response to access requests that
 * cannot be granted.  The message includes the original request message.
 * 
 * @author Rance Cleaveland
 *
 */
public class AccessRequestDeniedMsg {
	private final AccessRequest request;			// Message being replied to
	private final AccessRequestDenialReason reason;	// Why request was denied
	
	public AccessRequestDeniedMsg (AccessRequest request, AccessRequestDenialReason reason) {
		this.request = request;
		this.reason = reason;
	}
	
	/**
	 * Version of constructor to simplify construction of response message from request message.
	 * 
	 * @param msg		Message containing original request
	 * @param reason	Reason for denying request
	 */
	public AccessRequestDeniedMsg (AccessRequestMsg msg, AccessRequestDenialReason reason) {
		this.request = msg.getAccessRequest();
		this.reason = reason;
	}

	/**
	 * @return Original request that is being denied
	 */
	public AccessRequest getRequest() {
		return request;
	}

	/**
	 * @return Reason for denial
	 */
	public AccessRequestDenialReason getReason() {
		return reason;
	}
	
	@Override 
	public String toString() {
		return request.getType() + " for " + request.getResourceName() + " denied because " + reason.toString();
	}

}
