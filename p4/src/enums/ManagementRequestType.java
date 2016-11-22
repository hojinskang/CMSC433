package enums;

/**
 * Type of resource-management requests users may make.
 * 
 * @author Rance Cleaveland
 *
 */
public enum ManagementRequestType {
	ADD,		// Add resource
	ENABLE,		// Enable resource
	DISABLE,	// Disable, but do not remove, resource
	REMOVE		// Remove resource
}
