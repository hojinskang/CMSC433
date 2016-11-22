package cmsc433.p1;

/**
 *  @author Jonathan R. Hansford
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionServer {
	/**
	 * Singleton: the following code makes the server a Singleton. You should
	 * not edit the code in the following noted section.
	 * 
	 * For test purposes, we made the constructor protected.
	 */

	/* Singleton: Begin code that you SHOULD NOT CHANGE! */
	protected AuctionServer() {
	}

	private static AuctionServer instance = new AuctionServer();

	public static AuctionServer getInstance() {
		return instance;
	}

	/* Singleton: End code that you SHOULD NOT CHANGE! */

	/*
	 * Statistic variables and server constants: Begin code you should likely
	 * leave alone.
	 */

	/**
	 * Server statistic variables and access methods:
	 */
	private int soldItemsCount = 0;
	private int revenue = 0;
	private int uncollectedRevenue = 0;

	public int soldItemsCount() {
		synchronized (instanceLock) {
			return this.soldItemsCount;
		}
	}

	public int revenue() {
		synchronized (instanceLock) {
			return this.revenue;
		}
	}

	public int uncollectedRevenue() {
		synchronized (instanceLock) {
			return this.uncollectedRevenue;
		}
	}

	/**
	 * Server restriction constants:
	 */
	public static final int maxBidCount = 10; // The maximum number of bids at
												// any given time for a buyer.
	public static final int maxSellerItems = 20; // The maximum number of items
													// that a seller can submit
													// at any given time.
	public static final int serverCapacity = 80; // The maximum number of active
													// items at a given time.

	/*
	 * Statistic variables and server constants: End code you should likely
	 * leave alone.
	 */

	/**
	 * Some variables we think will be of potential use as you implement the
	 * server...
	 */

	// List of items currently up for bidding (will eventually remove things
	// that have expired).
	private List<Item> itemsUpForBidding = new ArrayList<Item>();

	// The last value used as a listing ID. We'll assume the first thing added
	// gets a listing ID of 0.
	private int lastListingID = -1;

	// List of item IDs and actual items. This is a running list with everything
	// ever added to the auction.
	private HashMap<Integer, Item> itemsAndIDs = new HashMap<Integer, Item>();

	// List of itemIDs and the highest bid for each item. This is a running list
	// with everything ever added to the auction.
	private HashMap<Integer, Integer> highestBids = new HashMap<Integer, Integer>();

	// List of itemIDs and the person who made the highest bid for each item.
	// This is a running list with everything ever bid upon.
	private HashMap<Integer, String> highestBidders = new HashMap<Integer, String>();

	// List of Bidders who have been permanently banned because they failed to
	// pay the amount they promised for an item.
	private HashSet<String> blacklist = new HashSet<String>();

	// List of sellers and how many items they have currently up for bidding.
	private HashMap<String, Integer> itemsPerSeller = new HashMap<String, Integer>();

	// List of buyers and how many items on which they are currently bidding.
	private HashMap<String, Integer> itemsPerBuyer = new HashMap<String, Integer>();

	// List of itemIDs that have been paid for. This is a running list including
	// everything ever paid for.
	private HashSet<Integer> itemsSold = new HashSet<Integer>();

	// Object used for instance synchronization if you need to do it at some
	// point
	// since as a good practice we don't use synchronized (this) if we are doing
	// internal
	// synchronization.
	//
	private Object instanceLock = new Object();

	private HashSet<Integer> uncollected = new HashSet<Integer>();
	// private HashSet<Integer> expired = new HashSet<Integer> ();

	private HashMap<String, List<Integer>> bidsByBidder = new HashMap<String, List<Integer>>();

	/*
	 * The code from this point forward can and should be changed to correctly
	 * and safely implement the methods as needed to create a working
	 * multi-threaded server for the system. If you need to add Object instances
	 * here to use for locking, place a comment with them saying what they
	 * represent. Note that if they just represent one structure then you should
	 * probably be using that structure's intrinsic lock.
	 */

	/**
	 * Attempt to submit an <code>Item</code> to the auction
	 * 
	 * @param sellerName
	 *            Name of the <code>Seller</code>
	 * @param itemName
	 *            Name of the <code>Item</code>
	 * @param lowestBiddingPrice
	 *            Opening price
	 * @param biddingDurationMs
	 *            Bidding duration in milliseconds
	 * @return A positive, unique listing ID if the <code>Item</code> listed
	 *         successfully, otherwise -1
	 */
	public int submitItem(String sellerName, String itemName, int lowestBiddingPrice, int biddingDurationMs) {
		// TODO: IMPLEMENT CODE HERE
		// Some reminders:
		// Make sure there's room in the auction site.
		// If the seller is a new one, add them to the list of sellers.
		// If the seller has too many items up for bidding, don't let them add
		// this one.
		// Don't forget to increment the number of things the seller has
		// currently listed.

		synchronized (instanceLock) {
			if (itemsUpForBidding.size() < serverCapacity
					&& (!itemsPerSeller.containsKey(sellerName) || itemsPerSeller.get(sellerName) < maxSellerItems)) {
				lastListingID++; // begins at -1
				Item item = new Item(sellerName, itemName, lastListingID, lowestBiddingPrice, biddingDurationMs);
				itemsUpForBidding.add(item);
				itemsAndIDs.put(lastListingID, item);

				highestBids.put(lastListingID, lowestBiddingPrice);

				if (!itemsPerSeller.containsKey(sellerName)) {
					itemsPerSeller.put(sellerName, 1);
				} else {
					itemsPerSeller.put(sellerName, itemsPerSeller.get(sellerName) + 1);
				}

				return lastListingID;
			}

			return -1;
		}
	}

	/**
	 * Get all <code>Items</code> active in the auction
	 * 
	 * @return A copy of the <code>List</code> of <code>Items</code>
	 */
	public List<Item> getItems() {
		// TODO: IMPLEMENT CODE HERE
		// Some reminders:
		// Don't forget that whatever you return is now outside of your control.

		synchronized (instanceLock) {
			List<Item> temp = new ArrayList<Item>();
			for (Item item : itemsUpForBidding) {
				int listingID = item.listingID();
				if (item.biddingOpen() && !itemsSold.contains(listingID)) {
					temp.add(new Item(item.seller(), item.name(), listingID, item.lowestBiddingPrice(),
							item.biddingDurationMs()));
				}
			}
			return temp;
		}
	}

	/**
	 * Attempt to submit a bid for an <code>Item</code>
	 * 
	 * @param bidderName
	 *            Name of the <code>Bidder</code>
	 * @param listingID
	 *            Unique ID of the <code>Item</code>
	 * @param biddingAmount
	 *            Total amount to bid
	 * @return True if successfully bid, false otherwise
	 */
	public boolean submitBid(String bidderName, int listingID, int biddingAmount) {
		// TODO: IMPLEMENT CODE HERE
		// Some reminders:
		// See if the item exists.
		// See if it can be bid upon.
		// See if this bidder has too many items in their bidding list.
		// Make sure the bidder has not been blacklisted
		// Get current bidding info.
		// See if they already hold the highest bid.
		// See if the new bid isn't better than the existing/opening bid floor.
		// Decrement the former winning bidder's count
		// Put your bid in place

		synchronized (instanceLock) {
			Item item = itemsAndIDs.get(listingID);
			if (item != null && itemsUpForBidding.contains(item) && item.biddingOpen()
					&& (!itemsPerBuyer.containsKey(bidderName) || itemsPerBuyer.get(bidderName) < maxBidCount)
					&& !blacklist.contains(bidderName)
					&& (!highestBidders.containsKey(listingID) || !highestBidders.get(listingID).equals(bidderName))
					&& (highestBids.containsKey(listingID) && highestBids.get(listingID) < biddingAmount)) {

				String formerBidder = highestBidders.get(listingID);

				if (formerBidder != null && itemsPerBuyer.containsKey(formerBidder)) {
					itemsPerBuyer.put(formerBidder, itemsPerBuyer.get(formerBidder) - 1);
				}

				highestBidders.put(listingID, bidderName);
				highestBids.put(listingID, biddingAmount);

				if (itemsPerBuyer.containsKey(bidderName)) {
					itemsPerBuyer.put(bidderName, itemsPerBuyer.get(bidderName) + 1);
				} else {
					itemsPerBuyer.put(bidderName, 1);
				}

				List<Integer> lst;
				if (!bidsByBidder.containsKey(bidderName)) {
					lst = new ArrayList<Integer>();
				} else {
					lst = new ArrayList<Integer>(bidsByBidder.get(bidderName));
				}
				lst.add(listingID);
				bidsByBidder.put(bidderName, lst);

				return true;
			}

			return false;
		}
	}

	/**
	 * Check the status of a <code>Bidder</code>'s bid on an <code>Item</code>
	 * 
	 * @param bidderName
	 *            Name of <code>Bidder</code>
	 * @param listingID
	 *            Unique ID of the <code>Item</code>
	 * @return 1 (success) if bid is over and this <code>Bidder</code> has won
	 *         <br>
	 *         2 (open) if this <code>Item</code> is still up for auction<br>
	 *         3 (failed) If this <code>Bidder</code> did not win or the
	 *         <code>Item</code> does not exist
	 */
	public int checkBidStatus(String bidderName, int listingID) {
		final int SUCCESS = 1, OPEN = 2, FAILURE = 3;
		// TODO: IMPLEMENT CODE HERE
		// Some reminders:
		// If the bidding is closed, clean up for that item.
		// Remove item from the list of things up for bidding.
		// Decrease the count of items being bid on by the winning bidder if
		// there was any...
		// Update the number of open bids for this seller
		// If the item was sold to someone, update the uncollectedRevenue field
		// appropriately

		int bidStatus = 3;

		synchronized (instanceLock) {
			Item item = itemsAndIDs.get(listingID);
			if (item != null) { // item exists
				if (!item.biddingOpen()) { // bidding closed
					itemsUpForBidding.remove(item);

					String seller = item.seller();
					itemsPerSeller.put(seller, itemsPerSeller.get(seller) - 1);

					if (highestBidders.containsKey(listingID)) { // bid exists
						String highestBidder = highestBidders.get(listingID);

						if (highestBidder.equals(bidderName)) { // current
																// bidder =
																// highest
																// bidder
							if (!uncollected.contains(listingID)) {
								uncollectedRevenue += highestBids.get(listingID);
								uncollected.add(listingID);
							}
							bidStatus = 1;
						} else { // bidder didn't win
							bidStatus = 3;
						}
					} else { // no bid exists
						bidStatus = 3;
					}
				} else { // bidding open
					bidStatus = 2;
				}
			} else { // item doesn't exist
				bidStatus = 3;
			}

			return bidStatus;
		}
	}

	/**
	 * Check the current bid for an <code>Item</code>
	 * 
	 * @param listingID
	 *            Unique ID of the <code>Item</code>
	 * @return The highest bid so far or the opening price if no bid has been
	 *         made, -1 if no <code>Item</code> exists or the bidding has closed
	 */
	public int itemPrice(int listingID) {
		// TODO: IMPLEMENT CODE HERE

		synchronized (instanceLock) {
			Item item = itemsAndIDs.get(listingID);
			if (item != null) {
				return highestBids.get(listingID);
			} else { // item doesn't exist or bidding closed
				return -1;
			}
		}
	}

	/**
	 * Check whether an <code>Item</code> has a bid on it
	 * 
	 * @param listingID
	 *            Unique ID of the <code>Item</code>
	 * @return True if there is no bid or the <code>Item</code> does not exist,
	 *         false otherwise
	 */
	public boolean itemUnbid(int listingID) {
		// TODO: IMPLEMENT CODE HERE

		synchronized (instanceLock) {
			Item item = itemsAndIDs.get(listingID);
			if (item != null // item exists
					&& highestBidders.containsKey(listingID)) { // bid exists
				return false;
			} else { // item doesn't exist or no bid exists
				return true;
			}
		}
	}

	/**
	 * Pay for an <code>Item</code> that has already been won.
	 * 
	 * @param bidderName
	 *            Name of <code>Bidder</code>
	 * @param listingID
	 *            Unique ID of the <code>Item</code>
	 * @param amount
	 *            The amount the <code>Bidder</code> is paying for the item
	 * @return The name of the <code>Item</code> won, or null if the
	 *         <code>Item</code> was not won by the <code>Bidder</code> or if
	 *         the <code>Item</code> did not exist
	 * @throws InsufficientFundsException
	 *             If the <code>Bidder</code> did not pay at least the final
	 *             selling price for the <code>Item</code>
	 */
	public String payForItem(String bidderName, int listingID, int amount) throws InsufficientFundsException {
		// TODO: IMPLEMENT CODE HERE
		// Remember:
		// - Check to make sure the buyer is the correct individual and can
		// afford the item
		// - Update server revenue and sold items count if the purchase is
		// valid.
		// - If the amount tendered is insufficient, cancel all active bids held
		// by the buyer,
		// add the buyer to the blacklist, and throw an
		// InsufficientFundsException

		synchronized (instanceLock) {
			Item item = itemsAndIDs.get(listingID);
			if (item != null // item exists
					&& !itemsSold.contains(listingID) // item not sold yet
					&& !item.biddingOpen() // bidding closed
					&& highestBidders.containsKey(listingID) // has bidder
					&& highestBidders.get(listingID).equals(bidderName)) { // correct
																			// bidder

				int price = highestBids.get(listingID);

				if (amount >= price) { // sufficient funds
					uncollectedRevenue -= price;
					revenue += amount;
					itemsSold.add(listingID);
					soldItemsCount++;

					itemsPerBuyer.put(bidderName, itemsPerBuyer.get(bidderName) - 1);
				} else { // insufficient funds
					blacklist.add(bidderName);

					// restart bid
					List<Integer> lst = bidsByBidder.get(bidderName);
					for (Integer ID : lst) {
						Item itm = itemsAndIDs.get(ID);
						highestBidders.remove(ID);
						highestBids.put(ID, itm.lowestBiddingPrice());
					}

					throw new InsufficientFundsException();
				}

				return item.name();
			} else {
				return null;
			}
		}
	}
}
