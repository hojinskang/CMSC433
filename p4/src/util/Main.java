package util;

import java.util.ArrayList;
import actors.SimulationManagerActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Terminated;
import akka.pattern.Patterns;
import enums.*;

import messages.SimulationFinishMsg;
import messages.SimulationStartMsg;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Sample class for setting up and running a resource-manager system.
 * 
 * Feel free to modify as you wish, but do not include any code that the rest of
 * your implementation depends on.
 * 
 * @author Rance Cleaveland
 *
 */
public class Main {

	ActorSystem system = ActorSystem.create("Resource manager system");

	public static void main(String[] args) {
		// Create actor system and instantiate a simulation manager.
		
		ActorSystem system = ActorSystem.create("Simulation");
		ArrayList<NodeSpecification> nodes = setupTest1();
		ActorRef simulationManager = SimulationManagerActor.makeSimulationManager(nodes, system);
		
		// Start simulation manager and retrieve result
		
		long futureDelay = 1000L;  // milliseconds
		Duration awaitDelay = Duration.Inf();

		Future<Object> fmsg = Patterns.ask(simulationManager, new SimulationStartMsg(), futureDelay);
		SimulationFinishMsg msg = null;
		try {
			msg = (SimulationFinishMsg)Await.result(fmsg, awaitDelay);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		// When each users has finished, terminate
		system.terminate();
		
		// Get future that returns result when system has terminated.
		Future<Terminated> term = system.whenTerminated();
		try {
			Await.result(term, awaitDelay);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		// It is critical not to examine the log until after the actor system has shutdown. Otherwise, the log
		// may still be being modified as ResourceManagers send messages to the LoggerActor.
		for (Object o : msg.getLog())
			System.out.println(o);
	}

	private static ArrayList<NodeSpecification> setupTest1 () {
		// Create initial resources
		
		ArrayList<Resource> printers = Systems.makeResources("Printer", 2);
		ArrayList<Resource> scanners = Systems.makeResources("Scanner", 1);
		
		// Create user scripts
		
		ArrayList<Object> list1 = new ArrayList<Object>();
		list1.add(new AccessRequest("Printer_0", AccessRequestType.EXCLUSIVE_WRITE_BLOCKING));
		list1.add(new AccessRequest("Scanner_0", AccessRequestType.CONCURRENT_READ_NONBLOCKING));
		
		ArrayList<Object> list2 = new ArrayList<Object>();
		list2.add(new AccessRelease("Printer_0", AccessType.EXCLUSIVE_WRITE));
		list2.add(new AccessRelease("Scanner_0", AccessType.CONCURRENT_READ));
		
		UserScript script1 = UserScript.concatenate(UserScript.makeSequential(list1), UserScript.makeSequential(list2));
		UserScript script2 = UserScript.concatenate(UserScript.makeConcurrent(list1), UserScript.makeConcurrent(list2));
		
		// Create node specifications
		
		ArrayList<UserScript> scriptList1 = new ArrayList<UserScript>();
		scriptList1.add(script1);
		NodeSpecification node1 = new NodeSpecification(printers, scriptList1);
		
		ArrayList<UserScript> scriptList2 = new ArrayList<UserScript>();
		scriptList2.add(script2);
		NodeSpecification node2 = new NodeSpecification(scanners, scriptList2);
		
		// Return list of nodes
		
		ArrayList<NodeSpecification> list = new ArrayList<NodeSpecification> ();
		list.add(node1);
		list.add(node2);
		return list;
	}
	
	private static ArrayList<NodeSpecification> setupTest2 () {
		ArrayList<Object> list1 = new ArrayList<Object> ();
		list1.add(new AccessRequest("Printer_0", AccessRequestType.CONCURRENT_READ_BLOCKING));
		list1.add(new ManagementRequest("Printer_0", ManagementRequestType.ADD));
		list1.add(new AccessRequest("Printer_0", AccessRequestType.CONCURRENT_READ_BLOCKING));
		list1.add(new ManagementRequest("Printer_0", ManagementRequestType.ENABLE));
		list1.add(new AccessRequest("Printer_0", AccessRequestType.CONCURRENT_READ_BLOCKING));
		list1.add(new AccessRelease("Printer_0", AccessType.CONCURRENT_READ));
		
		ArrayList<UserScript> scriptList1 = new ArrayList<UserScript> ();
		scriptList1.add(UserScript.makeSequential(list1));
		NodeSpecification node1 = new NodeSpecification(new ArrayList<Resource>(), scriptList1);
		NodeSpecification node2 = new NodeSpecification(new ArrayList<Resource>(), new ArrayList<UserScript>());
		
		ArrayList<NodeSpecification> list = new ArrayList<NodeSpecification> ();
		list.add(node1);
		list.add(node2);
		return list;
	}
}
