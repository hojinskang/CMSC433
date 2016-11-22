package cmsc433.p2;

/**
 * Food is what is prepared by Cooks, and ordered by Customers.  Food
 * is defined by its name, and the amount of time it takes to prepare
 * by Machine.  It is an immutable class.
 */
public class Food {
	public final String name;
	public final int cookTimeS;

	public Food(String name, int cookTimeS) {
		this.name = name;
		this.cookTimeS = cookTimeS;
	}

	public String toString() {
		return name;
	}
}