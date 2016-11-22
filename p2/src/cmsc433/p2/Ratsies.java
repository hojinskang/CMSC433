package cmsc433.p2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import cmsc433.p2.Machine.MachineType;

public class Ratsies {
	public static Ratsies singleton;

	private int numCustomers, numCooks, numTables, machineCapacity;
	private boolean randomOrders;

	private Thread[] cooks;
	private Thread[] customers;
	private HashMap<Food, Machine> machinesByFood;

	private List<Customer> tables;

	private HashMap<Integer, List<Food>> ordersByOrderNumber;
	private HashMap<Integer, Object> locksByOrderNumber;

	private HashSet<Integer> newOrders;
	private HashSet<Integer> inProgressOrders;
	private HashSet<Integer> finishedOrders;

	private int numFinishedOrders;

	private Object finishedLock;

	Ratsies(int numCustomers, int numCooks, int numTables, int machineCapacity, boolean randomOrders) {
		this.numCustomers = numCustomers;
		this.numCooks = numCooks;
		this.numTables = numTables;
		this.machineCapacity = machineCapacity;
		this.randomOrders = randomOrders;

		singleton = this;
	}

	void runSimulation() {
		// Set things up you might need
		cooks = new Thread[numCooks];
		machinesByFood = new HashMap<Food, Machine>();
		tables = new ArrayList<Customer>(numTables);
		ordersByOrderNumber = new HashMap<Integer, List<Food>>();
		locksByOrderNumber = new HashMap<Integer, Object>();
		newOrders = new HashSet<Integer>();
		inProgressOrders = new HashSet<Integer>();
		finishedOrders = new HashSet<Integer>();
		finishedLock = new Object();
		numFinishedOrders = 0;

		// Start up machines
		machinesByFood.put(FoodType.wings, new Machine(MachineType.fryer, FoodType.wings, machineCapacity));
		machinesByFood.put(FoodType.pizza, new Machine(MachineType.oven, FoodType.pizza, machineCapacity));
		machinesByFood.put(FoodType.sub, new Machine(MachineType.grillPress, FoodType.sub, machineCapacity));
		machinesByFood.put(FoodType.soda, new Machine(MachineType.fountain, FoodType.soda, machineCapacity));

		// Let cooks in
		for (int i = 0; i < numCooks; i++) {
			cooks[i] = new Thread(new Cook("Cook " + (i), machinesByFood));
		}

		// Build the customers.
		customers = new Thread[numCustomers];
		LinkedList<Food> order;
		if (!randomOrders) {
			order = new LinkedList<Food>();
			order.add(FoodType.wings);
			order.add(FoodType.pizza);
			order.add(FoodType.sub);
			order.add(FoodType.soda);
			for (int i = 0; i < customers.length; i++) {
				customers[i] = new Thread(new Customer("Customer " + (i), order));
			}
		} else {
			for (int i = 0; i < customers.length; i++) {
				Random rnd = new Random();
				int wingsCount = rnd.nextInt(4);
				int pizzaCount = rnd.nextInt(4);
				int subCount = rnd.nextInt(4);
				int sodaCount = rnd.nextInt(4);
				order = new LinkedList<Food>();
				for (int b = 0; b < wingsCount; b++) {
					order.add(FoodType.wings);
				}
				for (int f = 0; f < pizzaCount; f++) {
					order.add(FoodType.pizza);
				}
				for (int f = 0; f < subCount; f++) {
					order.add(FoodType.sub);
				}
				for (int c = 0; c < sodaCount; c++) {
					order.add(FoodType.soda);
				}
				customers[i] = new Thread(new Customer("Customer " + (i), order));
			}
		}

		for (int i = 0; i < cooks.length; i++) {
			cooks[i].start();
		}

		// Now "let the customers know the shop is open" by
		// starting them running in their own thread.
		for (int i = 0; i < customers.length; i++) {
			customers[i].start();
			// NOTE: Starting the customer does NOT mean they get to go
			// right into the shop. There has to be a table for
			// them. The Customer class' run method has many jobs
			// to do - one of these is waiting for an available
			// table...
		}

		try {
			// Wait for customers to finish
			// -- you need to add some code here...
			for (int i = 0; i < customers.length; i++) {
				customers[i].join();
			}

			while (!areAllOrdersFinished()) {
				synchronized (finishedLock) {
					try {
						finishedLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// Then send cooks home...
			// The easiest way to do this might be the following, where
			// we interrupt their threads. There are other approaches
			// though, so you can change this if you want to.
			for (int i = 0; i < cooks.length; i++)
				cooks[i].interrupt();
			for (int i = 0; i < cooks.length; i++)
				cooks[i].join();

		} catch (InterruptedException e) {
			System.out.println("Simulation thread interrupted.");
		}

		// Shut down machines
		Simulation.logEvent(SimulationEvent.machineEnding(machinesByFood.remove(FoodType.wings)));
		Simulation.logEvent(SimulationEvent.machineEnding(machinesByFood.remove(FoodType.pizza)));
		Simulation.logEvent(SimulationEvent.machineEnding(machinesByFood.remove(FoodType.sub)));
		Simulation.logEvent(SimulationEvent.machineEnding(machinesByFood.remove(FoodType.soda)));
	}

	void enterRatsies(Customer customer) {
		synchronized (tables) {
			while (numTables <= tables.size()) {
				try {
					tables.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// System.out.println(numTables + " " + tables.size() + " " +
			// customer);
			tables.add(customer);
		}
	}

	void leaveRatsies(Customer customer) {
		synchronized (tables) {
			// System.out.println(numTables + "x" + tables.size() + "x" +
			// customer);
			tables.remove(customer);
			tables.notifyAll();
		}
	}

	boolean placeOrder(Customer customer, List<Food> order, int orderNumber) {
		if (customer == null || order == null || newOrders == null || ordersByOrderNumber == null) {
			return false;
		}
		synchronized (ordersByOrderNumber) {
			ordersByOrderNumber.put(orderNumber, order);
		}

		synchronized (locksByOrderNumber) {
			locksByOrderNumber.put(orderNumber, new Object());
		}

		synchronized (newOrders) {
			newOrders.add(orderNumber);
			synchronized (this) {
				this.notifyAll();
			}
			newOrders.notifyAll();
		}
		return true;
	}

	void startOrder(Cook cook, int orderNumber) {
		synchronized (getOrderLock(orderNumber)) {
			synchronized (inProgressOrders) {
				inProgressOrders.add(orderNumber);
				getOrderLock(orderNumber).notifyAll();
			}
		}
	}

	void completeOrder(Cook cook, int orderNumber) {
		synchronized (getOrderLock(orderNumber)) {
			synchronized (inProgressOrders) {
				inProgressOrders.remove(orderNumber);
				synchronized (finishedOrders) {
					finishedOrders.add(orderNumber);
				}
				synchronized (finishedLock) {
					numFinishedOrders += 1;
				}
			}
			getOrderLock(orderNumber).notifyAll();
		}
	}

	Object getOrderLock(int orderNumber) {
		synchronized (locksByOrderNumber) {
			return locksByOrderNumber.get(orderNumber);
		}
	}

	HashSet<Integer> getNewOrders() {
		synchronized (newOrders) {
			return newOrders;
		}
	}

	boolean hasNewOrders() {
		synchronized (newOrders) {
			return !newOrders.isEmpty();
		}
	}

	Integer getNextOrder() {
		synchronized (newOrders) {
			while (newOrders.isEmpty() && !areAllOrdersFinished()) {
				if (areAllOrdersFinished()) {
					return null;
				}
				try {
					newOrders.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (areAllOrdersFinished()) {
					return null;
				}
			}
			Iterator<Integer> it = newOrders.iterator();
			Integer orderNumber = -1;
			while (it.hasNext()) {
				orderNumber = it.next();
				break;
			}
			newOrders.remove(orderNumber);
			return orderNumber;
		}
	}

	boolean isInProgress(int orderNumber) {
		synchronized (getOrderLock(orderNumber)) {
			synchronized (finishedOrders) {
				return !finishedOrders.contains(orderNumber);
			}
		}
	}

	List<Food> getOrder(int orderNumber) {
		synchronized (ordersByOrderNumber) {
			return ordersByOrderNumber.get(orderNumber);
		}
	}

	private boolean areAllOrdersFinished() {
		System.out.println(finishedOrders);
		synchronized (finishedLock) {
			return numFinishedOrders == numCustomers;
		}
	}
}
