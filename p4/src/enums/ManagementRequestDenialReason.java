package enums;

/**
 * Enum type of reasons why management requests may be denied.
 * 
 * @author Rance Cleaveland
 *
 */
public enum ManagementRequestDenialReason {
	RESOURCE_NAME_CLASH,	// Returned if adding resource could lead to name clash
	RESOURCE_NOT_DISABLED,	// Returned if trying to remove resource that has not been disabled
	RESOURCE_NOT_FOUND, 	// Returned if the resource does not exist in the system
	RESOURCE_NOT_LOCAL,		// Returned if trying to remove a resource that is not local to the resource manager.
	ACCESS_HELD_BY_USER		// Returned if a user is attempting to disable a resource they currently hold access to.
}
