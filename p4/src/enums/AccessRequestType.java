package enums;

/**
 * Type of resource-access requests that users may make.
 * 
 * @author Rance Cleaveland
 *
 */
public enum AccessRequestType {
	CONCURRENT_READ_BLOCKING,		// Concurrent read access, blocking request
	CONCURRENT_READ_NONBLOCKING,	// Concurrent read access, nonblocking request
	EXCLUSIVE_WRITE_BLOCKING,		// Exclusive write access, blocking request
	EXCLUSIVE_WRITE_NONBLOCKING,	// Exclusive write access, nonblocking request
}
