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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultithreadContextOverlapDistinct {

    public void run(Configuration configuration) {
        // Test uses system time
        //
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCommon().addEventType(TestEvent.class);
        EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getSimpleName(), configuration);
        runtime.initialize();

        RegressionPath path = new RegressionPath();
        String eplCtx = "@name('ctx') create context theContext " +
            " initiated by distinct(partitionKey) TestEvent as test " +
            " terminated after 100 milliseconds";
        EPCompiled compiledContext = compile(eplCtx, configuration, path);
        path.add(compiledContext);
        deploy(compiledContext, runtime);

        String eplStmt = "context theContext " +
            "select sum(value) as thesum, count(*) as thecnt " +
            "from TestEvent output snapshot when terminated";
        EPCompiled compiledStmt = compile(eplStmt, configuration, path);
        SupportUpdateListener listener = new SupportUpdateListener();
        deployAddListener(compiledStmt, "s0", listener, runtime);

        final int numLoops = 2000000;
        final int numEvents = numLoops * 4;
        for (int i = 0; i < numLoops; i++) {
            if (i % 100000 == 0) {
                System.out.println("Completed: " + i);
            }
            runtime.getEventService().sendEventBean(new TestEvent("TEST", 10), "TestEvent");
            runtime.getEventService().sendEventBean(new TestEvent("TEST", -10), "TestEvent");
            runtime.getEventService().sendEventBean(new TestEvent("TEST", 25), "TestEvent");
            runtime.getEventService().sendEventBean(new TestEvent("TEST", -25), "TestEvent");
        }

        int numDeliveries = listener.getNewDataList().size();
        System.out.println("Done " + numLoops + " loops, have " + numDeliveries + " deliveries");
        assertTrue(numDeliveries > 3);

        SupportCompileDeployUtil.threadSleep(1000);

        int sum = 0;
        long count = 0;
        for (EventBean event : listener.getNewDataListFlattened()) {
            Integer sumBatch = (Integer) event.get("thesum");
            // Comment-Me-In: System.out.println(EventBeanUtility.summarize(event));
            if (sumBatch != null) { // can be null when there is nothing to deliver
                sum += sumBatch;
                count += (Long) event.get("thecnt");
            }
        }
        System.out.println("count=" + count + "  sum=" + sum);
        assertEquals(numEvents, count);
        assertEquals(0, sum);

        runtime.destroy();
    }

    public static class TestEvent {
        private final String partitionKey;
        private final int value;

        public TestEvent(String partitionKey, int value) {
            this.partitionKey = partitionKey;
            this.value = value;
        }

        public String getPartitionKey() {
            return partitionKey;
        }

        public int getValue() {
            return value;
        }
    }
}
