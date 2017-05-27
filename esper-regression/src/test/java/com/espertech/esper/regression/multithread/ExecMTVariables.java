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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.VariableReadWriteCallable;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;

import java.util.TreeSet;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety of setting and reading variables.
 * <p>
 * Assume we have 2 statements that set 3 variables, and one statement that selects variables:
 * <p>
 * <pre>on A as a set var1 = a.value, var2 = a.value, var3 = var3 + 1<pre>
 * <pre>on B as a set var1 = b.value, var2 = b.value, var3 = var3 + 1<pre>
 * <pre>select var1, var2 from C(id=threadid)<pre> (one per thread)
 *
 * Result: If 4 threads send A and B events and assign a random value, then var1 and var2 should always be the same value
 * both when selected in the select statement.
 * In addition, the counter var3 should not miss a single value when posted to listeners of the set-statements.
 *
 * Each thread sends for each loop one A, B and C event, and returns the result for all "var3" values for checking when done.
 */
public class ExecMTVariables implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Long.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Long.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("var3", Long.class, 0);

        SupportMTUpdateListener listenerSetOne = new SupportMTUpdateListener();
        SupportMTUpdateListener listenerSetTwo = new SupportMTUpdateListener();

        String stmtSetOneText = "on " + SupportBean.class.getName() + " set var1=longPrimitive, var2=longPrimitive, var3=var3+1";
        String stmtSetTwoText = "on " + SupportMarketDataBean.class.getName() + " set var1=volume, var2=volume, var3=var3+1";
        epService.getEPAdministrator().createEPL(stmtSetOneText).addListener(listenerSetOne);
        epService.getEPAdministrator().createEPL(stmtSetTwoText).addListener(listenerSetTwo);

        trySetAndReadAtomic(epService, listenerSetOne, listenerSetTwo, 2, 10000);
    }

    private void trySetAndReadAtomic(EPServiceProvider epService, SupportMTUpdateListener listenerSetOne, SupportMTUpdateListener listenerSetTwo, int numThreads, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new VariableReadWriteCallable(i, epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }

        // Determine if we have all numbers for var3 and didn't skip one.
        // Since "var3 = var3 + 1" is executed by multiple statements and threads we need to have
        // this counter have all the values from 0 to N-1.
        TreeSet<Long> var3Values = new TreeSet<Long>();
        for (EventBean theEvent : listenerSetOne.getNewDataListFlattened()) {
            var3Values.add((Long) theEvent.get("var3"));
        }
        for (EventBean theEvent : listenerSetTwo.getNewDataListFlattened()) {
            var3Values.add((Long) theEvent.get("var3"));
        }
        assertEquals(numThreads * numRepeats, var3Values.size());
        for (int i = 1; i < numThreads * numRepeats + 1; i++) {
            assertTrue(var3Values.contains((long) i));
        }
    }
}
