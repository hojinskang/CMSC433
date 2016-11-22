package cmsc433.p2;

/**
 * We create all food objects used by the simulation in one place, here.  
 * This allows us to safely check equality via reference, rather than by 
 * structure/values.
 *
 */
public class FoodType {
	public static final Food wings = new Food("wings",350);
	public static final Food pizza = new Food("pizza",600);
	public static final Food sub = new Food("sub",200);
	public static final Food soda = new Food("soda",15);
}
