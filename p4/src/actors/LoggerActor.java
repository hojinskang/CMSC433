package actors;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import messages.LogMsg;
import messages.LogResultMsg;

/**
 * @author Rance Cleaveland
 *
 */
public class LoggerActor extends UntypedActor {
	
	// Event log
	private ArrayList<Object> log = new ArrayList<Object>();
	
	// Users who have started but not terminated.
	private ArrayList<ActorRef> activeUsers = new ArrayList<ActorRef>();
	
	// Destination for eventual log.
	
	private ActorRef simulationManager;

	/**
	 * Props structure-generator for this class.
	 * @return  Props structure
	 */
	static Props props (ActorRef simulationManager) {
		return Props.create(LoggerActor.class, simulationManager);
	}
	
	/**
	 * Factory method for creating resource managers
	 * @param simulationManager	Actor to whom to send log when simulation is done
	 * @param system			Actor system in which manager will execute
	 * @return					Reference to new manager
	 */
	public static ActorRef makeLogger (ActorRef simulationManager, ActorSystem system) {
		ActorRef newLogger = system.actorOf(props(simulationManager));
		return newLogger;
	}
	
	/**
	 * Constructor
	 * 
	 * @param simulationManager	Actor to send result to when simulation is finished.
	 */
	private LoggerActor(ActorRef simulationManager) {
		super();
		this.simulationManager = simulationManager;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		// TODO Auto-generated method stub
		if (msg instanceof LogMsg) {  // Message is event to log.
			log.add(msg);
			LogMsg lMsg = (LogMsg)msg;

			// Check if event corresponds to user start; if so, add to list of users
			if (lMsg.getType() == LogMsg.EventType.USER_START) {
				activeUsers.add(lMsg.getSender());
			}
			
			// Check if event corresponds to user termination; if so, remove from list
			// of users.  If list becomes empty, send log out
			else if (lMsg.getType() == LogMsg.EventType.USER_TERMINATE) {
				activeUsers.remove(lMsg.getSender());
				if (activeUsers.isEmpty()) {
					simulationManager.tell(new LogResultMsg(log), getSelf());
				}
			}
		}
		else {
			throw new Exception ("Invalid message sent to logger");
		}
	}

}
