package util;

import enums.ResourceStatus;

/**
 * Class of resources.
 * 
 * @author Rance Cleaveland
 *
 */
public class Resource {
	public final String name;	// Resource name
	private volatile ResourceStatus status = ResourceStatus.DISABLED;
	
	/**
	 * Creates new resource with given name, and default status of DISABLED.
	 * @param name	Name of resource
	 */
	public Resource (String name) {
		this.name = name;
	}
	
	/**
	 * @return	Name of resource
	 */
	public String getName() {
		return name;
	}


	/**
	 * @return Status of resource
	 */
	public ResourceStatus getStatus() {
		return status;
	}
	
	/**
	 * Change resource status to ENABLED.
	 */
	public void enable() {
		status = ResourceStatus.ENABLED;
	}
	
	/**
	 * Change resource status to DISABLED.
	 */
	public void disable () {
		status = ResourceStatus.DISABLED;
	}
}
