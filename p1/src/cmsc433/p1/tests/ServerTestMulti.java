package cmsc433.p1.tests;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cmsc433.p1.*;

public class ServerTestMulti
{
	Random random = new Random();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    @Before
    public void setUp() throws Exception
    {
        // Before each test, reset the Server, by re-initializing its instance.
        Constructor<ServerPrinter> serverConstructor = ServerPrinter.class.getDeclaredConstructor((Class<ServerPrinter>[])null);
        serverConstructor.setAccessible(true);

        Field serverInstance = AuctionServer.class.getDeclaredField("instance");
        serverInstance.setAccessible(true);

        serverInstance.set(null, serverConstructor.newInstance((Object[])null));
    }

    @After
    public void tearDown() throws Exception
    {
    }

    private void testMultiThread(int testNumber, int nrSellers, int nrBuyers)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);

        Logger.getInstance().setOutput(ps);

        Thread[] sellerThreads = new Thread[nrSellers];
        Thread[] buyerThreads = new Thread[nrBuyers];
        Seller[] sellers = new Seller[nrSellers];
        Bidder[] buyers = new Bidder[nrBuyers];
        
        //Random random = new Random();

        for (int i=0; i<nrSellers; ++i)
        {
            AuctionServer serverPrinter = AuctionServer.getInstance();

            sellers[i] = new Seller(
					/* server = */          serverPrinter,
					/* sellerName = */      "Seller"+i,
					/* cycles = */          random.nextInt(100),
					/* maxSleepTimeMs = */  random.nextInt(100)+1,
					/* randomSeed = */      i);
            sellerThreads[i] = new Thread(sellers[i]);
            sellerThreads[i].start();
        }




        for (int i=0; i<nrBuyers; ++i)
        {
        	if (random.nextInt(2) == 0) {
	            buyers[i] = new ConservativeBidder(
						/* server = */          AuctionServer.getInstance(),
						/* buyerName = */       "Buyer"+i,
						/* initialCash = */     random.nextInt(1000),
						/* cycles = */          random.nextInt(100),
						/* maxSleepTimeMs = */  random.nextInt(100)+1,
						/* randomSeed = */      i);
	            buyerThreads[i] = new Thread(buyers[i]);
	            buyerThreads[i].start();
        	} else {
	            buyers[i] = new AggressiveBidder(
						/* server = */          AuctionServer.getInstance(),
						/* buyerName = */       "Buyer"+i,
						/* initialCash = */     random.nextInt(1000),
						/* cycles = */          random.nextInt(100),
						/* maxSleepTimeMs = */  random.nextInt(100)+1,
						/* randomSeed = */      i);
	            buyerThreads[i] = new Thread(buyers[i]);
	            buyerThreads[i].start();
        	}
        }




        for (int i=0; i<nrSellers; ++i)
        {
            try
            {
                sellerThreads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        int moneySpent = 0;
        for (int i=0; i<nrBuyers; ++i)
        {
            try
            {
                buyerThreads[i].join();
                moneySpent += buyers[i].cashSpent();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }






        //        String output = os.toString();
        //
        //        FileWriter writer;
        //        try
        //        {
        //            // Output the log of the test in a separate file, for ease of debugging.
        //            writer = new FileWriter("out" + testNumber + ".txt");
        //            writer.write(output);
        //            writer.close();
        //        }
        //        catch (IOException e)
        //        {
        //            e.printStackTrace();
        //        }

        assertFalse("You don't have any recording of how much was spent.", moneySpent==0);
        //All the bidders are ConservativeBidders, hence the uncollected revenue should be zero.
        System.out.println("Money Spent: " + moneySpent);
        System.out.println("Revenue: " + AuctionServer.getInstance().revenue());
        System.out.println("Uncollected: " + AuctionServer.getInstance().uncollectedRevenue());
        //assertTrue("Uncollected revenue is not zero",AuctionServer.getInstance().uncollectedRevenue()==0);
        assertTrue("For test number " + testNumber + " the server revenue " + AuctionServer.getInstance().revenue() + " differs from the revenue reported by the buyers " + moneySpent + "!", moneySpent == AuctionServer.getInstance().revenue());
        for (int i=0; i<buyers.length; ++i)
        {
            assertTrue("For test number " + testNumber + " the server items capacity " + buyers[i].mostItemsAvailable() + " exceeds the limit of " + AuctionServer.serverCapacity + "!", buyers[i].mostItemsAvailable() <= AuctionServer.serverCapacity);
        }
    }

    @Test
    public void testMultiThread0()
    {
        testMultiThread(0, random.nextInt(500)+1, random.nextInt(500)+1);
    }

//    @Test
//    public void testMultiThread1()
//    {
//        testMultiThread(1, 80, 160);
//    }
//
//    @Test
//    public void testMultiThread2()
//    {
//        testMultiThread(2, 80, 240);
//    }
//
//    @Test
//    public void testMultiThread3()
//    {
//        testMultiThread(3, 20, 320);
//    }
//
//    @Test
//    public void testMultiThread4()
//    {
//        testMultiThread(3, 200, 20);
//    }




}
