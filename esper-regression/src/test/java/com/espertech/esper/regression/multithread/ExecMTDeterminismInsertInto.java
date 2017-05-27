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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.GeneratorIterator;
import com.espertech.esper.supportregression.multithread.SendEventRWLockCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class ExecMTDeterminismInsertInto implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecMTDeterminismInsertInto.class);

    public void run(EPServiceProvider epService) throws Exception {
        trySendCountFollowedBy(4, 100, ConfigurationEngineDefaults.Threading.Locking.SUSPEND);
        trySendCountFollowedBy(4, 100, ConfigurationEngineDefaults.Threading.Locking.SPIN);
        tryChainedCountSum(epService, 3, 100);
        tryMultiInsertGroup(epService, 3, 10, 100);
    }

    private void tryMultiInsertGroup(EPServiceProvider engine, int numThreads, int numStatements, int numEvents) throws Exception {
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        // setup statements
        EPStatement[] insertIntoStmts = new EPStatement[numStatements];
        for (int i = 0; i < numStatements; i++) {
            insertIntoStmts[i] = engine.getEPAdministrator().createEPL("insert into MyStream select " + i + " as ident,count(*) as cnt from " + SupportBean.class.getName());
        }
        EPStatement stmtInsertTwo = engine.getEPAdministrator().createEPL("select ident, sum(cnt) as mysum from MyStream group by ident");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtInsertTwo.addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, engine, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        EventBean[] newEvents = listener.getNewDataListFlattened();
        ArrayList[] resultsPerIdent = new ArrayList[numStatements];
        for (EventBean theEvent : newEvents) {
            int ident = (Integer) theEvent.get("ident");
            if (resultsPerIdent[ident] == null) {
                resultsPerIdent[ident] = new ArrayList();
            }
            long mysum = (Long) theEvent.get("mysum");
            resultsPerIdent[ident].add(mysum);
        }

        for (int statement = 0; statement < numStatements; statement++) {
            for (int i = 0; i < numEvents - 1; i++) {
                long expected = total(i + 1);
                assertEquals(expected, resultsPerIdent[statement].get(i));
            }
        }

        // destroy
        for (int i = 0; i < numStatements; i++) {
            insertIntoStmts[i].destroy();
        }
        stmtInsertTwo.destroy();
    }

    private void tryChainedCountSum(EPServiceProvider epService, int numThreads, int numEvents) throws Exception {
        // setup statements
        EPStatement stmtInsertOne = epService.getEPAdministrator().createEPL("insert into MyStreamOne select count(*) as cnt from " + SupportBean.class.getName());
        EPStatement stmtInsertTwo = epService.getEPAdministrator().createEPL("insert into MyStreamTwo select sum(cnt) as mysum from MyStreamOne");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtInsertTwo.addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, epService, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        EventBean[] newEvents = listener.getNewDataListFlattened();
        for (int i = 0; i < numEvents - 1; i++) {
            long expected = total(i + 1);
            assertEquals(expected, newEvents[i].get("mysum"));
        }

        stmtInsertOne.destroy();
        stmtInsertTwo.destroy();
    }

    private long total(int num) {
        long total = 0;
        for (int i = 1; i < num + 1; i++) {
            total += i;
        }
        return total;
    }

    private void trySendCountFollowedBy(int numThreads, int numEvents, ConfigurationEngineDefaults.Threading.Locking locking) throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInsertIntoDispatchLocking(locking);
        config.getEngineDefaults().getThreading().setInsertIntoDispatchTimeout(5000); // 5 second timeout
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        EPServiceProvider engine = EPServiceProviderManager.getProvider(this.getClass().getSimpleName(), config);
        engine.initialize();

        // setup statements
        EPStatement stmtInsert = engine.getEPAdministrator().createEPL("insert into MyStream select count(*) as cnt from " + SupportBean.class.getName());
        stmtInsert.addListener(new UpdateListener() {

            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                log.debug(".update cnt=" + newEvents[0].get("cnt"));
            }
        });

        SupportUpdateListener[] listeners = new SupportUpdateListener[numEvents];
        for (int i = 0; i < numEvents; i++) {
            String text = "select * from pattern [MyStream(cnt=" + (i + 1) + ") -> MyStream(cnt=" + (i + 2) + ")]";
            EPStatement stmt = engine.getEPAdministrator().createEPL(text);
            listeners[i] = new SupportUpdateListener();
            stmt.addListener(listeners[i]);
        }

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, engine, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        for (int i = 0; i < numEvents - 1; i++) {
            assertEquals("Listener not invoked: #" + i, 1, listeners[i].getNewDataList().size());
        }

        engine.destroy();
    }
}
