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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.GeneratorIterator;
import com.espertech.esper.supportregression.multithread.GeneratorIteratorCallback;
import com.espertech.esper.supportregression.multithread.MTListener;
import com.espertech.esper.supportregression.multithread.SendEventCallable;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for a simple aggregation case using count(*).
 */
public class ExecMTStmtFilter implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String plainFilter = "select count(*) as mycount from " + SupportBean.class.getName();
        tryCount(epService, 2, 1000, plainFilter, GeneratorIterator.DEFAULT_SUPPORTEBEAN_CB);
        tryCount(epService, 4, 1000, plainFilter, GeneratorIterator.DEFAULT_SUPPORTEBEAN_CB);

        GeneratorIteratorCallback enumCallback = new GeneratorIteratorCallback() {
            private final Collection<String> vals = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

            public Object getObject(int numEvent) {
                SupportCollection bean = new SupportCollection();
                bean.setStrvals(vals);
                return bean;
            }
        };
        String enumFilter = "select count(*) as mycount from " + SupportCollection.class.getName() + "(strvals.anyOf(v => v = 'j'))";
        tryCount(epService, 4, 1000, enumFilter, enumCallback);
    }

    public void tryCount(EPServiceProvider epService, int numThreads, int numMessages, String epl, GeneratorIteratorCallback generatorIteratorCallback) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        MTListener listener = new MTListener("mycount");
        stmt.addListener(listener);

        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new SendEventCallable(i, epService, new GeneratorIterator(numMessages, generatorIteratorCallback)));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // verify results
        assertEquals(numMessages * numThreads, listener.getValues().size());
        TreeSet<Integer> result = new TreeSet<Integer>();
        for (Object row : listener.getValues()) {
            result.add(((Number) row).intValue());
        }
        assertEquals(numMessages * numThreads, result.size());
        assertEquals(1, (Object) result.first());
        assertEquals(numMessages * numThreads, (Object) result.last());
    }
}
