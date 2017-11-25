/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.example.transaction.sim;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.example.transaction.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Runs the generator.
 *
 * @author Hans Gilde
 */
public class TxnGenMain implements Runnable {

    private final static Map<String, Integer> BUCKET_SIZES = new LinkedHashMap<String, Integer>();

    static {
        BUCKET_SIZES.put("tiniest", 20);
        BUCKET_SIZES.put("tiny", 499);
        BUCKET_SIZES.put("small", 4999);
        BUCKET_SIZES.put("medium", 14983);
        BUCKET_SIZES.put("large", 49999);
        BUCKET_SIZES.put("larger", 1999993);
        BUCKET_SIZES.put("largerer", 9999991);
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Arguments are: <bucket_size> <num_transactions>");
            System.exit(-1);
        }

        int bucketSize;
        try {
            bucketSize = BUCKET_SIZES.get(args[0]);
        } catch (NullPointerException e) {
            System.out.println("Invalid bucket size:");
            for (String key : BUCKET_SIZES.keySet()) {
                System.out.println("\t" + key + " -> " + BUCKET_SIZES.get(key));
            }

            System.exit(-2);
            return;
        }

        int numTransactions;
        try {
            numTransactions = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid num transactions");
            System.exit(-2);
            return;
        }

        // Run the sample
        System.out.println("Using bucket size of " + bucketSize + " with " + numTransactions + " transactions");
        TxnGenMain txnGenMain = new TxnGenMain(bucketSize, numTransactions, "TransactionExample", false);
        txnGenMain.run();
    }

    private int bucketSize;
    private int numTransactions;
    private String engineURI;
    private boolean continuousSimulation;

    public TxnGenMain(int bucketSize, int numTransactions, String engineURI, boolean continuousSimulation) {
        this.bucketSize = bucketSize;
        this.numTransactions = numTransactions;
        this.engineURI = engineURI;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        // Configure engine with event names to make the statements more readable.
        // This could also be done in a configuration file.
        Configuration configuration = new Configuration();
        configuration.addEventType("TxnEventA", TxnEventA.class.getName());
        configuration.addEventType("TxnEventB", TxnEventB.class.getName());
        configuration.addEventType("TxnEventC", TxnEventC.class.getName());

        // Get engine instance
        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, configuration);

        // We will be supplying timer events externally.
        // We will assume that each bucket arrives within a defined period of time.
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        // Set up statement for listening to combined events
        CombinedEventStmt combinedEventStmt = new CombinedEventStmt(epService.getEPAdministrator());
        combinedEventStmt.addListener(new CombinedEventListener());

        // Set up statements for realtime summary latency data - overall totals and totals per customer and per supplier
        RealtimeSummaryStmt realtimeSummaryStmt = new RealtimeSummaryStmt(epService.getEPAdministrator());
        realtimeSummaryStmt.addTotalsListener(new RealtimeSummaryTotalsListener());
        realtimeSummaryStmt.addByCustomerListener(new RealtimeSummaryGroupListener("customerId"));
        realtimeSummaryStmt.addBySupplierListener(new RealtimeSummaryGroupListener("supplierId"));

        // Set up statement for finding missing events
        FindMissingEventStmt findMissingEventStmt = new FindMissingEventStmt(epService.getEPAdministrator());
        findMissingEventStmt.addListener(new FindMissingEventListener());

        // The feeder to feed the engine
        FeederOutputStream feeder = new FeederOutputStream(epService.getEPRuntime());

        // Generate transactions
        TransactionEventSource source = new TransactionEventSource(numTransactions);
        ShuffledBucketOutput output = new ShuffledBucketOutput(source, feeder, bucketSize);

        // Feed events
        try {
            if (continuousSimulation) {
                while (true) {
                    output.output();
                    Thread.sleep(5000); // Send a batch every 5 seconds
                }
            } else {
                output.output();
            }
        } catch (InterruptedException ex) {
            // no action
        } catch (IOException ex) {
            throw new RuntimeException("Error outputting events: " + ex.getMessage(), ex);
        }

        epService.destroy();
    }
}
