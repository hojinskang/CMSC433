package messages;

import java.util.ArrayList;

import util.Resource;

/**
 * Class of messages asking resource manager to add given local resources.
 * 
 * This message should ONLY be used during the initialization stage of a manager,
 * before the users are started.
 * 
 * @author Rance Cleaveland
 *
 */
public class AddInitialLocalResourcesRequestMsg {

	private final ArrayList<Resource> localResources;

	public AddInitialLocalResourcesRequestMsg(ArrayList<Resource> localResources) {
		super();
		this.localResources = localResources;
	}

	public ArrayList<Resource> getLocalResources() {
		return localResources;
	}
	
	
}
