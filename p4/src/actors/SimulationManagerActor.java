package actors;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import messages.LogResultMsg;
import messages.SimulationFinishMsg;
import messages.SimulationStartMsg;
import messages.UserStartMsg;
import util.NodeSpecification;
import util.SystemActors;
import util.Systems;

/**
 * Class of actors managing a simulation of a single resource-management system.
 * 
 * @author Rance Cleaveland
 *
 */
/**
 * @author Rance Cleaveland
 *
 */
public class SimulationManagerActor extends UntypedActor {
	
	private ArrayList<NodeSpecification> nodes;	// Nodes in simulated system
	private ActorSystem system;	
	
	/**
	 * Constructor
	 * 
	 * @param nodes
	 * @param system
	 */
	public SimulationManagerActor(ArrayList<NodeSpecification> nodes, ActorSystem system) {
		super();
		this.nodes = nodes;
		this.system = system;
	}
	
	/**
	 * Props structure-generator for this class.
	 * @return  Props structure
	 */
	static Props props (ArrayList<NodeSpecification> nodes, ActorSystem system) {
		return Props.create(SimulationManagerActor.class, nodes, system);
	}
	
	/**
	 * Factory method for creating resource managers
	 * @param localResources	Local resources controlled by new manager
	 * @param logger			Actor to send logging messages to
	 * @param system			Actor system in which manager will execute
	 * @return					Reference to new manager
	 */
	public static ActorRef makeSimulationManager (ArrayList<NodeSpecification> nodes, ActorSystem system) {
		ActorRef newManager = system.actorOf(props(nodes, system));
		return newManager;
	}

	
	private ActorRef replyTo;	// Where to send simulation results when they are ready

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof SimulationStartMsg) {
			
			// Update replyTo field
			replyTo = getSender();
			
			// Create logger for use in simulated system, then system
			ActorRef logger = LoggerActor.makeLogger(getSelf(), system);
			SystemActors actors = Systems.makeSystem (nodes, logger, system);
			
			// Start simulation by sending each user a start message.
			ArrayList<ActorRef> users = actors.getUsers();
			UserStartMsg sMsg = new UserStartMsg();
			for (ActorRef u : users) {
				u.tell(sMsg, getSelf());
			}
		}
		else if (msg instanceof LogResultMsg) {
			
			// Forward simulation results caller and stop.
			LogResultMsg lMsg = (LogResultMsg)msg;
			replyTo.tell(new SimulationFinishMsg(lMsg.getLog()), getSelf());
			getContext().stop(getSelf());
		}
		else {
			throw new Exception("Bad message sent to simulation manager");
		}
	}

}
