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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for N threads feeding events that affect M statements which employ a small time window.
 * Each of the M statements is associated with a symbol and each event send hits exactly one
 * statement only.
 * <p>
 * Thus the timer is fairly busy when active, competing with N application threads.
 * Created for ESPER-59 Internal Threading Bugs Found.
 * <p>
 * Exceptions can occur in
 * (1) an application thread during sendEvent() outside of the listener, causes the test to fail
 * (2) an application thread during sendEvent() inside of the listener, causes assertion to fail
 * (3) the timer thread, causes an exception to be logged and assertion *may* fail
 */
public class MultithreadViewTimeWindowSceneTwo implements RegressionExecution {

    private Thread[] threads;
    private ResultUpdateListener[] listeners;

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        int numSymbols = 1;
        int numThreads = 4;
        int numEventsPerThread = 50000;
        double timeWindowSize = 0.2;

        // Set up threads, statements and listeners
        setUp(env, numSymbols, numThreads, numEventsPerThread, timeWindowSize);

        // Start threads
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.run();
        }

        // Wait for completion
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        long endTime = System.currentTimeMillis();

        // Check listener results
        long totalReceived = 0;
        for (ResultUpdateListener listener : listeners) {
            totalReceived += listener.getNumReceived();
            assertFalse(listener.isCaughtRuntimeException());
        }
        double numTimeWindowAdvancements = (endTime - startTime) / 1000 / timeWindowSize;

        log.info("Completed, expected=" + numEventsPerThread * numThreads +
            " numTimeWindowAdvancements=" + numTimeWindowAdvancements +
            " totalReceived=" + totalReceived);
        assertTrue(totalReceived < numEventsPerThread * numThreads + numTimeWindowAdvancements + 1);
        assertTrue(totalReceived >= numEventsPerThread * numThreads);

        listeners = null;
        threads = null;

        env.undeployAll();
    }

    private void setUp(RegressionEnvironment env, int numSymbols, int numThreads, int numEvents, double timeWindowSize) {
        threads = new Thread[numThreads];
        listeners = new ResultUpdateListener[numSymbols];

        // Create a statement for N number of symbols, each it's own listener
        String[] symbols = new String[numSymbols];
        listeners = new ResultUpdateListener[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            String annotation = String.format("@name('%s')", "stmt_" + i);
            symbols[i] = "S" + i;
            String epl = annotation + "select symbol, sum(volume) as sumVol from SupportMarketDataBean(symbol='" + symbols[i] + "')#time(" + timeWindowSize + ")";
            env.compileDeploy(epl);
            EPStatement testStmt = env.statement("stmt_" + i);
            listeners[i] = new ResultUpdateListener();
            testStmt.addListener(listeners[i]);
        }

        // Create threads to send events
        TimeWinRunnable[] runnables = new TimeWinRunnable[threads.length];
        ReentrantLock lock = new ReentrantLock();
        for (int i = 0; i < threads.length; i++) {
            runnables[i] = new TimeWinRunnable(i, env, lock, symbols, numEvents);
            threads[i] = new Thread(runnables[i], MultithreadViewTimeWindowSceneTwo.class.getSimpleName());
        }
    }

    public static class TimeWinRunnable implements Runnable {
        private final int threadNum;
        private final RegressionEnvironment env;
        private final ReentrantLock sharedLock;
        private final String[] symbols;
        private final int numberOfEvents;

        public TimeWinRunnable(int threadNum, RegressionEnvironment env, ReentrantLock sharedLock, String[] symbols, int numberOfEvents) {
            this.threadNum = threadNum;
            this.env = env;
            this.sharedLock = sharedLock;
            this.symbols = symbols;
            this.numberOfEvents = numberOfEvents;
        }

        public void run() {

            for (int i = 0; i < numberOfEvents; i++) {
                int symbolNum = (threadNum + numberOfEvents) % symbols.length;
                String symbol = symbols[symbolNum];
                long volume = 1;

                Object theEvent = new SupportMarketDataBean(symbol, -1, volume, null);

                sharedLock.lock();
                try {
                    env.sendEventBean(theEvent);
                } finally {
                    sharedLock.unlock();
                }
            }
        }
    }

    public static class ResultUpdateListener implements UpdateListener {
        private boolean isCaughtRuntimeException;
        private int numReceived = 0;
        private String lastSymbol = null;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            if ((newEvents == null) || (newEvents.length == 0)) {
                return;
            }

            try {
                numReceived += newEvents.length;

                String symbol = (String) newEvents[0].get("symbol");
                if (lastSymbol != null) {
                    Assert.assertEquals(lastSymbol, symbol);
                } else {
                    lastSymbol = symbol;
                }
            } catch (RuntimeException ex) {
                log.error("Unexpected exception querying results", ex);
                isCaughtRuntimeException = true;
                throw ex;
            }
        }

        public int getNumReceived() {
            return numReceived;
        }

        public boolean isCaughtRuntimeException() {
            return isCaughtRuntimeException;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MultithreadViewTimeWindowSceneTwo.class);
}
