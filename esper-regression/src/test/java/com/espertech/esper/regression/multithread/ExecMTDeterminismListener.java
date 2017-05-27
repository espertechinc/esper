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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.GeneratorIterator;
import com.espertech.esper.supportregression.multithread.SendEventCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety and deterministic behavior when using insert-into.
 */
public class ExecMTDeterminismListener implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        trySend(4, 10000, true, ConfigurationEngineDefaults.Threading.Locking.SUSPEND);
        trySend(4, 10000, true, ConfigurationEngineDefaults.Threading.Locking.SPIN);
    }

    public void manualTestOrderedDeliveryFail() throws Exception {
        /**
         * Commented out as this is a manual test -- it should fail since the disable preserve order. 
         */
        trySend(3, 1000, false, null);
    }

    private void trySend(int numThreads, int numEvents, boolean isPreserveOrder, ConfigurationEngineDefaults.Threading.Locking locking) throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(isPreserveOrder);
        config.getEngineDefaults().getThreading().setListenerDispatchLocking(locking);

        EPServiceProvider engine = EPServiceProviderManager.getProvider(this.getClass().getSimpleName(), config);
        engine.initialize();

        // setup statements
        EPStatement stmtInsert = engine.getEPAdministrator().createEPL("select count(*) as cnt from " + SupportBean.class.getName());
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        stmtInsert.addListener(listener);

        // execute
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventCallable(i, engine, new GeneratorIterator(numEvents)));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        EventBean[] events = listener.getNewDataListFlattened();
        long[] result = new long[events.length];
        for (int i = 0; i < events.length; i++) {
            result[i] = (Long) events[i].get("cnt");
        }
        //log.info(".trySend result=" + Arrays.toString(result));

        // assert result
        assertEquals(numEvents * numThreads, events.length);
        for (int i = 0; i < numEvents * numThreads; i++) {
            assertEquals(result[i], (long) i + 1);
        }

        engine.destroy();
    }
}
