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
package com.espertech.esper.example.stockticker;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.stockticker.eventbean.PriceLimit;
import com.espertech.esper.example.stockticker.eventbean.StockTick;
import com.espertech.esper.example.stockticker.monitor.StockTickerMonitor;
import com.espertech.esper.example.stockticker.monitor.StockTickerResultListener;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestStockTickerMultithreaded extends TestCase implements StockTickerRegressionConstants {
    StockTickerResultListener listener;
    private EPServiceProvider epService;

    protected void setUp() throws Exception {
        listener = new StockTickerResultListener();

        Configuration configuration = new Configuration();
        configuration.addEventType("PriceLimit", PriceLimit.class.getName());
        configuration.addEventType("StockTick", StockTick.class.getName());

        epService = EPServiceProviderManager.getProvider("TestStockTickerMultithreaded", configuration);
        epService.initialize();
        new StockTickerMonitor(epService, listener);
    }

    public void tearDown() throws Exception {
        epService.destroy();
    }

    public void testMultithreaded() {
        //performTest(3, 1000000, 100000, 60);  // on fast systems
        performTest(3, 50000, 10000, 15);   // for unit tests on slow machines
    }

    public void performTest(int numberOfThreads,
                            int numberOfTicksToSend,
                            int ratioPriceOutOfLimit,
                            int numberOfSecondsWaitForCompletion) {
        final int totalNumTicks = numberOfTicksToSend + 2 * TestStockTickerGenerator.NUM_STOCK_NAMES;

        log.info(".performTest Generating data, numberOfTicksToSend=" + numberOfTicksToSend +
                "  ratioPriceOutOfLimit=" + ratioPriceOutOfLimit);

        StockTickerEventGenerator generator = new StockTickerEventGenerator();
        LinkedList stream = generator.makeEventStream(numberOfTicksToSend, ratioPriceOutOfLimit, TestStockTickerGenerator.NUM_STOCK_NAMES,
                StockTickerRegressionConstants.PRICE_LIMIT_PCT_LOWER_LIMIT, StockTickerRegressionConstants.PRICE_LIMIT_PCT_UPPER_LIMIT,
                StockTickerRegressionConstants.PRICE_LOWER_LIMIT, StockTickerRegressionConstants.PRICE_UPPER_LIMIT, true);

        log.info(".performTest Send limit and initial tick events - singlethreaded");
        for (int i = 0; i < TestStockTickerGenerator.NUM_STOCK_NAMES * 2; i++) {
            Object theEvent = stream.removeFirst();
            epService.getEPRuntime().sendEvent(theEvent);
        }

        log.info(".performTest Loading thread pool work queue, numberOfRunnables=" + stream.size());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(0, numberOfThreads, 99999, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        for (Object theEvent : stream) {
            SendEventRunnable runnable = new SendEventRunnable(epService, theEvent);
            pool.execute(runnable);
        }

        log.info(".performTest Starting thread pool, threads=" + numberOfThreads);
        pool.setCorePoolSize(numberOfThreads);

        log.info(".performTest Listening for completion");
        EPRuntimeUtil.awaitCompletion(epService.getEPRuntime(), totalNumTicks, numberOfSecondsWaitForCompletion, 1, 10);

        pool.shutdown();

        // Check results : make sure the given ratio of out-of-limit stock prices was reported
        int expectedNumEmitted = (numberOfTicksToSend / ratioPriceOutOfLimit) + 1;
        assertTrue(listener.getSize() == expectedNumEmitted);

        log.info(".performTest Done test");
    }

    private static final Logger log = LoggerFactory.getLogger(TestStockTickerMultithreaded.class);
}
