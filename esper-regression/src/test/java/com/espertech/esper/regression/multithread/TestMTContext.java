/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Test for multithread-safety of context.
 */
public class TestMTContext extends TestCase
{
    private EPServiceProvider engine;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(EventUnderlyingType.MAP); // use Map-type events for testing
        engine = EPServiceProviderManager.getDefaultProvider(configuration);
        engine.initialize();
    }

    protected void tearDown() throws Exception {
    }

    public void testContextCountSimple() throws Exception
    {
        engine.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        engine.getEPAdministrator().createEPL("create context HashByUserCtx as coalesce by consistent_hash_crc32(theString) from SupportBean granularity 10000000");
        engine.getEPAdministrator().createEPL("@Name('select') context HashByUserCtx select theString from SupportBean");

        trySendContextCountSimple(4, 5);
    }

    public void testContextUnique() throws Exception {
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

        engine.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        MyUpdateListener listener = new MyUpdateListener();
        engine.getEPAdministrator().getStatement("Select").addListener(listener);

        List<Map> sendsT1 = new ArrayList<Map>();
        sendsT1.add(makeEvent("A", "house", "P0", 1));
        sendsT1.add(makeEvent("B", "house", "P0", 2));
        List<Map> sendsT2 = new ArrayList<Map>();
        sendsT2.add(makeEvent("B", "house", "P0", 3));
        sendsT1.add(makeEvent("A", "house", "P0", 4));

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(new SendEventRunnable(engine, sendsT1, "ScoreCycle"));
        threadPool.submit(new SendEventRunnable(engine, sendsT2, "ScoreCycle"));
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.SECONDS);

        // compare
        List<Object> received = listener.getReceived();
        for (Object item : received) {
            System.out.println(item);
        }
        assertEquals(4, received.size());
    }

    private void trySendContextCountSimple(int numThreads, int numRepeats) throws Exception
    {
        SupportMTUpdateListener listener = new SupportMTUpdateListener();
        engine.getEPAdministrator().getStatement("select").addListener(listener);

        List<Object> events = new ArrayList<Object>();
        for (int i = 0; i < numRepeats; i++) {
            events.add(new SupportBean("E" + i, i));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new SendEventCallable(i, engine, events.iterator());
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        EventBean[] result = listener.getNewDataListFlattened();
        assertEquals(numRepeats * numThreads, result.length);
    }

    private Map<String, Object> makeEvent(String userId, String keyword, String productId, long score) {
        Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
        theEvent.put("userId", userId);
        theEvent.put("keyword", keyword);
        theEvent.put("productId", productId);
        theEvent.put("score", score);
        return theEvent;
    }

    public static class MyUpdateListener implements UpdateListener
    {
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

    public static class SendEventRunnable implements Runnable
    {
        private final EPServiceProvider engine;
        private final List<Map> events;
        private final String type;

        public SendEventRunnable(EPServiceProvider engine, List<Map> events, String type) {
            this.engine = engine;
            this.events = events;
            this.type = type;
        }

        public void run()
        {
            for (Map theEvent : events) {
                engine.getEPRuntime().sendEvent(theEvent, type);
            }
        }
    }
}
