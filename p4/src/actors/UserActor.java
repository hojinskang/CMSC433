package actors;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import messages.AccessReleaseMsg;
import messages.AccessRequestDeniedMsg;
import messages.AccessRequestGrantedMsg;
import messages.AccessRequestMsg;
import messages.LogMsg;
import messages.ManagementRequestDeniedMsg;
import messages.ManagementRequestGrantedMsg;
import messages.ManagementRequestMsg;
import messages.UserStartMsg;
import util.AccessRelease;
import util.AccessRequest;
import util.ManagementRequest;
import util.UserScript;

/**
 * Class of user actors.
 * 
 * Each user has a script, which is a list of steps; each step is
 * in turn a list of requests to make all at once. The user should send each make
 * each request in a single step all at once, then await the responses before moving
 * on to the next step in the script.  When the script is finished, the user actor
 * should stop.
 * 
 * @author Rance Cleaveland
 *
 */
public class UserActor extends UntypedActor {
	
	private UserScript script;	// Script of messages to send
	private ActorRef localResourceManager;	// Local resource manager for user
	private ActorRef logger;	// Actor to send logging messages to
	
	/**
	 * Props structure-generator for this class.  Assumption:  script is list of 
	 * non-empty lists of messages.
	 * 
	 * @param script	Script of requests to make
	 * @param localResourceManager	Local resource manager for user
	 * @param logger	Actor to send logging messages to
	 * @return			Props structure
	 */
	static Props props(UserScript script, ActorRef localResourceManager, ActorRef logger) {
		return Props.create(UserActor.class, script, localResourceManager, logger);
	}
	
	/**
	 * Make a new user actor and install it in the given actor system
	 * @param script	Script of requests to make
	 * @param localResourceManager	Local resource manager for user
	 * @param logger	Actor to send logging messages to
	 * @param system	Actor system
	 * @return			Reference to new user actor
	 */
	public static ActorRef makeUser (UserScript script, ActorRef localResourceManager, ActorRef logger, ActorSystem system) {
		ActorRef newUser = system.actorOf(props(script, localResourceManager, logger));
		return newUser;
	}
		
	/**
	 * Constructor.
	 * 
	 * @param script	Script of requests to be made by user
	 * @param localResourceManager	Local resource manager for user
	 * @param logger	Actor to send logging messages to
	 */
	private UserActor(UserScript script, ActorRef localResourceManager, ActorRef logger) {
		super();
		this.script = script;
		this.localResourceManager = localResourceManager;
		this.logger = logger;
	}

	private ArrayList<Object> currentPendingRequests;	// Requests that need responses
	
	/**
	 * Remove access-release requests.
	 */
	private void removeResponselessRequests() {
		for (int j = currentPendingRequests.size() - 1; j >= 0; j--) {
			if (currentPendingRequests.get(j) instanceof AccessRelease) {
				currentPendingRequests.remove(j);
			}
		}
	}
	
	/**
	 * Method for logging receive
	 * 
	 * @param msg	Message received
	 */
	private void logReceive(Object msg) {
		logger.tell(LogMsg.makeReceiveLogMsg(getSender(), msg, getSelf()), getSelf());
	}
	
	/**
	 * Method for logging send of message.
	 * 
	 * @param msf	Message sent
	 * @param recipient	Recipient of message
	 */	
	private void logSend(Object msg, ActorRef recipient) {
		logger.tell(LogMsg.makeSendLogMsg(getSelf(), msg, recipient), getSelf());
	}
	
	/**
	 * Method for logging start of user.
	 */
	private void logStart() {
		logger.tell(LogMsg.makeUserStartLogMsg(getSelf()), getSelf());
	}
	
	/**
	 * Method for logging termination of user.
	 */
	private void logTerminate() {
		logger.tell(LogMsg.makeUserTerminateLogMsg(getSelf()), getSelf());
	}

	/**
	 * If script is non-empty, retrieve next step, delete it from script,
	 * then send all messages.  Otherwise, stop actor.
	 * 
	 * @return	Next list of messages to send, if it exists, and null otherwise
	 * @throws Exception 
	 */
	private void sendNextMsgs () throws Exception {
		if (script.isDone()) {  // No more messages to send, so log this and stop
			logTerminate();
			getContext().stop(getSelf());
		}
		else {	// Get next requests, record them, and send request messages.
			try {
				currentPendingRequests = script.firstStep();
				script = script.rest();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Send all messages corresponding to current requests.
			for (Object req : currentPendingRequests) {
				
				// Create request message to send
				Object msg = null;
				if (req instanceof AccessRequest) {
					msg = new AccessRequestMsg ((AccessRequest)req, getSelf());
				}
				else if (req instanceof ManagementRequest) {
					msg = new ManagementRequestMsg ((ManagementRequest)req, getSelf());
				}
				else if (req instanceof AccessRelease) {
					msg = new AccessReleaseMsg ((AccessRelease) req, getSelf());
				}
				else {
					throw new Exception ("Bad access request in sendNextMsgs()");
				}
				// Log, send message
				logSend(msg, localResourceManager);
				localResourceManager.tell(msg, getSelf());	
			}
			// Remove requests from current pending list that do not involve awaiting a
			// response
			removeResponselessRequests();
			
			// Send the next messages in the case that no requests warranted a response.
			if (currentPendingRequests.isEmpty())
				sendNextMsgs();
		}
	}
	
	/**
	 * Process request from list of pending requests by removing.  Throw exception if message
	 * is not in list.
	 * 
	 * @param msg	Request to remove.
	 * @throws Exception 
	 */
	private void processPendingRequest (Object msg) throws Exception {
		int i = currentPendingRequests.indexOf(msg);
		if (i != -1) {
			currentPendingRequests.remove(i);
		}
		else {
			throw (new Exception ("Message not found in pending message list"));
		}
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
		// Log receipt of message.
		logReceive(msg);
		
		// Start up user and send first round of messages.		
		if (msg instanceof UserStartMsg) {
			logStart();		// Log starting of user
			sendNextMsgs();
		}
		
		// Find correspond request in pending request list and delete it.		
		else if (msg instanceof AccessRequestDeniedMsg) {
			AccessRequestDeniedMsg aMsg = (AccessRequestDeniedMsg) msg;
			processPendingRequest (aMsg.getRequest());
		}
		else if (msg instanceof AccessRequestGrantedMsg) {
			AccessRequestGrantedMsg aMsg = (AccessRequestGrantedMsg) msg;
			processPendingRequest (aMsg.getRequest());	
		}
		else if (msg instanceof ManagementRequestDeniedMsg) {
			ManagementRequestDeniedMsg mMsg = (ManagementRequestDeniedMsg) msg;
			processPendingRequest (mMsg.getRequest());
		}
		else if (msg instanceof ManagementRequestGrantedMsg) {
			ManagementRequestGrantedMsg mMsg = (ManagementRequestGrantedMsg) msg;
			processPendingRequest (mMsg.getRequest());
		}
		
		// Check to see if pending request list is empty, and if so, move on to next step.
		if (currentPendingRequests.isEmpty()) {
			sendNextMsgs();
		}
	} // end of onReceive
}
