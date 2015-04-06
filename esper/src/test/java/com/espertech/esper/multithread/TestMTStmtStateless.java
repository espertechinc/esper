/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.support.bean.word.SentenceEvent;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestMTStmtStateless extends TestCase
{
    private static Log log = LogFactory.getLog(TestMTStmtStateless.class);
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        engine = EPServiceProviderManager.getDefaultProvider(config);
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testStateless() throws Exception
    {
        trySend(4, 1000);
    }

    private void trySend(int numThreads, int numRepeats) throws Exception
    {
        engine.getEPAdministrator().getConfiguration().addEventType(SentenceEvent.class);
        EPStatementSPI spi = (EPStatementSPI) engine.getEPAdministrator().createEPL("select * from SentenceEvent[words]");
        assertTrue(spi.getStatementContext().isStatelessSelect());

        StatelessRunnable[] runnables = new StatelessRunnable[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = new StatelessRunnable(engine, numRepeats);
        }

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < runnables.length; i++) {
            threads[i] = new Thread(runnables[i]);
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }
        long delta = System.currentTimeMillis() - start;
        log.info("Delta=" + delta + " for " + numThreads*numRepeats + " events");

        for (StatelessRunnable r : runnables) {
            assertNull(r.getException());
        }
    }

    public static class StatelessRunnable implements Runnable {

        private final EPServiceProvider engine;
        private final int numRepeats;

        private Throwable exception;

        public StatelessRunnable(EPServiceProvider engine, int numRepeats) {
            this.engine = engine;
            this.numRepeats = numRepeats;
        }

        public void run() {
            try {
                for (int i = 0; i < numRepeats; i++) {
                    engine.getEPRuntime().sendEvent(new SentenceEvent("This is stateless statement testing"));

                    if (i % 10000 == 0) {
                        log.info("Thread " + Thread.currentThread().getId() + " sending event " + i);
                    }
                }
            }
            catch (Throwable t) {
                exception = t;
            }
        }

        public Throwable getException() {
            return exception;
        }
    }
}
