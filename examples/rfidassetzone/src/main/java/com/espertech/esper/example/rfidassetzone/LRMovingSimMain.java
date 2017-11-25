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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Performance test for the following problem and statements:
 * <p>
 * <quote>If a given set of assets are not moving together from zone to zone, alert</quote>
 * <p>
 * Statements:
 * <pre>
 * insert into CountZone_[Nx] select [Nx] as groupId, zone, count(*) as cnt
 * from LocationReport(assetId in ([aNx1], [aNx2], [aNx3]))#unique(assetId)
 * group by zone
 *
 * select * from pattern [every a=CountZone_[Nx](cnt in [1:2]) ->
 * (timer:interval(10 sec) and not CountZone_[Nx](cnt in (0, 3)))]
 * </pre>
 * <p>
 * This performance test works as follows:
 * <OL>
 * <LI> Assume N is the number of asset groups (numAssetGroups), each group consisting of 3 assets
 * <LI> Generate unique assets ids for N*3 assets, assign a random start zone for each asset group
 * <LI> Create 2 times N statements: the first N statements count the assets per zones for the statement's asset group,
 * and generates stream CountZone_[Nx] where Nx=0..N
 * The second statement detects a pattern among each asset group's event stream CountZone_[Nx] where assets are split
 * between zones for more then 10 seconds.
 * <LI> Send one event for each asset to start of each asset in the assigned zone
 * <LI> Create M number of callables and an executor services, assigning each callable a range of asset groups.
 * For example, with 1000 asset groups and 3 callables (threads) then each callable gets 333 asset groups assigned to it. The callable only
 * sends events for the assigned asset group. The main thread starts the executor service.
 * <LI> Each callable enters a processing loop until a shutdown flag is set
 * <LI> If a random number integer number modulo the ratio of zone moves is 1, then the callable moves one asset group from zone to zone.
 * For this, it determines a random asset group and new zone and sends 3 events moving the 3 assets to the new zone.
 * <LI> If a random number integer number modulo the ratio of zone splits is 1, then the callable moves 2 of the 3 assets in
 * a random asset group to a new zone, and leaves one asset in the group in the old zone. It saves the asset group number
 * in a collection since this information is needed to reconciled later.
 * <LI> If neither random number matches, then the callable picks a random asset group and resends all 3 asset location
 * report events for the current zone for that asset.
 * <LI> The main thread runs for the given number of seconds, sleeps 1 seconds and compiles statistics for reporting
 * by asking each callable for events generated
 * <LI> At 15 seconds before the end of the test the main thread invokes a setter method on all callables to stop
 * generating split asset groups.
 * <LI> The main thread stops the executor service
 * <LI> The main thread reconciles the events received by listeners with the asset groups that were split by any callables.
 * </OL>
 */
public class LRMovingSimMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(LRMovingSimMain.class);

    private final int numberOfThreads;
    private final int numberOfAssetGroups;
    private final int numberOfSeconds;
    private boolean isAssert;
    private String engineURI;
    private boolean continuousSimulation;

    private EPServiceProvider epService;
    private Random random = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Arguments are: <number of threads> <number of asset groups> <number of seconds to run>");
            System.out.println("  number of threads: the number of threads sending events into the engine (e.g. 4)");
            System.out.println("  number of asset groups: number of groups tracked (e.g. 1000)");
            System.out.println("  number of seconds: the number of seconds the simulation runs (e.g. 60)");
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

        int numberOfAssetGroups;
        try {
            numberOfAssetGroups = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number of asset groups:" + args[1]);
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
        System.out.println("Using " + numberOfThreads + " threads and " + numberOfAssetGroups + " asset groups, for " + numberOfSeconds + " seconds");
        LRMovingSimMain simMain = new LRMovingSimMain(numberOfThreads, numberOfAssetGroups, numberOfSeconds, false, "LRMovingExampleURI", false);
        simMain.run();
    }

    public LRMovingSimMain(int numberOfThreads, int numberOfAssetGroups, int numberOfSeconds, boolean isAssert, String engineURI, boolean continuousSimulation) {
        this.numberOfThreads = numberOfThreads;
        this.numberOfAssetGroups = numberOfAssetGroups;
        this.numberOfSeconds = numberOfSeconds;
        this.isAssert = isAssert;
        this.engineURI = engineURI;

        Configuration config = new Configuration();
        config.addEventType("LocationReport", LocationReport.class);

        epService = EPServiceProviderManager.getProvider(engineURI, config);
        epService.initialize();
    }

    public void run() {
        // Number of seconds the total test runs
        int numSeconds = numberOfSeconds;    // usually 60

        // Number of asset groups
        int numAssetGroups = numberOfAssetGroups;      // usually 1000

        // Number of threads
        int numThreads = numberOfThreads;

        // Ratio of events indicating that all assets moved to a new zone
        int ratioZoneMove = 3;

        // Ratio of events indicating that the asset group split between zones, i.e. only some assets in a group move to a new zone
        int ratioZoneSplit = 1000000;       // usually 1000000;

        tryPerf(numSeconds, numAssetGroups, numThreads, ratioZoneMove, ratioZoneSplit);

        epService.destroy();
    }

    private void tryPerf(int numSeconds, int numAssetGroups, int numThreads, int ratioZoneMove, int ratioZoneSplit) {
        // Create Asset Ids and assign to a zone
        log.info(".tryPerf Creating asset ids");
        String[][] assetIds = new String[numAssetGroups][3];
        int[][] zoneIds = new int[numAssetGroups][3];
        for (int i = 0; i < numAssetGroups; i++) {
            // Generate unique asset id over all groups
            String assetPrefix = String.format("%010d", i); // 10 digit zero padded, i.e. 00000001.n;
            assetIds[i][0] = assetPrefix + "0";
            assetIds[i][1] = assetPrefix + "1";
            assetIds[i][2] = assetPrefix + "2";

            int currentZone = Math.abs(random.nextInt()) % AssetEventGenCallable.NUM_ZONES;
            zoneIds[i][0] = currentZone;
            zoneIds[i][1] = currentZone;
            zoneIds[i][2] = currentZone;
        }

        // Create statements
        log.info(".tryPerf Creating " + numAssetGroups * 2 + " statements for " + numAssetGroups + " asset groups");
        AssetZoneSplitListener[] listeners = new AssetZoneSplitListener[numAssetGroups];
        for (int i = 0; i < numAssetGroups; i++) {
            String streamName = "CountZone_" + i;
            String assetIdList = "'" + assetIds[i][0] + "','" + assetIds[i][1] + "','" + assetIds[i][2] + "'";

            String textOne = "insert into " + streamName +
                    " select " + i + " as groupId, zone, count(*) as cnt " +
                    "from LocationReport(assetId in (" + assetIdList + "))#unique(assetId) " +
                    "group by zone";
            EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
            if (log.isDebugEnabled()) stmtOne.addListener(new AssetGroupCountListener()); //for debugging

            String textTwo = "select * from pattern [" +
                    "  every a=" + streamName + "(cnt in [1:2]) ->" +
                    "  (timer:interval(10 sec) and not " + streamName + "(cnt in (0, 3)))]";
            EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
            listeners[i] = new AssetZoneSplitListener();
            stmtTwo.addListener(listeners[i]);
        }

        // First, send an event for each asset with it's current zone
        log.info(".tryPerf Sending one event for each asset");
        for (int i = 0; i < assetIds.length; i++) {
            for (int j = 0; j < assetIds[i].length; j++) {
                LocationReport report = new LocationReport(assetIds[i][j], zoneIds[i][j]);
                epService.getEPRuntime().sendEvent(report);
            }
        }

        // Reset listeners
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].reset();
        }
        Integer[][] assetGroupsForThread = getGroupsPerThread(numAssetGroups, numThreads);

        // For continuous simulation (ends when interrupted),
        if (continuousSimulation) {
            while (true) {
                AssetEventGenCallable callable = new AssetEventGenCallable(epService, assetIds, zoneIds, assetGroupsForThread[0], ratioZoneMove, ratioZoneSplit);
                try {
                    callable.call();
                } catch (Exception ex) {
                    log.warn("Exception simulating in continuous mode: " + ex.getMessage(), ex);
                    break;
                }
            }
            return;
        }

        // Create threadpool
        log.info(".tryPerf Starting " + numThreads + " threads");
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        AssetEventGenCallable[] callables = new AssetEventGenCallable[numThreads];

        for (int i = 0; i < numThreads; i++) {
            callables[i] = new AssetEventGenCallable(epService, assetIds, zoneIds, assetGroupsForThread[i], ratioZoneMove, ratioZoneSplit);
            Future<Boolean> f = threadPool.submit(callables[i]);
            future[i] = f;
        }

        // Create threadpool
        log.info(".tryPerf Running for " + numSeconds + " seconds");
        long startTime = System.currentTimeMillis();
        long currTime;
        double deltaSeconds;
        int lastTotalEvents = 0;
        do {
            // sleep
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.debug("Interrupted", e);
                break;
            }
            currTime = System.currentTimeMillis();
            deltaSeconds = (currTime - startTime) / 1000.0;

            // report statistics
            int totalEvents = 0;
            int totalZoneMoves = 0;
            int totalZoneSplits = 0;
            int totalZoneSame = 0;
            for (int i = 0; i < callables.length; i++) {
                totalEvents += callables[i].getNumEventsSend();
                totalZoneMoves += callables[i].getNumZoneMoves();
                totalZoneSplits += callables[i].getNumZoneSplits();
                totalZoneSame += callables[i].getNumSameZone();
            }
            double throughputOverall = totalEvents / deltaSeconds;
            double totalLastBatch = totalEvents - lastTotalEvents;
            log.info("totalEvents=" + totalEvents +
                    " delta=" + deltaSeconds +
                    " throughputOverall=" + throughputOverall +
                    " lastBatch=" + totalLastBatch +
                    " zoneMoves=" + totalZoneMoves +
                    " zoneSame=" + totalZoneSame +
                    " zoneSplits=" + totalZoneSplits
            );
            lastTotalEvents = totalEvents;

            // If we are within 15 seconds of shutdown, stop generating zone splits
            if (((numSeconds - deltaSeconds) < 15) && (callables[0].isGenerateZoneSplit())) {
                log.info(".tryPerf Setting stop split flag on threads");
                for (int i = 0; i < callables.length; i++) {
                    callables[i].setGenerateZoneSplit(false);
                }
            }
        }
        while (deltaSeconds < numSeconds);

        log.info(".tryPerf Shutting down threads");
        for (int i = 0; i < callables.length; i++) {
            callables[i].setShutdown(true);
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug("Interrupted", e);
        }

        if (!isAssert) {
            return;
        }

        for (int i = 0; i < numThreads; i++) {
            try {
                if (!(Boolean) future[i].get()) {
                    throw new RuntimeException("Invalid result of callable");
                }
            } catch (Exception e) {
                log.error("Exception encountered sending events: " + e.getMessage(), e);
            }
        }

        // Get groups split
        Set<Integer> splitGroups = new HashSet<Integer>();
        for (int i = 0; i < callables.length; i++) {
            splitGroups.addAll(callables[i].getSplitZoneGroups());
        }
        log.info(".tryPerf Generated splits were " + splitGroups + " groups");

        // Compare to listeners
        for (Integer groupId : splitGroups) {
            if (listeners[groupId].getCallbacks().size() == 0) {
                throw new RuntimeException("Invalid result for listener, expected split group");
            }
        }
    }

    // Subdivide say 1000 groups into 3 threads, i.e. 0 - 333, 334 to 666, 667 - 999 (roughly)
    private Integer[][] getGroupsPerThread(int numGroups, int numThreads) {
        Integer[][] result = new Integer[numThreads][];
        int bucketSize = numGroups / numThreads;
        for (int i = 0; i < numThreads; i++) {
            int start = i * bucketSize;
            int end = start + bucketSize;
            List<Integer> groups = new ArrayList<Integer>();

            for (int j = start; j < end; j++) {
                groups.add(j);
            }

            result[i] = groups.toArray(new Integer[0]);
            log.info(".tryPerf Thread " + i + " getting groups " + result[i][0] + " to " + result[i][result[i].length - 1]);
        }
        return result;
    }
}
