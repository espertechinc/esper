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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.util.SupportThreadFactory;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for multithread-safety of context.
 */
public class MultithreadContextUnique implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String epl = "create schema ScoreCycle (userId string, keyword string, productId string, score long);\n" +
            "\n" +
            "create schema UserKeywordTotalStream (userId string, keyword string, sumScore long);\n" +
            "\n" +
            "create context HashByUserCtx as\n" +
            "coalesce by consistent_hash_crc32(userId) from ScoreCycle,\n" +
            "consistent_hash_crc32(userId) from UserKeywordTotalStream \n" +
            "granularity 10000000;\n" +
            "\n" +
            "context HashByUserCtx create window ScoreCycleWindow#unique(userId, keyword, productId) as ScoreCycle;\n" +
            "\n" +
            "context HashByUserCtx insert into ScoreCycleWindow select * from ScoreCycle;\n" +
            "\n" +
            "@Name('Select') context HashByUserCtx insert into UserKeywordTotalStream\n" +
            "select userId, keyword, sum(score) as sumScore from ScoreCycleWindow group by userId, keyword;";
        env.compileDeployWBusPublicType(epl, new RegressionPath());
        MyUpdateListener listener = new MyUpdateListener();
        env.statement("Select").addListener(listener);

        List<Map> sendsT1 = new ArrayList<Map>();
        sendsT1.add(makeEvent("A", "house", "P0", 1));
        sendsT1.add(makeEvent("B", "house", "P0", 2));
        List<Map> sendsT2 = new ArrayList<Map>();
        sendsT2.add(makeEvent("B", "house", "P0", 3));
        sendsT1.add(makeEvent("A", "house", "P0", 4));

        ExecutorService threadPool = Executors.newFixedThreadPool(2, new SupportThreadFactory(MultithreadContextUnique.class));
        SendEventRunnable runnableOne = new SendEventRunnable(env.runtime(), sendsT1, "ScoreCycle");
        SendEventRunnable runnableTwo = new SendEventRunnable(env.runtime(), sendsT2, "ScoreCycle");
        threadPool.submit(runnableOne);
        threadPool.submit(runnableTwo);
        threadPool.shutdown();
        SupportCompileDeployUtil.threadpoolAwait(threadPool, 1, TimeUnit.SECONDS);

        assertNull(runnableOne.lastException);
        assertNull(runnableTwo.lastException);

        // compare
        List<Object> received = listener.getReceived();
        for (Object item : received) {
            System.out.println(item);
        }
        assertEquals(4, received.size());

        env.undeployAll();
    }

    private Map<String, Object> makeEvent(String userId, String keyword, String productId, long score) {
        Map<String, Object> theEvent = new LinkedHashMap<>();
        theEvent.put("userId", userId);
        theEvent.put("keyword", keyword);
        theEvent.put("productId", productId);
        theEvent.put("score", score);
        return theEvent;
    }

    public static class MyUpdateListener implements UpdateListener {
        private List<Object> received = new ArrayList<Object>();

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            for (int i = 0; i < newEvents.length; i++) {
                received.add(newEvents[i].getUnderlying());
            }
        }

        public List<Object> getReceived() {
            return received;
        }
    }

    public static class SendEventRunnable implements Runnable {
        private final EPRuntime runtime;
        private final List<Map> events;
        private final String type;
        private Throwable lastException;

        public SendEventRunnable(EPRuntime runtime, List<Map> events, String type) {
            this.runtime = runtime;
            this.events = events;
            this.type = type;
        }

        public void run() {
            try {
                for (Map theEvent : events) {
                    runtime.getEventService().sendEventMap(theEvent, type);
                }
            } catch (Throwable t) {
                lastException = t;
                t.printStackTrace();
            }
        }

        public Throwable getLastException() {
            return lastException;
        }
    }
}
