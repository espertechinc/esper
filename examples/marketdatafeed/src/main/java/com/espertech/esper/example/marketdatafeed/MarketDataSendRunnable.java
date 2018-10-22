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

import com.espertech.esper.runtime.client.EPRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class MarketDataSendRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MarketDataSendRunnable.class);
    private final EPRuntime runtime;
    private final boolean continuousSimulation;

    private volatile FeedEnum rateDropOffFeed;
    private volatile boolean isShutdown;
    private Random random = new Random();

    public MarketDataSendRunnable(EPRuntime runtime, boolean continuousSimulation) {
        this.runtime = runtime;
        this.continuousSimulation = continuousSimulation;
    }

    public void run() {
        log.info(".call Thread " + Thread.currentThread() + " starting");

        try {
            while (!isShutdown) {
                int nextFeed = Math.abs(random.nextInt() % 2);
                FeedEnum feed = FeedEnum.values()[nextFeed];
                if (rateDropOffFeed != feed) {
                    runtime.getEventService().sendEventBean(new MarketDataEvent("SYM", feed), "MarketDataEvent");
                }

                if (continuousSimulation) {
                    Thread.sleep(200);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Error in send loop", ex);
        } catch (InterruptedException e) {
            log.debug("Interruped", e);
        }

        log.info(".call Thread " + Thread.currentThread() + " done");
    }

    public void setRateDropOffFeed(FeedEnum feedToDrop) {
        rateDropOffFeed = feedToDrop;
    }

    public void setShutdown() {
        isShutdown = true;
    }
}
