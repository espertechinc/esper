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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtUpdateSendCallable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety (or lack thereof) for iterators: iterators fail with concurrent mods as expected behavior
 */
public class ExecMTUpdate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString from " + SupportBean.class.getName());

        final List<String> strings = Collections.synchronizedList(new ArrayList<String>());
        stmt.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                strings.add((String) newEvents[0].get("theString"));
            }
        });

        trySend(epService, 2, 50000);

        boolean found = false;
        for (String value : strings) {
            if (value.equals("a")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtUpdateSendCallable(i, epService, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        for (int i = 0; i < 50; i++) {
            EPStatement stmtUpd = epService.getEPAdministrator().createEPL("update istream " + SupportBean.class.getName() + " set theString='a'");
            Thread.sleep(10);
            stmtUpd.destroy();
        }

        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue((Boolean) future[i].get());
        }
    }
}
