package cmsc433.p1;

/*
 * An exception that is thrown by the auction server when a bidder does not pay at least the amount promised
 * for an item.
 */
public class InsufficientFundsException extends Exception {
	private static final long serialVersionUID = 1L;

	public InsufficientFundsException () {}
}
