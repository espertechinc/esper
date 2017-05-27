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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtNamedWindowUpdateCallable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety and named window updates.
 */
public class ExecMTStmtNamedWindowUpdate implements RegressionExecution {
    public final static int NUM_STRINGS = 100;
    public final static int NUM_INTS = 10;

    public void run(EPServiceProvider epService) throws Exception {
        trySend(epService, 5, 10000);
    }

    private void trySend(EPServiceProvider epService, int numThreads, int numEventsPerThread) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // setup statements
        epService.getEPAdministrator().createEPL("create window MyWindow#unique(theString, intPrimitive) as select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean(boolPrimitive = true)");
        epService.getEPAdministrator().createEPL("on SupportBean(boolPrimitive = false) sb " +
                "update MyWindow win set intBoxed = win.intBoxed + 1, doublePrimitive = win.doublePrimitive + sb.doublePrimitive" +
                " where sb.theString = win.theString and sb.intPrimitive = win.intPrimitive");

        // send primer events, initialize totals
        Map<MultiKeyUntyped, UpdateTotals> totals = new HashMap<MultiKeyUntyped, UpdateTotals>();
        for (int i = 0; i < NUM_STRINGS; i++) {
            for (int j = 0; j < NUM_INTS; j++) {
                SupportBean primer = new SupportBean(Integer.toString(i), j);
                primer.setBoolPrimitive(true);
                primer.setIntBoxed(0);
                primer.setDoublePrimitive(0);

                epService.getEPRuntime().sendEvent(primer);
                MultiKeyUntyped key = new MultiKeyUntyped(primer.getTheString(), primer.getIntPrimitive());
                totals.put(key, new UpdateTotals(0, 0));
            }
        }

        // execute
        long startTime = System.currentTimeMillis();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future<StmtNamedWindowUpdateCallable.UpdateResult>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowUpdateCallable("Thread" + i, epService, numEventsPerThread));
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // total up result
        long deltaCumulative = 0;
        for (int i = 0; i < numThreads; i++) {
            StmtNamedWindowUpdateCallable.UpdateResult result = future[i].get();
            deltaCumulative += result.getDelta();
            for (StmtNamedWindowUpdateCallable.UpdateItem item : result.getUpdates()) {
                MultiKeyUntyped key = new MultiKeyUntyped(item.getTheString(), item.getIntval());
                UpdateTotals total = totals.get(key);
                if (total == null) {
                    throw new RuntimeException("Totals not found for key " + key);
                }
                total.setNum(total.getNum() + 1);
                total.setSum(total.getSum() + item.getDoublePrimitive());
            }
        }

        // compare
        EventBean[] rows = epService.getEPRuntime().executeQuery("select * from MyWindow").getArray();
        assertEquals(rows.length, totals.size());
        long totalUpdates = 0;
        for (EventBean row : rows) {
            UpdateTotals total = totals.get(new MultiKeyUntyped(row.get("theString"), row.get("intPrimitive")));
            assertEquals(total.getNum(), row.get("intBoxed"));
            assertEquals(total.getSum(), row.get("doublePrimitive"));
            totalUpdates += total.getNum();
        }

        assertEquals(totalUpdates, numThreads * numEventsPerThread);
        //long deltaTime = endTime - startTime;
        //System.out.println("Totals updated: " + totalUpdates + "  Delta cumu: " + deltaCumulative + "  Delta pooled: " + deltaTime);
    }

    private static class UpdateTotals {
        private int num;
        private double sum;

        private UpdateTotals(int num, double sum) {
            this.num = num;
            this.sum = sum;
        }

        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public double getSum() {
            return sum;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }
    }
}
