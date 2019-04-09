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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.StmtNamedWindowUpdateCallable;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import org.junit.Assert;

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
public class MultithreadStmtNamedWindowUpdate implements RegressionExecution {
    public final static int NUM_STRINGS = 100;
    public final static int NUM_INTS = 10;

    public void run(RegressionEnvironment env) {
        trySend(env, 5, 10000);
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    private static void trySend(RegressionEnvironment env, int numThreads, int numEventsPerThread) {

        // setup statements
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window MyWindow#unique(theString, intPrimitive) as select * from SupportBean", path);
        env.compileDeploy("insert into MyWindow select * from SupportBean(boolPrimitive = true)", path);
        env.compileDeploy("on SupportBean(boolPrimitive = false) sb " +
            "update MyWindow win set intBoxed = win.intBoxed + 1, doublePrimitive = win.doublePrimitive + sb.doublePrimitive" +
            " where sb.theString = win.theString and sb.intPrimitive = win.intPrimitive", path);

        // send primer events, initialize totals
        Map<Pair<String, Integer>, UpdateTotals> totals = new HashMap<>();
        for (int i = 0; i < NUM_STRINGS; i++) {
            for (int j = 0; j < NUM_INTS; j++) {
                SupportBean primer = new SupportBean(Integer.toString(i), j);
                primer.setBoolPrimitive(true);
                primer.setIntBoxed(0);
                primer.setDoublePrimitive(0);

                env.sendEventBean(primer);
                Pair<String, Integer> key = new Pair<>(primer.getTheString(), primer.getIntPrimitive());
                totals.put(key, new UpdateTotals(0, 0));
            }
        }

        // execute
        long startTime = System.currentTimeMillis();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads, new SupportThreadFactory(MultithreadStmtNamedWindowUpdate.class));
        Future<StmtNamedWindowUpdateCallable.UpdateResult>[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            future[i] = threadPool.submit(new StmtNamedWindowUpdateCallable("Thread" + i, env.runtime(), numEventsPerThread));
        }

        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // total up result
        long deltaCumulative = 0;
        for (int i = 0; i < numThreads; i++) {
            StmtNamedWindowUpdateCallable.UpdateResult result = null;
            try {
                result = future[i].get();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            deltaCumulative += result.getDelta();
            for (StmtNamedWindowUpdateCallable.UpdateItem item : result.getUpdates()) {
                Pair<String, Integer> key = new Pair<>(item.getTheString(), item.getIntval());
                UpdateTotals total = totals.get(key);
                if (total == null) {
                    throw new RuntimeException("Totals not found for key " + key);
                }
                total.setNum(total.getNum() + 1);
                total.setSum(total.getSum() + item.getDoublePrimitive());
            }
        }

        // compare
        EventBean[] rows = env.compileExecuteFAF("select * from MyWindow", path).getArray();
        assertEquals(rows.length, totals.size());
        long totalUpdates = 0;
        for (EventBean row : rows) {
            UpdateTotals total = totals.get(new Pair<>((String) row.get("theString"), (Integer) row.get("intPrimitive")));
            Assert.assertEquals(total.getNum(), row.get("intBoxed"));
            Assert.assertEquals(total.getSum(), row.get("doublePrimitive"));
            totalUpdates += total.getNum();
        }

        assertEquals(totalUpdates, numThreads * numEventsPerThread);
        //long deltaTime = endTime - startTime;
        //System.out.println("Totals updated: " + totalUpdates + "  Delta cumu: " + deltaCumulative + "  Delta pooled: " + deltaTime);

        env.undeployAll();
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
