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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.SendEventCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of context.
 */
public class ExecMTContextCountSimple implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create context HashByUserCtx as coalesce by consistent_hash_crc32(theString) from SupportBean granularity 10000000");
        epService.getEPAdministrator().createEPL("@Name('select') context HashByUserCtx select theString from SupportBean");

        trySendContextCountSimple(epService, 4, 5);
    }

    private void trySendContextCountSimple(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        epService.getEPAdministrator().getStatement("select").addListener(listener);

        List<Object> events = new ArrayList<Object>();
        for (int i = 0; i < numRepeats; i++) {
            events.add(new SupportBean("E" + i, i));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new SendEventCallable(i, epService, events.iterator());
            future[i] = threadPool.submit(callable);
        }

        Thread.sleep(2000);
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        EventBean[] result = listener.getNewDataListFlattened();
        assertEquals(numRepeats * numThreads, result.length);
    }
}
