package messages;

import akka.actor.ActorRef;

/**
 * Messages to be sent to logger.
 * 
 * @author Rance Cleaveland
 *
 */
public class LogMsg {

	public static enum EventType {
		RECEIVE,
		SEND,
		USER_START,
		USER_TERMINATE
	}
	
	// Static methods for constructing log messages
	
	public static LogMsg makeReceiveLogMsg (ActorRef sender, Object msg, ActorRef recipient) {
		return new LogMsg(EventType.RECEIVE, sender, msg, recipient);
	}
	
	public static LogMsg makeSendLogMsg (ActorRef sender, Object msg, ActorRef recipient) {
		return new LogMsg(EventType.SEND, sender, msg, recipient);
	}
	
	public static LogMsg makeUserStartLogMsg (ActorRef user) {
		return new LogMsg(EventType.USER_START, user, null, null);
	}
	
	public static LogMsg makeUserTerminateLogMsg (ActorRef user) {
		return new LogMsg(EventType.USER_TERMINATE, user, null, null);
	}
	
	private final EventType type;		// Type of event
	private final ActorRef sender;		// Message sender, or user generating event
	private final Object msg;			// Message (or null if user-generated event)
	private final ActorRef recipient;	// Message recipient (or null if user-generated event)
	
	public LogMsg(EventType type, ActorRef sender, Object msg, ActorRef recipient) {
		this.type = type;
		this.sender = sender;
		this.msg = msg;
		this.recipient = recipient;
	}
	
	public EventType getType() {
		return type;
	}

	public ActorRef getSender() {
		return sender;
	}

	public Object getMsg() {
		return msg;
	}

	public ActorRef getRecipient() {
		return recipient;
	}
	
	@Override public String toString() {
		if (type == EventType.USER_START) {
			return "User Starting: " + sender.path().toString();
		} else if (type == EventType.USER_TERMINATE) {
			return "User Terminating: " + sender.path().toString();
		} else if (type == EventType.SEND) {
			return "User " + sender.path().toString() + " sent \"" + msg.toString() + "\" to " + recipient.path().toString();
		} else if (type == EventType.RECEIVE) {
			return "User " + recipient.path().toString() + " received \"" + msg.toString() + "\" from " + sender.path().toString();
		} else {
			throw new AssertionError ("Unrecognized Event Type: " + type);
		}
	}
}
