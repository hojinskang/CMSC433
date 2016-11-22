package util;

import enums.ManagementRequestType;

/**
 * Class of resource-management requests that users can make.
 * 
 * @author Rance Cleaveland
 *
 */
public class ManagementRequest {

	private final String resourceName;
	private final ManagementRequestType type;
	
	public ManagementRequest (String name, ManagementRequestType type) {
		this.resourceName = name;
		this.type = type;
	}

	public String getResourceName () {
		return resourceName;
	}
	
	public ManagementRequestType getType () {
		return type;
	}
}
