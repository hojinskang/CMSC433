package cmsc433.p2;

import java.util.List;

/**
 * Customers are simulation actors that have two fields: a name, and a list of
 * Food items that constitute the Customer's order. When running, an customer
 * attempts to enter the Ratsie's (only successful if the Ratsie's has a free
 * table), place its order, and then leave the Ratsie's when the order is
 * complete.
 */
public class Customer implements Runnable {
	// JUST ONE SET OF IDEAS ON HOW TO SET THINGS UP...
	private final String name;
	private final List<Food> order;
	private final int orderNumber;

	private static int counter = 0;

	/**
	 * You can feel free modify this constructor. It must take at least the name
	 * and order but may take other parameters if you would find adding them
	 * useful.
	 */
	public Customer(String name, List<Food> order) {
		this.name = name;
		this.order = order;
		this.orderNumber = counter;
		counter += 1;
	}

	public String toString() {
		return name;
	}

	/**
	 * This method defines what an Customer does: The customer attempts to enter
	 * the Ratsie's (only successful when the Ratsie's has a free table), place
	 * its order, and then leave the Ratsie's when the order is complete.
	 */
	public void run() {
		// YOUR CODE GOES HERE...
		Simulation.logEvent(SimulationEvent.customerStarting(this));
		Ratsies.singleton.enterRatsies(this);
		Simulation.logEvent(SimulationEvent.customerEnteredRatsies(this));
		Simulation.logEvent(SimulationEvent.customerPlacedOrder(this, order, orderNumber));
		Ratsies.singleton.placeOrder(this, order, orderNumber);
		synchronized (Ratsies.singleton.getOrderLock(orderNumber)) {
			while (Ratsies.singleton.isInProgress(orderNumber)) {
				try {
					Ratsies.singleton.getOrderLock(orderNumber).wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Simulation.logEvent(SimulationEvent.customerReceivedOrder(this, order, orderNumber));
		Simulation.logEvent(SimulationEvent.customerLeavingRatsies(this));
		Ratsies.singleton.leaveRatsies(this);
	}
}