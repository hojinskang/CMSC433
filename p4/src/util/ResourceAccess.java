package util;

import enums.AccessType;

public class ResourceAccess {
	private Resource resource;
	private AccessType type;
	
	public ResourceAccess(Resource resource, AccessType type) {
		this.resource = resource;
		this.type = type;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	public AccessType getAccessType() {
		return type;
	}
	
	public boolean equals(Object o) {
		if (o == null) return false;
		if (o instanceof ResourceAccess) {
			ResourceAccess temp = (ResourceAccess) o;
			return resource.getName().equals(temp.getResource().getName());
		}
		return false;
	}
	
	public int hashCode() {
		return resource.getName().hashCode();
	}
}
