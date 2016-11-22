package cmsc433.p2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Cooks are simulation actors that have at least one field, a name. When
 * running, a cook attempts to retrieve outstanding orders placed by Eaters and
 * process them.
 */
public class Cook implements Runnable {
	private static final int wings = 0;
	private static final int pizza = 1;
	private static final int sub = 2;
	private static final int soda = 3;

	private final String name;

	private HashMap<Food, Machine> machinesByFood;
	private HashMap<Integer, HashMap<Food, LinkedList<Thread>>> foodThreadsByOrderNumber;

	/**
	 * You can feel free to modify this constructor. It must take at least the
	 * name, but may take other parameters if you would find adding them useful.
	 *
	 * @param: the name of the cook
	 */
	public Cook(String name, HashMap<Food, Machine> machinesByFood) {
		this.name = name;
		this.machinesByFood = machinesByFood;
		this.foodThreadsByOrderNumber = new HashMap<Integer, HashMap<Food, LinkedList<Thread>>>();
	}

	public String toString() {
		return name;
	}

	/**
	 * This method executes as follows. The cook tries to retrieve orders placed
	 * by Customers. For each order, a List<Food>, the cook submits each Food
	 * item in the List to an appropriate Machine, by calling makeFood(). Once
	 * all machines have produced the desired Food, the order is complete, and
	 * the Customer is notified. The cook can then go to process the next order.
	 * If during its execution the cook is interrupted (i.e., some other thread
	 * calls the interrupt() method on it, which could raise
	 * InterruptedException if the cook is blocking), then it terminates.
	 */
	public void run() {

		Simulation.logEvent(SimulationEvent.cookStarting(this));
		try {
			while (true) {
				Integer orderNumber;
				synchronized (Ratsies.singleton.getNewOrders()) {
					while (!Ratsies.singleton.hasNewOrders()) {
						Ratsies.singleton.getNewOrders().wait();
					}
					orderNumber = Ratsies.singleton.getNextOrder();
				}
				List<Food> order = Ratsies.singleton.getOrder(orderNumber);

				processOrder(order, orderNumber);
			}
		} catch (InterruptedException e) {
			// This code assumes the provided code in the Simulation class
			// that interrupts each cook thread when all customers are done.
			// You might need to change this if you change how things are
			// done in the Simulation class.
			Simulation.logEvent(SimulationEvent.cookEnding(this));
		}
	}

	private void processOrder(List<Food> order, int orderNumber) throws InterruptedException {
		synchronized (Ratsies.singleton.getOrderLock(orderNumber)) {
			Simulation.logEvent(SimulationEvent.cookReceivedOrder(this, order, orderNumber));
			foodThreadsByOrderNumber.put(orderNumber, new HashMap<Food, LinkedList<Thread>>());

			Food[] foods = { FoodType.wings, FoodType.pizza, FoodType.sub, FoodType.soda };
			int[] foodCounts = new int[4];
			for (Food food : order) {
				if (food == FoodType.wings)
					foodCounts[wings] += 1;
				else if (food == FoodType.pizza)
					foodCounts[pizza] += 1;
				else if (food == FoodType.sub)
					foodCounts[sub] += 1;
				else if (food == FoodType.soda)
					foodCounts[soda] += 1;
			}
			
			for (int i = 0; i < 4; i++) {
				Food food = foods[i];
				foodThreadsByOrderNumber.get(orderNumber).put(food, new LinkedList<Thread>());
				for (int j = 0; j < foodCounts[i]; j++) {
					Machine machine = machinesByFood.get(food);
					Simulation.logEvent(SimulationEvent.cookStartedFood(this, food, orderNumber));
					Ratsies.singleton.startOrder(this, orderNumber);
					Thread thread = machine.makeFood(food);
					foodThreadsByOrderNumber.get(orderNumber).get(food).add(thread);
				}
			}
			
			for (Food food : foodThreadsByOrderNumber.get(orderNumber).keySet()) {
				for (Thread thread : foodThreadsByOrderNumber.get(orderNumber).get(food)) {
					thread.join();
					Simulation.logEvent(SimulationEvent.cookFinishedFood(this, food, orderNumber));
				}
			}
			
			Ratsies.singleton.completeOrder(this, orderNumber);
			Simulation.logEvent(SimulationEvent.cookCompletedOrder(this, orderNumber));
		}
	}
}