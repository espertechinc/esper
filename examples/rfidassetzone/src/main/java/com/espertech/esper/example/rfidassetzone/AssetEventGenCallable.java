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

import com.espertech.esper.client.EPServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

public class AssetEventGenCallable implements Callable<Boolean> {
    public static final int NUM_ZONES = 20;

    private static final Logger log = LoggerFactory.getLogger(AssetEventGenCallable.class);
    private final EPServiceProvider engine;
    private final String[][] assetIds;
    private final int[][] zoneIds;
    private final Integer[] assetGroupsForThread;
    private final int ratioZoneMove;
    private final int ratioZoneSplit;

    private int numEventsSend;
    private int numZoneMoves;
    private int numZoneSplits;
    private int numSameZone;
    private Set<Integer> splitZoneGroups = new HashSet<Integer>();
    private Random random = new Random();

    private boolean shutdown;
    private boolean isGenerateZoneSplit;

    public AssetEventGenCallable(EPServiceProvider engine, String[][] assetIds, int[][] zoneIds, Integer[] assetGroupsForThread, int ratioZoneMove, int ratioZoneSplit) {
        this.engine = engine;
        this.assetIds = assetIds;
        this.zoneIds = zoneIds;
        this.assetGroupsForThread = assetGroupsForThread;
        this.ratioZoneMove = ratioZoneMove;
        this.ratioZoneSplit = ratioZoneSplit;
        isGenerateZoneSplit = true;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public void setGenerateZoneSplit(boolean generateZoneSplit) {
        isGenerateZoneSplit = generateZoneSplit;
    }

    public boolean isGenerateZoneSplit() {
        return isGenerateZoneSplit;
    }

    public int getNumEventsSend() {
        return numEventsSend;
    }

    public Boolean call() throws Exception {
        try {
            log.info(".call Thread " + Thread.currentThread().getId() + " starting");
            while (!shutdown) {
                boolean isZoneMove = (random.nextInt() % ratioZoneMove) == 1;
                boolean isZoneSplit = (random.nextInt() % ratioZoneSplit) == 1;
                if (isZoneMove) {
                    doZoneMove();
                } else if (isZoneSplit && isGenerateZoneSplit) {
                    doZoneSplit();
                } else {
                    doSameZone();
                }
            }
            log.info(".call Thread " + Thread.currentThread().getId() + " done");
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private void doZoneMove() {
        // Chose among one of the groups for this thread
        int index = Math.abs(random.nextInt()) % assetGroupsForThread.length;
        int groupNum = assetGroupsForThread[index];

        // If this is a currently-split group, don't reunion
        if (splitZoneGroups.contains(groupNum)) {
            return;
        }

        // Determine zone to move to
        int newZone;
        do {
            newZone = Math.abs(random.nextInt()) % NUM_ZONES;
        }
        while (zoneIds[groupNum][0] == newZone);

        // Move all assets for this group to a new, random zone
        for (int i = 0; i < assetIds[i].length; i++) {
            zoneIds[groupNum][i] = newZone;
            LocationReport report = new LocationReport(assetIds[groupNum][i], newZone);
            engine.getEPRuntime().sendEvent(report);
            numEventsSend++;
        }
        numZoneMoves++;
    }

    private void doSameZone() {
        // Chose among one of the groups for this thread
        int index = Math.abs(random.nextInt()) % assetGroupsForThread.length;
        int groupNum = assetGroupsForThread[index];

        // If this is a currently-split group, don't reunion
        if (splitZoneGroups.contains(groupNum)) {
            return;
        }

        // Re-send all assets for this group as the same zone
        for (int i = 0; i < assetIds[i].length; i++) {
            LocationReport report = new LocationReport(assetIds[groupNum][i], zoneIds[groupNum][i]);
            engine.getEPRuntime().sendEvent(report);
            numEventsSend++;
        }
        numSameZone++;
    }

    private void doZoneSplit() {
        int groupNum;
        do {
            int index = Math.abs(random.nextInt()) % assetGroupsForThread.length;
            groupNum = assetGroupsForThread[index];
        }
        while (splitZoneGroups.contains(groupNum));
        splitZoneGroups.add(groupNum);

        // Determine zone to move to
        int oldZone = zoneIds[groupNum][0];
        int newZone;
        do {
            newZone = Math.abs(random.nextInt()) % NUM_ZONES;
        }
        while (zoneIds[groupNum][0] == newZone);

        log.info(".doZoneSplit Split group " + groupNum + " to different zones, from zone " + oldZone + " to zone " + newZone);

        // Move all assets for this group except the last asset to the new zone
        for (int i = 0; i < assetIds[i].length - 1; i++) {
            zoneIds[groupNum][i] = newZone;
            LocationReport report = new LocationReport(assetIds[groupNum][i], newZone);
            engine.getEPRuntime().sendEvent(report);
            numEventsSend++;
        }
        numZoneSplits++;
    }

    public int getNumZoneMoves() {
        return numZoneMoves;
    }

    public int getNumZoneSplits() {
        return numZoneSplits;
    }

    public int getNumSameZone() {
        return numSameZone;
    }

    public Set<Integer> getSplitZoneGroups() {
        return splitZoneGroups;
    }
}
