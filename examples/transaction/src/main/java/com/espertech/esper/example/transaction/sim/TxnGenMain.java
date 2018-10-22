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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.example.transaction.*;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

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
    private String runtimeURI;
    private boolean continuousSimulation;

    public TxnGenMain(int bucketSize, int numTransactions, String runtimeURI, boolean continuousSimulation) {
        this.bucketSize = bucketSize;
        this.numTransactions = numTransactions;
        this.runtimeURI = runtimeURI;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        // Configure runtime with event names to make the statements more readable.
        // This could also be done in a configuration file.
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType("TxnEventA", TxnEventA.class.getName());
        configuration.getCommon().addEventType("TxnEventB", TxnEventB.class.getName());
        configuration.getCommon().addEventType("TxnEventC", TxnEventC.class.getName());

        // Get runtime instance
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        // We will be supplying timer events externally.
        // We will assume that each bucket arrives within a defined period of time.
        runtime.getEventService().clockExternal();

        // Set up statement for listening to combined events
        CombinedEventStmt combinedEventStmt = new CombinedEventStmt(runtime);
        combinedEventStmt.addListener(new CombinedEventListener());

        // Set up statements for realtime summary latency data - overall totals and totals per customer and per supplier
        RealtimeSummaryStmt realtimeSummaryStmt = new RealtimeSummaryStmt(runtime);
        realtimeSummaryStmt.addTotalsListener(new RealtimeSummaryTotalsListener());
        realtimeSummaryStmt.addByCustomerListener(new RealtimeSummaryGroupListener("customerId"));
        realtimeSummaryStmt.addBySupplierListener(new RealtimeSummaryGroupListener("supplierId"));

        // Set up statement for finding missing events
        FindMissingEventStmt findMissingEventStmt = new FindMissingEventStmt(runtime);
        findMissingEventStmt.addListener(new FindMissingEventListener());

        // The feeder to feed the runtime
        FeederOutputStream feeder = new FeederOutputStream(runtime.getEventService());

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

        runtime.destroy();
    }
}
