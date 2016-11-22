package enums;

/**
 * Enum type for reasons that access requests might be denied.
 * 
 * @author Rance Cleaveland
 *
 */
public enum AccessRequestDenialReason {
	RESOURCE_BUSY,		// Used for responses to non-blocking requests
	RESOURCE_DISABLED,	// Used if resource is present but disabled
	RESOURCE_NOT_FOUND	// Used if resource is not present
}
