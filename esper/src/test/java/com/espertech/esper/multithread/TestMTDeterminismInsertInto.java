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

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class TestMTDeterminismInsertInto extends TestCase
{
    private static final Log log = LogFactory.getLog(TestMTDeterminismInsertInto.class);

    public void testSceneOneSuspend() throws Exception
    {
        trySendCountFollowedBy(4, 10000, ConfigurationEngineDefaults.Threading.Locking.SUSPEND);
    }

    public void testSceneOneSpin() throws Exception
    {
        trySendCountFollowedBy(4, 10000, ConfigurationEngineDefaults.Threading.Locking.SPIN);
    }

    public void testSceneTwo() throws Exception
    {
        tryChainedCountSum(3, 10000);
    }

    public void testSceneThree() throws Exception
    {
        tryMultiInsertGroup(3, 10, 1000);
    }

    private void tryMultiInsertGroup(int numThreads, int numStatements, int numEvents) throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        // setup statements
        EPStatement[] insertIntoStmts = new EPStatement[numStatements];
        for (int i = 0; i < numStatements; i++)
        {
            insertIntoStmts[i] = engine.getEPAdministrator().createEPL("insert into MyStream select " + i + " as ident,count(*) as cnt from " + SupportBean.class.getName());
        }
        EPStatement stmtInsertTwo = engine.getEPAdministrator().createEPL("select ident, sum(cnt) as mysum from MyStream group by ident");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtInsertTwo.addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++)
        {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, engine, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        EventBean newEvents[] = listener.getNewDataListFlattened();
        ArrayList resultsPerIdent[] = new ArrayList[numStatements];
        for (EventBean theEvent : newEvents)
        {
            int ident = (Integer)theEvent.get("ident");
            if (resultsPerIdent[ident] == null)
            {
                resultsPerIdent[ident] = new ArrayList();
            }
            long mysum = (Long) theEvent.get("mysum");
            resultsPerIdent[ident].add(mysum);
        }

        for (int statement = 0; statement < numStatements; statement++)
        {
            for (int i = 0; i < numEvents - 1; i++)
            {
                long expected = total(i + 1);
                assertEquals(expected, resultsPerIdent[statement].get(i));
            }
        }

        // destroy
        for (int i = 0; i < numStatements; i++)
        {
            insertIntoStmts[i].destroy();
        }
        stmtInsertTwo.destroy();
    }

    private void tryChainedCountSum(int numThreads, int numEvents) throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        // setup statements
        EPStatement stmtInsertOne = engine.getEPAdministrator().createEPL("insert into MyStreamOne select count(*) as cnt from " + SupportBean.class.getName());
        EPStatement stmtInsertTwo = engine.getEPAdministrator().createEPL("insert into MyStreamTwo select sum(cnt) as mysum from MyStreamOne");
        EPStatement stmtInsertThree = engine.getEPAdministrator().createEPL("select * from MyStreamTwo");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtInsertThree.addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++)
        {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, engine, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        EventBean newEvents[] = listener.getNewDataListFlattened();
        for (int i = 0; i < numEvents - 1; i++)
        {
            long expected = total(i + 1);
            assertEquals(expected, newEvents[i].get("mysum"));
        }

        stmtInsertOne.destroy();
        stmtInsertTwo.destroy();
        stmtInsertThree.destroy();
    }

    private long total(int num)
    {
        long total = 0;
        for (int i = 1; i < num + 1; i++)
        {
            total += i; 
        }
        return total;
    }

    private void trySendCountFollowedBy(int numThreads, int numEvents, ConfigurationEngineDefaults.Threading.Locking locking) throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInsertIntoDispatchLocking(locking);
        config.getEngineDefaults().getThreading().setInsertIntoDispatchTimeout(5000); // 5 second timeout
        // This should fail all test in this class
        // config.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);

        EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider(config);
        engine.initialize();

        // setup statements
        EPStatement stmtInsert = engine.getEPAdministrator().createEPL("insert into MyStream select count(*) as cnt from " + SupportBean.class.getName());
        stmtInsert.addListener(new UpdateListener() {

            public void update(EventBean[] newEvents, EventBean[] oldEvents)
            {
                log.debug(".update cnt=" + newEvents[0].get("cnt"));
            }
        });

        SupportUpdateListener listeners[] = new SupportUpdateListener[numEvents];
        for (int i = 0; i < numEvents; i++)
        {
            String text = "select * from pattern [MyStream(cnt=" + (i + 1) + ") -> MyStream(cnt=" + (i + 2) + ")]";
            EPStatement stmt = engine.getEPAdministrator().createEPL(text);
            listeners[i] = new SupportUpdateListener();
            stmt.addListener(listeners[i]);
        }

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        ReentrantReadWriteLock sharedStartLock = new ReentrantReadWriteLock();
        sharedStartLock.writeLock().lock();
        for (int i = 0; i < numThreads; i++)
        {
            future[i] = threadPool.submit(new SendEventRWLockCallable(i, sharedStartLock, engine, new GeneratorIterator(numEvents)));
        }
        Thread.sleep(100);
        sharedStartLock.writeLock().unlock();

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue((Boolean) future[i].get());
        }

        // assert result
        for (int i = 0; i < numEvents - 1; i++)
        {
            assertEquals("Listener not invoked: #" + i, 1, listeners[i].getNewDataList().size());
        }
    }
}
