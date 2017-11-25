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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
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
            System.out.println("  number of threads: the number of threads sending feed events into the engine");
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
    private String engineURI;
    private boolean continuousSimulation;

    public FeedSimMain(int numberOfThreads, double dropProbability, int numSeconds, boolean isWaitKeypress, String engineURI, boolean continuousSimulation) {
        this.numberOfThreads = numberOfThreads;
        this.dropProbability = dropProbability;
        this.numSeconds = numSeconds;
        this.isWaitKeypress = isWaitKeypress;
        this.engineURI = engineURI;
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

        // Configure engine with event names to make the statements more readable.
        // This could also be done in a configuration file.
        Configuration configuration = new Configuration();
        configuration.addEventType("MarketDataEvent", MarketDataEvent.class.getName());

        // Get engine instance
        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, configuration);

        // Set up statements
        TicksPerSecondStatement tickPerSecStmt = new TicksPerSecondStatement(epService.getEPAdministrator());
        tickPerSecStmt.addListener(new RateReportingListener());

        TicksFalloffStatement falloffStmt = new TicksFalloffStatement(epService.getEPAdministrator());
        falloffStmt.addListener(new RateFalloffAlertListener());

        // For continuous non-ending simulation
        if (continuousSimulation) {
            new MarketDataSendRunnable(epService, true).run();
        } else {
            // Send events
            ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
            MarketDataSendRunnable[] runnables = new MarketDataSendRunnable[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                runnables[i] = new MarketDataSendRunnable(epService, false);
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

        epService.destroy();
    }
}
