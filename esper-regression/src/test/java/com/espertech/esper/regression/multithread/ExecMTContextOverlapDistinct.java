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
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecMTContextOverlapDistinct implements RegressionExecution {

    public void run(EPServiceProvider defaultEpService) throws Exception {
        // Test uses system time
        //
        Configuration configuration = new Configuration();
        EPServiceProvider engine = EPServiceProviderManager.getProvider(this.getClass().getSimpleName(), configuration);
        engine.initialize();

        engine.getEPAdministrator().getConfiguration().addEventType(TestEvent.class);
        engine.getEPAdministrator().createEPL("create context theContext " +
                " initiated by distinct(partitionKey) TestEvent as test " +
                " terminated after 100 milliseconds");

        EPStatement stmt = engine.getEPAdministrator().createEPL("context theContext " +
                "select sum(value) as thesum, count(*) as thecnt " +
                "from TestEvent output snapshot when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        final int numLoops = 2000000;
        final int numEvents = numLoops * 4;
        for (int i = 0; i < numLoops; i++) {
            if (i % 100000 == 0) {
                System.out.println("Completed: " + i);
            }
            engine.getEPRuntime().sendEvent(new TestEvent("TEST", 10));
            engine.getEPRuntime().sendEvent(new TestEvent("TEST", -10));
            engine.getEPRuntime().sendEvent(new TestEvent("TEST", 25));
            engine.getEPRuntime().sendEvent(new TestEvent("TEST", -25));
        }

        int numDeliveries = listener.getNewDataList().size();
        System.out.println("Done " + numLoops + " loops, have " + numDeliveries + " deliveries");
        assertTrue(numDeliveries > 3);

        Thread.sleep(1000);

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

        engine.destroy();
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
