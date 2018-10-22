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
package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FeedSimMain implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FeedSimMain.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3) {
            System.out.println("Arguments are: <number of threads> <drop probability percent> <number of seconds to run>");
            System.out.println("  number of threads: the number of threads sending feed events into the runtime");
            System.out.println("  drop probability percent: a number between zero and 100 that dictates the ");
            System.out.println("                            probability that per second one of the feeds drops off");
            System.out.println("  number of seconds: the number of seconds the simulation runs");
            System.exit(-1);
        }

        int numberOfThreads;
        try {
            numberOfThreads = Integer.parseInt(args[0]);
        } catch (NullPointerException e) {
            System.out.println("Invalid number of threads:" + args[0]);
            System.exit(-2);
            return;
        }

        double dropProbability;
        try {
            dropProbability = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid drop probability:" + args[1]);
            System.exit(-2);
            return;
        }

        int numberOfSeconds;
        try {
            numberOfSeconds = Integer.parseInt(args[2]);
        } catch (NullPointerException e) {
            System.out.println("Invalid number of seconds to run:" + args[2]);
            System.exit(-2);
            return;
        }

        // Run the sample
        System.out.println("Using " + numberOfThreads + " threads with a drop probability of " + dropProbability + "%, for " + numberOfSeconds + " seconds");
        FeedSimMain feedSimMain = new FeedSimMain(numberOfThreads, dropProbability, numberOfSeconds, true, "FeedSimMain", false);
        feedSimMain.run();
    }

    private int numberOfThreads;
    private double dropProbability;
    private int numSeconds;
    private boolean isWaitKeypress;
    private String runtimeURI;
    private boolean continuousSimulation;

    public FeedSimMain(int numberOfThreads, double dropProbability, int numSeconds, boolean isWaitKeypress, String runtimeURI, boolean continuousSimulation) {
        this.numberOfThreads = numberOfThreads;
        this.dropProbability = dropProbability;
        this.numSeconds = numSeconds;
        this.isWaitKeypress = isWaitKeypress;
        this.runtimeURI = runtimeURI;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        if (isWaitKeypress) {
            System.out.println("...press enter to start simulation...");
            try {
                System.in.read();
            } catch (IOException e) {
                log.error("Exception reading keyboard input: " + e.getMessage(), e);
            }
        }

        // Configure runtime with event names to make the statements more readable.
        // This could also be done in a configuration file.
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType("MarketDataEvent", MarketDataEvent.class.getName());

        // Get runtime
        EPRuntime runtime = EPRuntimeProvider.getRuntime(runtimeURI, configuration);

        // Set up statements
        TicksPerSecondStatement tickPerSecStmt = new TicksPerSecondStatement(runtime.getDeploymentService(), configuration);
        tickPerSecStmt.addListener(new RateReportingListener());

        TicksFalloffStatement falloffStmt = new TicksFalloffStatement(runtime.getDeploymentService(), configuration, runtime.getRuntimePath());
        falloffStmt.addListener(new RateFalloffAlertListener());

        // For continuous non-ending simulation
        if (continuousSimulation) {
            new MarketDataSendRunnable(runtime, true).run();
        } else {
            // Send events
            ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
            MarketDataSendRunnable[] runnables = new MarketDataSendRunnable[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                runnables[i] = new MarketDataSendRunnable(runtime, false);
                threadPool.submit(runnables[i]);
            }

            int seconds = 0;
            Random random = new Random();
            while (seconds < numSeconds) {
                seconds++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.info("Interrupted", e);
                    break;
                }

                FeedEnum feedToDropOff;
                if (random.nextDouble() * 100 < dropProbability) {
                    feedToDropOff = FeedEnum.FEED_A;
                    if (random.nextBoolean()) {
                        feedToDropOff = FeedEnum.FEED_B;
                    }
                    log.info("Setting drop-off for feed " + feedToDropOff);

                } else {
                    feedToDropOff = null;
                }
                for (int i = 0; i < runnables.length; i++) {
                    runnables[i].setRateDropOffFeed(feedToDropOff);
                }
            }

            log.info("Shutting down threadpool");
            for (int i = 0; i < runnables.length; i++) {
                runnables[i].setShutdown();
            }
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // no action
            }
        }

        runtime.destroy();
    }
}
