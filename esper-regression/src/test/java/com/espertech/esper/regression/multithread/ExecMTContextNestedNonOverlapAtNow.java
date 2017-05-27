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

public class ExecMTContextNestedNonOverlapAtNow implements RegressionExecution {

    public void run(EPServiceProvider defaultEpService) throws Exception {
        Configuration configuration = new Configuration();
        EPServiceProvider epService = EPServiceProviderManager.getProvider(this.getClass().getSimpleName(), configuration);
        epService.initialize();
        Thread.sleep(100); // allow time for start up

        epService.getEPAdministrator().getConfiguration().addEventType(TestEvent.class);
        epService.getEPAdministrator().createEPL("create context theContext " +
                "context perPartition partition by partitionKey from TestEvent," +
                "context per10Seconds start @now end after 100 milliseconds");

        EPStatement stmt = epService.getEPAdministrator().createEPL("context theContext " +
                "select sum(value) as thesum, count(*) as thecnt, context.perPartition.key1 as thekey " +
                "from TestEvent output snapshot when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        final int numLoops = 200000;
        final int numEvents = numLoops * 4;
        for (int i = 0; i < numLoops; i++) {
            if (i % 100000 == 0) {
                System.out.println("Completed: " + i);
            }
            epService.getEPRuntime().sendEvent(new TestEvent("TEST", 10));
            epService.getEPRuntime().sendEvent(new TestEvent("TEST", -10));
            epService.getEPRuntime().sendEvent(new TestEvent("TEST", 25));
            epService.getEPRuntime().sendEvent(new TestEvent("TEST", -25));
        }

        Thread.sleep(250);

        int numDeliveries = listener.getNewDataList().size();
        assertTrue("Done " + numLoops + " loops, have " + numDeliveries + " deliveries", numDeliveries >= 2);

        int sum = 0;
        long count = 0;
        for (EventBean event : listener.getNewDataListFlattened()) {
            Integer sumBatch = (Integer) event.get("thesum");
            if (sumBatch != null) { // can be null when there is nothing to deliver
                sum += sumBatch;
                count += (Long) event.get("thecnt");
            }
        }
        assertEquals(0, sum);
        assertEquals(numEvents, count);
        epService.destroy();
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
