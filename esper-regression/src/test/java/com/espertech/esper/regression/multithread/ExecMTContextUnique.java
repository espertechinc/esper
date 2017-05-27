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
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test for multithread-safety of context.
 */
public class ExecMTContextUnique implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
    }

    public void run(EPServiceProvider epService) throws Exception {
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

        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        MyUpdateListener listener = new MyUpdateListener();
        epService.getEPAdministrator().getStatement("Select").addListener(listener);

        List<Map> sendsT1 = new ArrayList<Map>();
        sendsT1.add(makeEvent("A", "house", "P0", 1));
        sendsT1.add(makeEvent("B", "house", "P0", 2));
        List<Map> sendsT2 = new ArrayList<Map>();
        sendsT2.add(makeEvent("B", "house", "P0", 3));
        sendsT1.add(makeEvent("A", "house", "P0", 4));

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(new SendEventRunnable(epService, sendsT1, "ScoreCycle"));
        threadPool.submit(new SendEventRunnable(epService, sendsT2, "ScoreCycle"));
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.SECONDS);

        // compare
        List<Object> received = listener.getReceived();
        for (Object item : received) {
            System.out.println(item);
        }
        assertEquals(4, received.size());
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

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents) {
            for (int i = 0; i < newEvents.length; i++) {
                received.add(newEvents[i].getUnderlying());
            }
        }

        public List<Object> getReceived() {
            return received;
        }
    }

    public static class SendEventRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final List<Map> events;
        private final String type;

        public SendEventRunnable(EPServiceProvider engine, List<Map> events, String type) {
            this.engine = engine;
            this.events = events;
            this.type = type;
        }

        public void run() {
            for (Map theEvent : events) {
                engine.getEPRuntime().sendEvent(theEvent, type);
            }
        }
    }
}
