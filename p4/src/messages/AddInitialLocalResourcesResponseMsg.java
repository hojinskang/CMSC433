package messages;

/**
 * Class of messages for responding to local-user addition requests.  The original
 * request message should be stored in the body of the message.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddInitialLocalResourcesResponseMsg {
	private final AddInitialLocalResourcesRequestMsg request;

	public AddInitialLocalResourcesResponseMsg(AddInitialLocalResourcesRequestMsg request) {
		super();
		this.request = request;
	}

	public AddInitialLocalResourcesRequestMsg getRequest() {
		return request;
	}
	
}
