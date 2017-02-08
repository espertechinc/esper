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
package com.espertech.esper.regression.nwtable;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.util.IndexAssertion;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestNamedWindowJoin extends TestCase implements IndexBackingTableInfo
{
    private final static Logger log = LoggerFactory.getLogger(TestNamedWindowJoin.class);

    private EPServiceProvider epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerWindowTwo;
    private SupportUpdateListener listenerStmtOne;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listenerWindow = new SupportUpdateListener();
        listenerWindowTwo = new SupportUpdateListener();
        listenerStmtOne = new SupportUpdateListener();
        SupportQueryPlanIndexHook.reset();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listenerWindow = null;
        listenerStmtOne = null;
        listenerWindowTwo = null;
    }

    public void testWindowUnidirectionalJoin() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);

        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on S1 as s1 delete from MyWindow where s1.p10 = theString");

        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select window(win.*) as c0," +
                "window(win.*).where(v => v.intPrimitive < 2) as c1, " +
                "window(win.*).toMap(k=>k.theString,v=>v.intPrimitive) as c2 " +
                "from S0 as s0 unidirectional, MyWindow as win");
        stmt.addListener(listenerStmtOne);

        SupportBean[] beans = new SupportBean[3];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = new SupportBean("E" + i, i);
        }

        epService.getEPRuntime().sendEvent(beans[0]);
        epService.getEPRuntime().sendEvent(beans[1]);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertReceived(beans, new int[]{0, 1}, new int[]{0, 1}, "E0,E1".split(","), new Object[] {0,1});

        // add bean
        epService.getEPRuntime().sendEvent(beans[2]);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertReceived(beans, new int[]{0, 1, 2}, new int[]{0, 1}, "E0,E1,E2".split(","), new Object[] {0,1, 2});

        // delete bean
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(12));
        assertReceived(beans, new int[]{0, 2}, new int[]{0}, "E0,E2".split(","), new Object[] {0,2});

        // delete another bean
        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "E0"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(14));
        assertReceived(beans, new int[]{2}, new int[0], "E2".split(","), new Object[] {2});

        // delete last bean
        epService.getEPRuntime().sendEvent(new SupportBean_S1(15, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(16));
        assertFalse(listenerStmtOne.getAndClearIsInvoked());

        // compile a non-unidirectional query, join and subquery
        epService.getEPAdministrator().createEPL("select window(win.*) from MyWindow as win");
        epService.getEPAdministrator().createEPL("select window(win.*) as c0 from S0#lastevent as s0, MyWindow as win");
        epService.getEPAdministrator().createEPL("select (select window(win.*) from MyWindow as win) from S0");
    }

    private void assertReceived(SupportBean[] beans, int[] indexesAll, int[] indexesWhere, String[] mapKeys, Object[] mapValues) {
        EventBean received = listenerStmtOne.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder(SupportBean.getBeansPerIndex(beans, indexesAll), (Object[]) received.get("c0"));
        EPAssertionUtil.assertEqualsExactOrder(SupportBean.getBeansPerIndex(beans, indexesWhere), (Collection) received.get("c1"));
        EPAssertionUtil.assertPropsMap((Map) received.get("c2"), mapKeys, mapValues);
    }

    public void testJoinIndexChoice() {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);

        Object[] preloadedEventsOne = new Object[] {new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
        IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "ssb2.s2,ssb1.s1,ssb1.i1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[] {"E1", "E1", 10});
            }
        };

        // no index, since this is "unique(s1)" we don't need one
        String[] noindexes = new String[] {};
        assertIndexChoice(noindexes, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

        // single index one field (duplicate in essence, since "unique(s1)"
        String[] indexOneField = new String[] {"create unique index One on MyWindow (s1)"};
        assertIndexChoice(indexOneField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

        // single index two field (includes "unique(s1)")
        String[] indexTwoField = new String[] {"create unique index One on MyWindow (s1, l1)"};
        assertIndexChoice(indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                        new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

        // two index one unique ("unique(s1)")
        String[] indexSetTwo = new String[] {
                "create index One on MyWindow (s1)",
                "create unique index Two on MyWindow (s1, d1)"};
        assertIndexChoice(indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and d1 = d2 and l1 = l2", true, eventSendAssertion),
                });

        // two index one unique ("win:keepall()")
        assertIndexChoice(indexSetTwo, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[] {
                        new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2", false, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and l1 = l2", false, eventSendAssertion),
                        new IndexAssertion(null, "s1 = s2 and d1 = d2 and l1 = l2", true, eventSendAssertion),
                        new IndexAssertion(null, "d1 = d2 and s1 = s2", true, eventSendAssertion),
                });
    }

    private void assertIndexChoice(String[] indexes, Object[] preloadedEvents, String datawindow,
                                   IndexAssertion ... assertions) {
        epService.getEPAdministrator().createEPL("create window MyWindow." + datawindow + " as SSB1");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SSB1");
        for (String index : indexes) {
            epService.getEPAdministrator().createEPL(index);
        }
        for (Object event : preloadedEvents) {
            epService.getEPRuntime().sendEvent(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) +
                    "select * " +
                    "from SSB2 as ssb2 unidirectional, MyWindow as ssb1 " +
                    "where " + assertion.getWhereClause();

            EPStatement stmt;
            try {
                stmt = epService.getEPAdministrator().createEPL(epl);
                stmt.addListener(listenerStmtOne);
            }
            catch (EPStatementException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // assert index and access
            SupportQueryPlanIndexHook.assertJoinOneStreamAndReset(assertion.getUnique());
            assertion.getEventSendAssertion().run();
            stmt.destroy();
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testInnerJoinLateStart() {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionInnerJoinLateStart(rep);
        }
    }

    private void runAssertionInnerJoinLateStart(EventRepresentationChoice eventRepresentationEnum) {

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema Product (product string, size int)");
        assertTrue(eventRepresentationEnum.matchesClass(stmtOne.getEventType().getUnderlyingType()));
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema Portfolio (portfolio string, product string)");
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window ProductWin#keepall as Product");
        assertTrue(eventRepresentationEnum.matchesClass(stmtTwo.getEventType().getUnderlyingType()));

        epService.getEPAdministrator().createEPL("insert into ProductWin select * from Product");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window PortfolioWin#keepall as Portfolio");
        epService.getEPAdministrator().createEPL("insert into PortfolioWin select * from Portfolio");

        sendProduct(eventRepresentationEnum, "productA", 1);
        sendProduct(eventRepresentationEnum, "productB", 2);
        sendPortfolio(eventRepresentationEnum, "Portfolio", "productA");

        String stmtText = "@Name(\"Query2\") select portfolio, ProductWin.product, size " +
                "from PortfolioWin unidirectional inner join ProductWin on PortfolioWin.product=ProductWin.product";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listenerStmtOne);

        sendPortfolio(eventRepresentationEnum, "Portfolio", "productB");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), new String[]{"portfolio", "ProductWin.product", "size"}, new Object[]{"Portfolio", "productB", 2});

        sendPortfolio(eventRepresentationEnum, "Portfolio", "productC");
        listenerStmtOne.reset();

        sendProduct(eventRepresentationEnum, "productC", 3);
        sendPortfolio(eventRepresentationEnum, "Portfolio", "productC");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), new String[]{"portfolio", "ProductWin.product", "size"}, new Object[]{"Portfolio", "productC", 3});

        epService.initialize();
    }

    private void sendProduct(EventRepresentationChoice eventRepresentationEnum, String product, int size) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {product, size}, "Product");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("product", product);
            theEvent.put("size", size);
            epService.getEPRuntime().sendEvent(theEvent, "Product");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "Product"));
            theEvent.put("product", product);
            theEvent.put("size", size);
            epService.getEPRuntime().sendEventAvro(theEvent, "Product");
        }
        else {
            fail();
        }
    }

    private void sendPortfolio(EventRepresentationChoice eventRepresentationEnum, String portfolio, String product) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {portfolio, product}, "Portfolio");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("portfolio", portfolio);
            theEvent.put("product", product);
            epService.getEPRuntime().sendEvent(theEvent, "Portfolio");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(epService, "Portfolio"));
            theEvent.put("portfolio", portfolio);
            theEvent.put("product", product);
            epService.getEPRuntime().sendEventAvro(theEvent, "Portfolio");
        }
        else {
            fail();
        }
    }

    public void testRightOuterJoinLateStart()
    {
        // Test for ESPER-186 Iterator not honoring order by clause for grouped join query with output-rate clause
        // Test for ESPER-187 Join of two or more named windows on late start may not return correct aggregation state on iterate

        // create window for Leave events
        String stmtTextCreate = "create window WindowLeave#time(6000) as select timeLeave, id, location from " + SupportQueueLeave.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        String stmtTextInsert = "insert into WindowLeave select timeLeave, id, location from " + SupportQueueLeave.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create second window for enter events
        stmtTextCreate = "create window WindowEnter#time(6000) as select location, sku, timeEnter, id from " + SupportQueueEnter.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtTextInsert = "insert into WindowEnter select location, sku, timeEnter, id from " + SupportQueueEnter.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // fill data
        for (int i = 0; i < 8; i++)
        {
            String location = Integer.toString(i / 2);
            epService.getEPRuntime().sendEvent(new SupportQueueLeave(i + 1, location, 247));
        }
        /** Comment in for debug
        System.out.println("Leave events:");
        for (Iterator<EventBean> it = stmtNamedOne.iterator(); it.hasNext();)
        {
            EventBean event = it.next();
            System.out.println(event.get("timeLeave") +
                               " " + event.get("id") +
                    " " + event.get("location"));
        }
         */

        for (int i = 0; i < 10; i++)
        {
            String location = Integer.toString(i / 2);
            String sku = (i % 2 == 0) ? "166583" : "169254";
            epService.getEPRuntime().sendEvent(new SupportQueueEnter(i + 1, location, sku, 123));
        }
        /** Comment in for debug
        System.out.println("Enter events:");
        for (Iterator<EventBean> it = stmtNamedTwo.iterator(); it.hasNext();)
        {
            EventBean event = it.next();
            System.out.println(event.get("timeEnter") +
                               " " + event.get("id") +
                    " " + event.get("sku") +
                    " " + event.get("location"));
        }
         */

        String stmtTextOne = "select s1.location as loc, sku, avg((coalesce(timeLeave, 250) - timeEnter)) as avgTime, " +
                          "count(timeEnter) as cntEnter, count(timeLeave) as cntLeave, (count(timeEnter) - count(timeLeave)) as diff " +
                          "from WindowLeave as s0 right outer join WindowEnter as s1 " +
                          "on s0.id = s1.id and s0.location = s1.location " +
                          "group by s1.location, sku " +
                          "output every 1.0 seconds " +
                          "order by s1.location, sku";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select s1.location as loc, sku, avg((coalesce(timeLeave, 250) - timeEnter)) as avgTime, " +
                          "count(timeEnter) as cntEnter, count(timeLeave) as cntLeave, (count(timeEnter) - count(timeLeave)) as diff " +
                          "from WindowEnter as s1 left outer join WindowLeave as s0 " +
                          "on s0.id = s1.id and s0.location = s1.location " +
                          "group by s1.location, sku " +
                          "output every 1.0 seconds " +
                          "order by s1.location, sku";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);

        /** Comment in for debugging
        System.out.println("Statement 1");
        for (Iterator<EventBean> it = stmtOne.iterator(); it.hasNext();)
        {
            EventBean event = it.next();
            System.out.println("loc " + event.get("loc") +
                               " sku " + event.get("sku") +
                    " avgTime " + event.get("avgTime") +
                    " cntEnter " + event.get("cntEnter") +
                    " cntLeave " + event.get("cntLeave") +
                    " diff " + event.get("diff"));
        }
         */

        Object[][] expected = new Object[][] {
                        {"0", "166583", 124.0, 1L, 1L, 0L},
                        {"0", "169254", 124.0, 1L, 1L, 0L},
                        {"1", "166583", 124.0, 1L, 1L, 0L},
                        {"1", "169254", 124.0, 1L, 1L, 0L},
                        {"2", "166583", 124.0, 1L, 1L, 0L},
                        {"2", "169254", 124.0, 1L, 1L, 0L},
                        {"3", "166583", 124.0, 1L, 1L, 0L},
                        {"3", "169254", 124.0, 1L, 1L, 0L},
                        {"4", "166583", 127.0, 1L, 0L, 1L},
                        {"4", "169254", 127.0, 1L, 0L, 1L}
                    };

        // assert iterator results
        EventBean[] received = EPAssertionUtil.iteratorToArray(stmtTwo.iterator());
        EPAssertionUtil.assertPropsPerRow(received, "loc,sku,avgTime,cntEnter,cntLeave,diff".split(","), expected);
        received = EPAssertionUtil.iteratorToArray(stmtOne.iterator());
        EPAssertionUtil.assertPropsPerRow(received, "loc,sku,avgTime,cntEnter,cntLeave,diff".split(","), expected);
    }

    public void testFullOuterJoinNamedAggregationLateStart()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#groupwin(theString, intPrimitive)#length(3) as select theString, intPrimitive, boolPrimitive from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString, intPrimitive, boolPrimitive from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // fill window
        String[] stringValues = new String[] {"c0", "c1", "c2"};
        for (int i = 0; i < stringValues.length; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                for (int k = 0; k < 2; k++)
                {
                    SupportBean bean = new SupportBean(stringValues[i], j);
                    bean.setBoolPrimitive(true);
                    epService.getEPRuntime().sendEvent(bean);
                }
            }
        }
        SupportBean bean = new SupportBean("c1", 2);
        bean.setBoolPrimitive(true);
        epService.getEPRuntime().sendEvent(bean);

        EventBean[] received = EPAssertionUtil.iteratorToArray(stmtCreate.iterator());
        assertEquals(19, received.length);

        // create select stmt
        String stmtTextSelect = "select theString, intPrimitive, count(boolPrimitive) as cntBool, symbol " +
                                "from MyWindow full outer join " + SupportMarketDataBean.class.getName() + "#keepall " +
                                "on theString = symbol " +
                                "group by theString, intPrimitive, symbol order by theString, intPrimitive, symbol";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);

        // send outer join events
        this.sendMarketBean("c0");
        this.sendMarketBean("c3");

        // get iterator results
        received = EPAssertionUtil.iteratorToArray(stmtSelect.iterator());
        EPAssertionUtil.assertPropsPerRow(received, "theString,intPrimitive,cntBool,symbol".split(","),
                new Object[][]{
                        {null, null, 0L, "c3"},
                        {"c0", 0, 2L, "c0"},
                        {"c0", 1, 2L, "c0"},
                        {"c0", 2, 2L, "c0"},
                        {"c1", 0, 2L, null},
                        {"c1", 1, 2L, null},
                        {"c1", 2, 3L, null},
                        {"c2", 0, 2L, null},
                        {"c2", 1, 2L, null},
                        {"c2", 2, 2L, null},
                });
        /*
        for (int i = 0; i < received.length; i++)
        {
            System.out.println("string=" + received[i].get("string") +
                    " intPrimitive=" + received[i].get("intPrimitive") +
                    " cntBool=" + received[i].get("cntBool") +
                    " symbol=" + received[i].get("symbol"));
        }
        */

        stmtSelect.destroy();
        stmtCreate.destroy();

    }

    public void testJoinNamedAndStream()
    {
        // create window
        String stmtTextCreate = "create window MyWindow#keepall as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyWindow where id = a";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"symbol", "a", "b"};
        String stmtTextSelectOne = "select irstream symbol, a, b " +
                                   " from " + SupportMarketDataBean.class.getName() + "#length(10) as s0," +
                                             "MyWindow as s1 where s1.a = symbol";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);
        EPAssertionUtil.assertEqualsAnyOrder(stmtSelectOne.getEventType().getPropertyNames(), new String[]{"symbol", "a", "b"});
        assertEquals(String.class, stmtSelectOne.getEventType().getPropertyType("symbol"));
        assertEquals(String.class, stmtSelectOne.getEventType().getPropertyType("a"));
        assertEquals(int.class, stmtSelectOne.getEventType().getPropertyType("b"));

        sendMarketBean("S1");
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean("S1", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", "S1", 1});

        sendSupportBean_A("S1"); // deletes from window
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"S1", "S1", 1});

        sendMarketBean("S1");
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean("S2", 2);
        assertFalse(listenerStmtOne.isInvoked());

        sendMarketBean("S2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S2", "S2", 2});

        sendSupportBean("S3", 3);
        sendSupportBean("S3", 4);
        assertFalse(listenerStmtOne.isInvoked());

        sendMarketBean("S3");
        assertEquals(2, listenerStmtOne.getLastNewData().length);
        listenerStmtOne.reset();

        sendSupportBean_A("S3"); // deletes from window
        assertEquals(2, listenerStmtOne.getLastOldData().length);
        listenerStmtOne.reset();

        sendMarketBean("S3");
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testJoinBetweenNamed()
    {
        String[] fields = new String[] {"a1", "b1", "a2", "b2"};

        // create window
        String stmtTextCreateOne = "create window MyWindowOne#keepall as select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName();
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        stmtCreateOne.addListener(listenerWindow);

        // create window
        String stmtTextCreateTwo = "create window MyWindowTwo#keepall as select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName();
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        stmtCreateTwo.addListener(listenerWindowTwo);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + "(volume=1) delete from MyWindowOne where symbol = a1";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + "(volume=0) delete from MyWindowTwo where symbol = a2";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsert = "insert into MyWindowOne select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName() + "(boolPrimitive = true)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        stmtTextInsert = "insert into MyWindowTwo select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName() + "(boolPrimitive = false)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumers
        String stmtTextSelectOne = "select irstream a1, b1, a2, b2 " +
                                   " from MyWindowOne as s0," +
                                         "MyWindowTwo as s1 where s0.a1 = s1.a2";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(true, "S0", 1);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean(false, "S0", 2);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

        sendSupportBean(false, "S1", 3);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean(true, "S1", 4);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 4, "S1", 3});

        sendSupportBean(true, "S1", 5);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 5, "S1", 3});

        sendSupportBean(false, "S1", 6);
        assertEquals(2, listenerStmtOne.getLastNewData().length);
        listenerStmtOne.reset();

        // delete and insert back in
        sendMarketBean("S0", 0);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

        sendSupportBean(false, "S0", 7);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

        // delete and insert back in
        sendMarketBean("S0", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

        sendSupportBean(true, "S0", 8);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 8, "S0", 7});
    }

    public void testJoinBetweenSameNamed()
    {
        String[] fields = new String[] {"a0", "b0", "a1", "b1"};

        // create window
        String stmtTextCreateOne = "create window MyWindow#keepall as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        stmtCreateOne.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " delete from MyWindow where symbol = a";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsert = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumers
        String stmtTextSelectOne = "select irstream s0.a as a0, s0.b as b0, s1.a as a1, s1.b as b1 " +
                                   " from MyWindow as s0," +
                                         "MyWindow as s1 where s0.a = s1.a";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean("E1", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "E1", 1});

        sendSupportBean("E2", 2);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "E2", 2});

        sendMarketBean("E1", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1, "E1", 1});

        sendMarketBean("E0", 0);
        assertFalse(listenerStmtOne.isInvoked());
    }

    public void testJoinSingleInsertOneWindow()
    {
        String[] fields = new String[] {"a1", "b1", "a2", "b2"};

        // create window
        String stmtTextCreateOne = "create window MyWindowOne#keepall as select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName();
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        stmtCreateOne.addListener(listenerWindow);

        // create window
        String stmtTextCreateTwo = "create window MyWindowTwo#keepall as select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName();
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        stmtCreateTwo.addListener(listenerWindowTwo);

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + "(volume=1) delete from MyWindowOne where symbol = a1";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + "(volume=0) delete from MyWindowTwo where symbol = a2";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsert = "insert into MyWindowOne select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName() + "(boolPrimitive = true)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        stmtTextInsert = "insert into MyWindowTwo select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName() + "(boolPrimitive = false)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        // create consumers
        String stmtTextSelectOne = "select irstream a1, b1, a2, b2 " +
                                   " from MyWindowOne as s0," +
                                         "MyWindowTwo as s1 where s0.a1 = s1.a2";
        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(stmtTextSelectOne);
        stmtSelectOne.addListener(listenerStmtOne);

        sendSupportBean(true, "S0", 1);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean(false, "S0", 2);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

        sendSupportBean(false, "S1", 3);
        assertFalse(listenerStmtOne.isInvoked());

        sendSupportBean(true, "S1", 4);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 4, "S1", 3});

        sendSupportBean(true, "S1", 5);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S1", 5, "S1", 3});

        sendSupportBean(false, "S1", 6);
        assertEquals(2, listenerStmtOne.getLastNewData().length);
        listenerStmtOne.reset();

        // delete and insert back in
        sendMarketBean("S0", 0);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

        sendSupportBean(false, "S0", 7);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

        // delete and insert back in
        sendMarketBean("S0", 1);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

        sendSupportBean(true, "S0", 8);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"S0", 8, "S0", 7});
    }

    public void testUnidirectional()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall select * from SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select w.* from MyWindow w unidirectional, SupportBean_A#lastevent s where s.id = w.theString");
        stmtOne.addListener(listenerStmtOne);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertFalse(listenerStmtOne.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_A("E1"));
        assertFalse(listenerStmtOne.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_A("E2"));
        assertFalse(listenerStmtOne.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertTrue(listenerStmtOne.isInvoked());
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(boolean boolPrimitive, String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendMarketBean(String symbol)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(String symbol, long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
