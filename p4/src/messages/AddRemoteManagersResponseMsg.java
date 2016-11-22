package messages;

/**
 * Class of messages for responding to remote-manager addition requests.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddRemoteManagersResponseMsg {

	private final AddRemoteManagersRequestMsg requestMsg;	// Original request
	
	public AddRemoteManagersResponseMsg (AddRemoteManagersRequestMsg msg) {
		this.requestMsg = msg;
	}

	public AddRemoteManagersRequestMsg getRequestMsg() {
		return requestMsg;
	}

}
