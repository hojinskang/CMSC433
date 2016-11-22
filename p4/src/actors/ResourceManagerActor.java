package actors;

import messages.*;
import util.*;
import enums.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

// Goal: Implement distributed resource management scheme
// Make the resources seem "local" to the users (illusion)
// Resource Manager communicates with other nodes to get information about the requested resource
public class ResourceManagerActor extends UntypedActor {

	private ActorRef logger; // Actor to send logging messages to

	/**
	 * Props structure-generator for this class.
	 * 
	 * @return Props structure
	 */
	static Props props(ActorRef logger) {
		return Props.create(ResourceManagerActor.class, logger);
	}

	/**
	 * Factory method for creating resource managers
	 * 
	 * @param logger
	 *            Actor to send logging messages to
	 * @param system
	 *            Actor system in which manager will execute
	 * @return Reference to new manager
	 */
	public static ActorRef makeResourceManager(ActorRef logger, ActorSystem system) {
		ActorRef newManager = system.actorOf(props(logger));
		return newManager;
	}

	/**
	 * Method for logging receive
	 * 
	 * @param msg
	 *            Message received
	 */
	private void logReceive(Object msg) {
		logger.tell(LogMsg.makeReceiveLogMsg(getSender(), msg, getSelf()), getSelf());
	}

	/**
	 * Method for logging send of message.
	 * 
	 * @param msf
	 *            Message sent
	 * @param recipient
	 *            Recipient of message
	 */
	private void logSend(Object msg, ActorRef recipient) {
		logger.tell(LogMsg.makeSendLogMsg(getSelf(), msg, recipient), getSelf());
	}

	/**
	 * Constructor
	 * 
	 * @param logger
	 *            Actor to send logging messages to
	 */
	private ResourceManagerActor(ActorRef logger) {
		super();
		this.logger = logger;

	}

	// You may want to add data structures for managing local resources and
	// users, storing
	// remote managers, etc.
	private Set<ActorRef> localUsers = new HashSet<ActorRef>();
	private Set<ActorRef> remoteManagers = new HashSet<ActorRef>();

	private Map<String, Resource> localResources = new HashMap<String, Resource>();

	private Map<String, Queue<AccessRequestMsg>> resourceToBlockingRequests = new HashMap<String, Queue<AccessRequestMsg>>();

	private Map<String, List<ActorRef>> resourceToReads = new HashMap<String, List<ActorRef>>();
	private Map<String, List<ActorRef>> resourceToWrites = new HashMap<String, List<ActorRef>>();

	private Map<String, ManagementRequestMsg> toBeDisabledResources = new HashMap<String, ManagementRequestMsg>();

	private Map<Object, Set<ActorRef>> msgToRemoteManagers = new HashMap<Object, Set<ActorRef>>();

	/*
	 * (non-Javadoc)
	 * 
	 * You must provide an implementation of the onReceive method below.
	 * 
	 * @see akka.actor.UntypedActor#onReceive(java.lang.Object)
	 */
	@Override
	public void onReceive(Object msg) throws Exception {
		logReceive(msg);

		// TODO You must implement this method
		if (msg instanceof HasResourceMsg) { // possible resource name clash
			HasResourceMsg payload = (HasResourceMsg) msg;
			Object request = payload.getOriginalRequest();
			// msgToRemoteManagers.remove(request);

			// resource name used remotely
			if (request instanceof ManagementRequestMsg) {
				ManagementRequestMsg managementRequest = (ManagementRequestMsg) request;
				if (managementRequest.getRequest().getType() == ManagementRequestType.ADD) {
					ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(managementRequest,
							ManagementRequestDenialReason.RESOURCE_NAME_CLASH);
					sendResponse(response, managementRequest.getReplyTo());
				}
			}

		} else if (msg instanceof DoesNotHaveResourceMsg) {
			DoesNotHaveResourceMsg payload = (DoesNotHaveResourceMsg) msg;
			Object request = payload.getOriginalRequest();

			Set<ActorRef> forwardedRemoteManagers = msgToRemoteManagers.get(request);
			if (forwardedRemoteManagers == null) {
				forwardedRemoteManagers = new HashSet<ActorRef>();
			}
			forwardedRemoteManagers.add(getSender());
			msgToRemoteManagers.put(request, forwardedRemoteManagers);

			// all remote managers said they don't have the resource
			// local manager guaranteed to not have it as well
			if (forwardedRemoteManagers.size() == remoteManagers.size()) {
				if (request instanceof ManagementRequestMsg) {
					ManagementRequestMsg managementRequest = (ManagementRequestMsg) request;
					if (managementRequest.getRequest().getType() == ManagementRequestType.ADD) {
						String resourceName = managementRequest.getRequest().getResourceName();
						ActorRef replyTo = managementRequest.getReplyTo();

						Resource newResource = new Resource(resourceName);
						localResources.put(resourceName, newResource);
						ManagementRequestGrantedMsg response = new ManagementRequestGrantedMsg(
								managementRequest.getRequest());
						sendResponse(response, replyTo);

						// enable by separate request
						ManagementRequest enableRequest = new ManagementRequest(resourceName,
								ManagementRequestType.ENABLE);
						ManagementRequestMsg enableRequestMsg = new ManagementRequestMsg(enableRequest, replyTo);
						sendResponse(enableRequestMsg, replyTo);
					} else {
						ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(managementRequest,
								ManagementRequestDenialReason.RESOURCE_NOT_FOUND);
						sendResponse(response, managementRequest.getReplyTo());
					}
				} else if (request instanceof AccessRequestMsg) {
					AccessRequestMsg accessRequest = (AccessRequestMsg) request;
					AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(accessRequest.getAccessRequest(),
							AccessRequestDenialReason.RESOURCE_NOT_FOUND);
					sendResponse(response, accessRequest.getReplyTo());
				}
			}

		} else if (msg instanceof AccessReleaseMsg) {
			// System.out.println("AccessReleaseMsg");
			AccessReleaseMsg payload = (AccessReleaseMsg) msg;
			AccessRelease accessRelease = payload.getAccessRelease();
			ActorRef sender = payload.getSender();

			String resourceName = accessRelease.getResourceName();

			boolean broadcasted = processBroadcast(resourceName, msg);

			// check local resources
			if (localResources.containsKey(resourceName)) {
				releaseAccess(resourceName, accessRelease.getType(), sender);

				// if all accesses released for that resource, disable if needed
				if (hasAllAccessesReleased(resourceName) && toBeDisabledResources.containsKey(resourceName)) {
					localResources.get(resourceName).disable();
					ManagementRequestGrantedMsg response = new ManagementRequestGrantedMsg(
							toBeDisabledResources.get(resourceName));
					sendResponse(response, toBeDisabledResources.get(resourceName).getReplyTo());
					toBeDisabledResources.remove(resourceName);
				}

				// process with leftover requests
				processBlockingRequests(resourceName);
			} else if (!broadcasted) {
				broadcast(msg, remoteManagers);
			}

		} else if (msg instanceof AccessRequestMsg) {
			AccessRequestMsg payload = (AccessRequestMsg) msg;
			AccessRequest request = payload.getAccessRequest();
			ActorRef replyTo = payload.getReplyTo();

			String resourceName = request.getResourceName();
			AccessRequestType accessType = request.getType();

			boolean broadcasted = processBroadcast(resourceName, msg);

			// if (broadcasted) {
			// System.out.print("BROADCASTED ");
			// }
			// System.out.println("AccessRequestMsg: " + resourceName + " " +
			// replyTo + " " + accessType);

			if (localResources.containsKey(resourceName)) {
				if (localResources.get(resourceName).getStatus() == ResourceStatus.DISABLED // resource
						// disabled
						|| toBeDisabledResources.containsKey(resourceName)) { // resource
					// to-be-disabled
					AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(payload,
							AccessRequestDenialReason.RESOURCE_DISABLED);
					sendResponse(response, replyTo);
					return;
				}

				if (accessType == AccessRequestType.CONCURRENT_READ_BLOCKING
						|| accessType == AccessRequestType.EXCLUSIVE_WRITE_BLOCKING) {
					addToBlockingRequests(resourceName, payload);
					processBlockingRequests(resourceName);

				} else { // non-blocking (no blocking requests)
					if (resourceToBlockingRequests.get(resourceName) == null
							|| resourceToBlockingRequests.get(resourceName).isEmpty()) {

						switch (accessType) {
						case CONCURRENT_READ_NONBLOCKING:
							if (canHaveConcurrentReadAccess(resourceName, replyTo)) {
								giveConcurrentReadAccess(resourceName, replyTo, request);
							} else {
								AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(payload,
										AccessRequestDenialReason.RESOURCE_BUSY);
								sendResponse(response, replyTo);
							}
							break;
						case EXCLUSIVE_WRITE_NONBLOCKING:
							if (canHaveExclusiveWriteAccess(resourceName, replyTo)) {
								giveExclusiveWriteAccess(resourceName, replyTo, request);
							} else {
								AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(payload,
										AccessRequestDenialReason.RESOURCE_BUSY);
								sendResponse(response, replyTo);
							}
							break;
						}
					} else {
						AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(payload,
								AccessRequestDenialReason.RESOURCE_BUSY);
						sendResponse(response, replyTo);
					}
				}
			} else if (!broadcasted) { // request from user, not
										// another manager
//				System.out.println(msg);
//				for (String r : localResources.keySet()) {
//					System.out.println(r);
//				}
//				System.out.println();
				broadcast(msg, remoteManagers);
			}

		} else if (msg instanceof AddInitialLocalResourcesRequestMsg) {
			AddInitialLocalResourcesRequestMsg payload = (AddInitialLocalResourcesRequestMsg) msg;
			for (Resource resource : payload.getLocalResources()) {
				resource.enable();
				localResources.put(resource.getName(), resource);
			}

			AddInitialLocalResourcesResponseMsg response = new AddInitialLocalResourcesResponseMsg(payload);
			sendResponse(response, getSender());

		} else if (msg instanceof AddLocalUsersRequestMsg) {
			AddLocalUsersRequestMsg payload = (AddLocalUsersRequestMsg) msg;
			for (ActorRef user : payload.getLocalUsers()) {
				localUsers.add(user);
			}

			AddLocalUsersResponseMsg response = new AddLocalUsersResponseMsg(payload);
			sendResponse(response, getSender());

		} else if (msg instanceof AddRemoteManagersRequestMsg) {
			AddRemoteManagersRequestMsg payload = (AddRemoteManagersRequestMsg) msg;
			// deep copy without self
			for (ActorRef manager : payload.getManagerList()) {
				if (!manager.equals(getSelf())) {
					remoteManagers.add(manager);
				}
			}

			AddRemoteManagersResponseMsg response = new AddRemoteManagersResponseMsg(payload);
			sendResponse(response, getSender());

		} else if (msg instanceof ManagementRequestMsg) {
			// System.out.println("ManagementRequestMsg");
			ManagementRequestMsg payload = (ManagementRequestMsg) msg;
			ManagementRequest request = payload.getRequest();
			ActorRef replyTo = payload.getReplyTo();

			String resourceName = request.getResourceName();
			ManagementRequestType type = request.getType();

			Resource localResource = localResources.get(resourceName);

			boolean broadcasted = processBroadcast(resourceName, msg);

			switch (type) {
			case ADD:
				if (localResource != null) {
					ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(payload,
							ManagementRequestDenialReason.RESOURCE_NAME_CLASH);
					sendResponse(response, replyTo);
				} else if (!broadcasted) {
					broadcast(msg, remoteManagers); // check to see if name used
													// in remote resources
				}
				break;
			case REMOVE:
				if (localResource == null) {
					ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(payload,
							ManagementRequestDenialReason.RESOURCE_NOT_LOCAL);
					sendResponse(response, replyTo);
				} else if (localResource.getStatus() == ResourceStatus.DISABLED) {
					localResources.remove(resourceName);
					ManagementRequestGrantedMsg response = new ManagementRequestGrantedMsg(payload);
					sendResponse(response, replyTo);
				} else {
					ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(payload,
							ManagementRequestDenialReason.RESOURCE_NOT_DISABLED);
					sendResponse(response, replyTo);
				}
				break;
			case ENABLE:
				if (localResource == null) { // not local
					broadcast(msg, remoteManagers);
				} else if (localResource != null) { // found locally
					if (localResource.getStatus() == ResourceStatus.DISABLED) {
						localResource.enable();
						// process blocking requests TODO
					}
					ManagementRequestGrantedMsg response = new ManagementRequestGrantedMsg(payload);
					sendResponse(response, replyTo);
				}
				break;
			case DISABLE:
				if (localResource == null) { // not local
					broadcast(msg, remoteManagers);
				} else if (localResource != null) { // found locally
					if (hasConcurrentReadAccess(resourceName, replyTo)
							|| hasExclusiveWriteAccess(resourceName, replyTo)) { // currently
																					// has
																					// access
																					// to
																					// resource
						ManagementRequestDeniedMsg response = new ManagementRequestDeniedMsg(payload,
								ManagementRequestDenialReason.ACCESS_HELD_BY_USER);
						sendResponse(response, replyTo);
					} else if ((resourceToReads.get(resourceName) == null
							|| resourceToReads.get(resourceName).isEmpty())
							&& (resourceToWrites.get(resourceName) == null
									|| resourceToWrites.get(resourceName).isEmpty())) {
						localResource.disable();
						processBlockingRequests(resourceName); // clear out the
																// blocking
																// requests
						// TODO SEND THAT IT'S BEEN DISABLED
					} else {
						toBeDisabledResources.put(resourceName, payload);
						processBlockingRequests(resourceName); // clear out the
																// blocking
																// requests
					}
				}
				break;
			}
		} else {
			System.out.println();
			unhandled(msg);
		}
	}

	private void sendResponse(Object msg, ActorRef recipient) {
		logSend(msg, recipient);
		recipient.tell(msg, getSelf());
	}

	private void broadcast(Object msg, Set<ActorRef> recipients) {
		for (ActorRef recipient : recipients) {
			sendResponse(msg, recipient);
			// System.out.println("BROADCASTED " + msg + " to " + recipient);
		}
	}

	private boolean hasConcurrentReadAccess(String resourceName, ActorRef user) {
		if (resourceToReads.containsKey(resourceName)) {
			for (ActorRef temp : resourceToReads.get(resourceName)) {
				if (temp.equals(user))
					return true;
			}
		}
		return false;
	}

	private boolean hasExclusiveWriteAccess(String resourceName, ActorRef user) {
		if (resourceToWrites.containsKey(resourceName)) {
			for (ActorRef temp : resourceToWrites.get(resourceName)) {
				if (temp.equals(user))
					return true;
			}
		}
		return false;
	}

	private boolean noOtherUserHasExclusiveWriteAccess(String resourceName, ActorRef user) {
		if (resourceToWrites.containsKey(resourceName)) {
			for (ActorRef temp : resourceToWrites.get(resourceName)) {
				if (!temp.equals(user))
					return false;
			}
		}
		return true;
	}

	private boolean noOtherUserHasConcurrentReadAccess(String resourceName, ActorRef user) {
		if (resourceToReads.containsKey(resourceName)) {
			for (ActorRef temp : resourceToReads.get(resourceName)) {
				if (!temp.equals(user))
					return false;
			}
		}
		return true;
	}

	private boolean canHaveConcurrentReadAccess(String resourceName, ActorRef user) {
		return noOtherUserHasExclusiveWriteAccess(resourceName, user);
	}

	private boolean canHaveExclusiveWriteAccess(String resourceName, ActorRef user) {
		return noOtherUserHasExclusiveWriteAccess(resourceName, user)
				&& noOtherUserHasConcurrentReadAccess(resourceName, user);
	}

	private boolean hasAllAccessesReleased(String resourceName) {
		return (!resourceToReads.containsKey(resourceName) && !resourceToWrites.containsKey(resourceName));
	}

	private void giveConcurrentReadAccess(String resourceName, ActorRef user, AccessRequest request) {
		List<ActorRef> allUsers = resourceToReads.get(resourceName);
		if (allUsers == null) {
			allUsers = new ArrayList<ActorRef>();
		}
		allUsers.add(user);
		resourceToReads.put(resourceName, allUsers);

		// System.out.println(user + " granted " + request.getType() + " for " +
		// resourceName);
		AccessRequestGrantedMsg response = new AccessRequestGrantedMsg(request);
		sendResponse(response, user);
	}

	private void giveExclusiveWriteAccess(String resourceName, ActorRef user, AccessRequest request) {
		List<ActorRef> allUsers = resourceToWrites.get(resourceName);
		if (allUsers == null) {
			allUsers = new ArrayList<ActorRef>();
		}
		allUsers.add(user);
		resourceToWrites.put(resourceName, allUsers);

		// System.out.println(user + " granted " + request.getType() + " for " +
		// resourceName);
		AccessRequestGrantedMsg response = new AccessRequestGrantedMsg(request);
		sendResponse(response, user);
	}

	private void releaseAccess(String resourceName, AccessType accessType, ActorRef user) {
		if (accessType == AccessType.CONCURRENT_READ) {
			if (resourceToReads.containsKey(resourceName)) {
				List<ActorRef> list = resourceToReads.get(resourceName);
				if (list.contains(user)) {
					list.remove(user);
				}
			}
		} else if (accessType == AccessType.EXCLUSIVE_WRITE) {
			if (resourceToWrites.containsKey(resourceName)) {
				List<ActorRef> list = resourceToWrites.get(resourceName);
				if (list.contains(user)) {
					list.remove(user);
				}
			}
		}
	}

	private void addToBlockingRequests(String resourceName, AccessRequestMsg msg) {
		Queue<AccessRequestMsg> blockingRequests = resourceToBlockingRequests.get(resourceName);
		if (blockingRequests == null) {
			blockingRequests = new LinkedList<AccessRequestMsg>();
		}
		blockingRequests.add(msg);
		resourceToBlockingRequests.put(resourceName, blockingRequests);
	}

	private void processBlockingRequests(String resourceName) {
		Queue<AccessRequestMsg> blockingRequests = resourceToBlockingRequests.get(resourceName);
		if (blockingRequests == null) { // should never occur
			return;
		}

		while (!blockingRequests.isEmpty()) {
			AccessRequestMsg payload = blockingRequests.peek();
			AccessRequest request = payload.getAccessRequest();
			ActorRef replyTo = payload.getReplyTo();
			AccessRequestType accessType = request.getType();

			if (localResources.get(resourceName).getStatus() == ResourceStatus.DISABLED // resource
																						// disabled
					|| toBeDisabledResources.containsKey(resourceName)) { // resource
																			// to-be-disabled
				AccessRequestDeniedMsg response = new AccessRequestDeniedMsg(payload,
						AccessRequestDenialReason.RESOURCE_DISABLED);
				sendResponse(response, replyTo);
			} else {
				switch (accessType) {
				case CONCURRENT_READ_BLOCKING:
					if (canHaveConcurrentReadAccess(resourceName, replyTo)) {
						giveConcurrentReadAccess(resourceName, replyTo, request);
					} else {
						return;
					}
					break;
				case EXCLUSIVE_WRITE_BLOCKING:
					if (canHaveExclusiveWriteAccess(resourceName, replyTo)) {
						giveExclusiveWriteAccess(resourceName, replyTo, request);
					} else {
						return;
					}
					break;
				}
			}

			blockingRequests.remove();
		}
	}

	private boolean processBroadcast(String resourceName, Object msg) {
		// forwarded request from another manager
		if (remoteManagers.contains(getSender())) {
			if (localResources.containsKey(resourceName)) {
				HasResourceMsg response = new HasResourceMsg(msg);
				sendResponse(response, getSender());
			} else {
				DoesNotHaveResourceMsg response = new DoesNotHaveResourceMsg(msg);
				sendResponse(response, getSender());
			}
			return true;
		} else {
			return false;
		}
	}
}
