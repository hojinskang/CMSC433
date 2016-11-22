package messages;

import enums.ManagementRequestDenialReason;
import util.ManagementRequest;

/**
 * Class of messages resource managers send in response to management requests that
 * cannot be granted.  The message includes the original request.
 * 
 * @author Rance Cleaveland
 *
 */
public class ManagementRequestDeniedMsg {
	private final ManagementRequest request;			// Request being replied to
	private final ManagementRequestDenialReason reason;	// Why request was denied
	
	public ManagementRequestDeniedMsg (ManagementRequest request, ManagementRequestDenialReason reason) {
		this.request = request;
		this.reason = reason;
	}
	
	/**
	 * Version of constructor for building response message directly from original request message.
	 * 
	 * @param msg		Message containing request
	 * @param reason	Reason request was denied
	 */
	public ManagementRequestDeniedMsg (ManagementRequestMsg msg, ManagementRequestDenialReason reason) {
		this.request = msg.getRequest();
		this.reason = reason;
	}

	/**
	 * @return Original request that is being denied
	 */
	public ManagementRequest getRequest() {
		return request;
	}

	/**
	 * @return Reason for denial of request
	 */
	public ManagementRequestDenialReason getReason() {
		return reason;
	}
	
	@Override 
	public String toString () {
		return request.getType().toString() + " " + request.getResourceName() + " denied because " + reason.toString(); 
	}

}
