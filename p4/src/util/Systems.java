package util;

import java.util.ArrayList;
import java.util.Arrays;
import actors.ResourceManagerActor;
import actors.UserActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import messages.AddInitialLocalResourcesRequestMsg;
import messages.AddInitialLocalResourcesResponseMsg;
import messages.AddLocalUsersRequestMsg;
import messages.AddRemoteManagersRequestMsg;
import messages.AddRemoteManagersResponseMsg;
import messages.AddLocalUsersResponseMsg;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Class of static methods for assembling resource-management systems
 * 
 * @author Rance Cleaveland
 *
 */
public class Systems {

	/**
	 * Make a resource with the given name.
	 * 
	 * @param name
	 *            Resource name
	 * @return Resource object with given name
	 */
	public static Resource makeResource(String name) {
		return new Resource(name);
	}

	/**
	 * Make a resource with the given base name, and "_number" appended.
	 * 
	 * @param baseName
	 *            Base name of resource
	 * @param number
	 *            Number to append to base name
	 * @return Resource object with given name
	 */
	public static Resource makeResource(String baseName, int number) {
		return makeResource(baseName + "_" + Integer.toString(number));
	}

	/**
	 * Make an array list of resources with given base name, and numbers 0 ..
	 * number-1.
	 * 
	 * @param baseName
	 *            Base name of resources
	 * @param number
	 *            Number of instances of resource to create
	 * @return Array list of resources
	 */
	public static ArrayList<Resource> makeResources(String baseName, int number) {
		Resource[] resourceArray = new Resource[number];
		for (int i = 0; i < number; i++) {
			resourceArray[i] = makeResource(baseName, i);
		}
		return new ArrayList<Resource>(Arrays.asList(resourceArray));
	}
	/**
	 * Create system of resource-manager, user actors from node list and
	 * return actors.
	 * 
	 * @param nodes		List of node specs (resource list, user scripts)
	 * @param logger	Actor to send logging messages to
	 * @param context	Context in which to install actors
	 * @return List of user actors created
	 */
	public static SystemActors makeSystem(ArrayList<NodeSpecification> nodes, ActorRef logger, ActorSystem system) {

		ArrayList<ActorRef> managers = new ArrayList<ActorRef>();
		ArrayList<ActorRef> users = new ArrayList<ActorRef>();
		
		long futureDelay = 1000; // millisecond
		Duration awaitDelay = Duration.Inf();
		
		// For each node spec, create manager, users, accumulating each
		
		for (NodeSpecification spec : nodes) {
			
			// Create manager, add to list of managers.
			ActorRef manager = ResourceManagerActor.makeResourceManager(logger, system);
			managers.add(manager);
			
			// Assign local resources to new manager.
			AddInitialLocalResourcesRequestMsg rmsg = new AddInitialLocalResourcesRequestMsg (spec.getResources());
			Future<Object> fmsg = Patterns.ask(manager, rmsg, futureDelay);
			try {
				AddInitialLocalResourcesResponseMsg ack = (AddInitialLocalResourcesResponseMsg)Await.result(fmsg, awaitDelay);
			}
			catch (Exception e) {
				System.out.println(e);
				System.out.println("Error in makeSystem(): local users");
				return null;
			}
			
			
			// Create users and add them into manager
			for (UserScript s : spec.getUserScripts()) {
				ActorRef user = UserActor.makeUser(s, manager, logger, system);
				users.add(user);
			}
			AddLocalUsersRequestMsg amsg = new AddLocalUsersRequestMsg(users);
			fmsg = Patterns.ask(manager, amsg, futureDelay);
			try {
				AddLocalUsersResponseMsg ack = (AddLocalUsersResponseMsg)Await.result(fmsg, awaitDelay);
			}
			catch (Exception e) {
				System.out.println(e);
				System.out.println("Error in makeSystem(): local users");
				return null;
			}
			
		}
		
		// Update remote managers of each manager
		for (ActorRef m : managers) {
			Future<Object> fmsg = Patterns.ask(m, new AddRemoteManagersRequestMsg(managers), futureDelay);
			try {
				AddRemoteManagersResponseMsg msg = (AddRemoteManagersResponseMsg)Await.result(fmsg, awaitDelay);
			}
			catch (Exception e) {
				System.out.println(e);
				System.out.println("Error in makeSystem(): remote managers");
				return null;
			}
		}
		
		// Return list of users

		return new SystemActors(managers, users);
	}



}
