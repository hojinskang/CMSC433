package messages;

/**
 * Class of messages for responding to local-user addition requests.  The original
 * request message should be stored in the body of the message.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddLocalUsersResponseMsg {
	private final AddLocalUsersRequestMsg requestMsg;	// Original request
	
	public AddLocalUsersResponseMsg (AddLocalUsersRequestMsg msg) {
		this.requestMsg = msg;
	}

	public AddLocalUsersRequestMsg getRequestMsg() {
		return requestMsg;
	}
}
