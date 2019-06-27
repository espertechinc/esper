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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.IndexAssertion;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowJoin implements IndexBackingTableInfo {
    private final static Logger log = LoggerFactory.getLogger(InfraNamedWindowJoin.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraJoinIndexChoice());
        execs.add(new InfraRightOuterJoinLateStart());
        execs.add(new InfraFullOuterJoinNamedAggregationLateStart());
        execs.add(new InfraJoinNamedAndStream());
        execs.add(new InfraJoinBetweenNamed());
        execs.add(new InfraJoinBetweenSameNamed());
        execs.add(new InfraJoinSingleInsertOneWindow());
        execs.add(new InfraUnidirectional());
        execs.add(new InfraWindowUnidirectionalJoin());
        execs.add(new InfraInnerJoinLateStart());
        return execs;
    }

    private static class InfraWindowUnidirectionalJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindowWUJ#keepall as SupportBean;\n" +
                "insert into MyWindowWUJ select * from SupportBean;\n" +
                "on SupportBean_S1 as s1 delete from MyWindowWUJ where s1.p10 = theString;\n" +
                "@name('s0') select window(win.*) as c0," +
                "window(win.*).where(v => v.intPrimitive < 2) as c1, " +
                "window(win.*).toMap(k=>k.theString,v=>v.intPrimitive) as c2 " +
                "from SupportBean_S0 as s0 unidirectional, MyWindowWUJ as win";
            env.compileDeploy(epl, path).addListener("s0");

            SupportBean[] beans = new SupportBean[3];
            for (int i = 0; i < beans.length; i++) {
                beans[i] = new SupportBean("E" + i, i);
            }

            env.sendEventBean(beans[0]);
            env.sendEventBean(beans[1]);
            env.sendEventBean(new SupportBean_S0(10));
            assertReceived(env.listener("s0"), beans, new int[]{0, 1}, new int[]{0, 1}, "E0,E1".split(","), new Object[]{0, 1});

            // add bean
            env.sendEventBean(beans[2]);
            env.sendEventBean(new SupportBean_S0(10));
            assertReceived(env.listener("s0"), beans, new int[]{0, 1, 2}, new int[]{0, 1}, "E0,E1,E2".split(","), new Object[]{0, 1, 2});

            // delete bean
            env.sendEventBean(new SupportBean_S1(11, "E1"));
            env.sendEventBean(new SupportBean_S0(12));
            assertReceived(env.listener("s0"), beans, new int[]{0, 2}, new int[]{0}, "E0,E2".split(","), new Object[]{0, 2});

            // delete another bean
            env.sendEventBean(new SupportBean_S1(13, "E0"));
            env.sendEventBean(new SupportBean_S0(14));
            assertReceived(env.listener("s0"), beans, new int[]{2}, new int[0], "E2".split(","), new Object[]{2});

            // delete last bean
            env.sendEventBean(new SupportBean_S1(15, "E2"));
            env.sendEventBean(new SupportBean_S0(16));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            // compile a non-unidirectional query, join and subquery
            env.compileDeploy("select window(win.*) from MyWindowWUJ as win", path);
            env.compileDeploy("select window(win.*) as c0 from SupportBean_S0#lastevent as s0, MyWindowWUJ as win", path);
            env.compileDeploy("select (select window(win.*) from MyWindowWUJ as win) from SupportBean_S0", path);

            env.undeployAll();
        }
    }

    private static class InfraJoinIndexChoice implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
            IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "ssb2.s2,ssb1.s1,ssb1.i1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
                }
            };

            // no index, since this is "unique(s1)" we don't need one
            String[] noindexes = new String[]{};
            assertIndexChoice(env, noindexes, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

            // single index one field (duplicate in essence, since "unique(s1)"
            String[] indexOneField = new String[]{"create unique index One on MyWindow (s1)"};
            assertIndexChoice(env, indexOneField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

            // single index two field (includes "unique(s1)")
            String[] indexTwoField = new String[]{"create unique index One on MyWindow (s1, l1)"};
            assertIndexChoice(env, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                    new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                });

            // two index one unique ("unique(s1)")
            String[] indexSetTwo = new String[]{
                "create index One on MyWindow (s1)",
                "create unique index Two on MyWindow (s1, d1)"};
            assertIndexChoice(env, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2", true, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and l1 = l2", true, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and d1 = d2 and l1 = l2", true, eventSendAssertion),
                });

            // two index one unique ("win:keepall()")
            assertIndexChoice(env, indexSetTwo, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = d2", false, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2", false, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and l1 = l2", false, eventSendAssertion),
                    new IndexAssertion(null, "s1 = s2 and d1 = d2 and l1 = l2", true, eventSendAssertion),
                    new IndexAssertion(null, "d1 = d2 and s1 = s2", true, eventSendAssertion),
                });
        }

        private static void assertIndexChoice(RegressionEnvironment env, String[] indexes, Object[] preloadedEvents, String datawindow,
                                              IndexAssertion... assertions) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow." + datawindow + " as SupportSimpleBeanOne", path);
            env.compileDeploy("insert into MyWindow select * from SupportSimpleBeanOne", path);
            for (String index : indexes) {
                env.compileDeploy(index, path);
            }
            for (Object event : preloadedEvents) {
                env.sendEventBean(event);
            }

            int count = 0;
            for (IndexAssertion assertion : assertions) {
                log.info("======= Testing #" + count);
                count++;

                String epl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) +
                    "select * " +
                    "from SupportSimpleBeanTwo as ssb2 unidirectional, MyWindow as ssb1 " +
                    "where " + assertion.getWhereClause();

                try {
                    env.compileDeploy("@name('s0')" + epl, path).addListener("s0");
                } catch (Throwable ex) {
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
                env.undeployModuleContaining("s0");
            }

            env.undeployAll();
        }
    }

    private static class InfraInnerJoinLateStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionInnerJoinLateStart(env, rep);
            }
        }

        private static void tryAssertionInnerJoinLateStart(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
            String schemaEPL = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedProduct.class) + "@name('schema') create schema Product (product string, size int);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedPortfolio.class) + " create schema Portfolio (portfolio string, product string);\n";
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType(schemaEPL, path);

            env.compileDeploy("@name('window') create window ProductWin#keepall as Product", path);

            assertTrue(eventRepresentationEnum.matchesClass(env.statement("schema").getEventType().getUnderlyingType()));
            assertTrue(eventRepresentationEnum.matchesClass(env.statement("window").getEventType().getUnderlyingType()));

            env.compileDeploy("insert into ProductWin select * from Product", path);
            env.compileDeploy("create window PortfolioWin#keepall as Portfolio", path);
            env.compileDeploy("insert into PortfolioWin select * from Portfolio", path);

            sendProduct(env, eventRepresentationEnum, "productA", 1);
            sendProduct(env, eventRepresentationEnum, "productB", 2);
            sendPortfolio(env, eventRepresentationEnum, "Portfolio", "productA");

            String stmtText = "@Name(\"Query2\") select portfolio, ProductWin.product, size " +
                "from PortfolioWin unidirectional inner join ProductWin on PortfolioWin.product=ProductWin.product";
            env.compileDeploy(stmtText, path).addListener("Query2");

            sendPortfolio(env, eventRepresentationEnum, "Portfolio", "productB");
            EPAssertionUtil.assertProps(env.listener("Query2").assertOneGetNewAndReset(), new String[]{"portfolio", "ProductWin.product", "size"}, new Object[]{"Portfolio", "productB", 2});

            sendPortfolio(env, eventRepresentationEnum, "Portfolio", "productC");
            env.listener("Query2").reset();

            sendProduct(env, eventRepresentationEnum, "productC", 3);
            sendPortfolio(env, eventRepresentationEnum, "Portfolio", "productC");
            EPAssertionUtil.assertProps(env.listener("Query2").assertOneGetNewAndReset(), new String[]{"portfolio", "ProductWin.product", "size"}, new Object[]{"Portfolio", "productC", 3});

            env.undeployAll();
        }

        private static void sendProduct(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String product, int size) {
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{product, size}, "Product");
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
                theEvent.put("product", product);
                theEvent.put("size", size);
                env.sendEventMap(theEvent, "Product");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("Product")));
                theEvent.put("product", product);
                theEvent.put("size", size);
                env.eventService().sendEventAvro(theEvent, "Product");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                JsonObject object = new JsonObject();
                object.add("product", product);
                object.add("size", size);
                env.eventService().sendEventJson(object.toString(), "Product");
            } else {
                fail();
            }
        }

        private static void sendPortfolio(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String portfolio, String product) {
            if (eventRepresentationEnum.isObjectArrayEvent()) {
                env.sendEventObjectArray(new Object[]{portfolio, product}, "Portfolio");
            } else if (eventRepresentationEnum.isMapEvent()) {
                Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
                theEvent.put("portfolio", portfolio);
                theEvent.put("product", product);
                env.sendEventMap(theEvent, "Portfolio");
            } else if (eventRepresentationEnum.isAvroEvent()) {
                GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("Portfolio")));
                theEvent.put("portfolio", portfolio);
                theEvent.put("product", product);
                env.eventService().sendEventAvro(theEvent, "Portfolio");
            } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
                JsonObject object = new JsonObject();
                object.add("portfolio", portfolio);
                object.add("product", product);
                env.eventService().sendEventJson(object.toString(), "Portfolio");
            } else {
                fail();
            }
        }
    }

    private static class InfraRightOuterJoinLateStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test for ESPER-186 Iterator not honoring order by clause for grouped join query with output-rate clause
            // Test for ESPER-187 Join of two or more named windows on late start may not return correct aggregation state on iterate

            RegressionPath path = new RegressionPath();

            // create window for Leave events
            String epl = "create window WindowLeave#time(6000) as select timeLeave, id, location from SupportQueueLeave;\n" +
                "insert into WindowLeave select timeLeave, id, location from SupportQueueLeave;\n";
            env.compileDeploy(epl, path);

            // create second window for enter events
            epl = "create window WindowEnter#time(6000) as select location, sku, timeEnter, id from SupportQueueEnter;\n" +
                "insert into WindowEnter select location, sku, timeEnter, id from SupportQueueEnter;\n";
            env.compileDeploy(epl, path);

            // fill data
            for (int i = 0; i < 8; i++) {
                String location = Integer.toString(i / 2);
                env.sendEventBean(new SupportQueueLeave(i + 1, location, 247));
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

            for (int i = 0; i < 10; i++) {
                String location = Integer.toString(i / 2);
                String sku = (i % 2 == 0) ? "166583" : "169254";
                env.sendEventBean(new SupportQueueEnter(i + 1, location, sku, 123));
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

            String stmtTextOne = "@name('s1') select s1.location as loc, sku, avg((coalesce(timeLeave, 250) - timeEnter)) as avgTime, " +
                "count(timeEnter) as cntEnter, count(timeLeave) as cntLeave, (count(timeEnter) - count(timeLeave)) as diff " +
                "from WindowLeave as s0 right outer join WindowEnter as s1 " +
                "on s0.id = s1.id and s0.location = s1.location " +
                "group by s1.location, sku " +
                "output every 1.0 seconds " +
                "order by s1.location, sku";
            env.compileDeploy(stmtTextOne, path);

            String stmtTextTwo = "@name('s2') select s1.location as loc, sku, avg((coalesce(timeLeave, 250) - timeEnter)) as avgTime, " +
                "count(timeEnter) as cntEnter, count(timeLeave) as cntLeave, (count(timeEnter) - count(timeLeave)) as diff " +
                "from WindowEnter as s1 left outer join WindowLeave as s0 " +
                "on s0.id = s1.id and s0.location = s1.location " +
                "group by s1.location, sku " +
                "output every 1.0 seconds " +
                "order by s1.location, sku";
            env.compileDeploy(stmtTextTwo, path);

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

            Object[][] expected = new Object[][]{
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
            EventBean[] received = EPAssertionUtil.iteratorToArray(env.iterator("s2"));
            EPAssertionUtil.assertPropsPerRow(received, "loc,sku,avgTime,cntEnter,cntLeave,diff".split(","), expected);
            received = EPAssertionUtil.iteratorToArray(env.iterator("s1"));
            EPAssertionUtil.assertPropsPerRow(received, "loc,sku,avgTime,cntEnter,cntLeave,diff".split(","), expected);

            env.undeployAll();
        }
    }

    private static class InfraFullOuterJoinNamedAggregationLateStart implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('create') create window MyWindowFO#groupwin(theString, intPrimitive)#length(3) as select theString, intPrimitive, boolPrimitive from SupportBean;\n" +
                "insert into MyWindowFO select theString, intPrimitive, boolPrimitive from SupportBean;\n";
            env.compileDeploy(epl, path).addListener("create");

            // fill window
            String[] stringValues = new String[]{"c0", "c1", "c2"};
            for (int i = 0; i < stringValues.length; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 2; k++) {
                        SupportBean bean = new SupportBean(stringValues[i], j);
                        bean.setBoolPrimitive(true);
                        env.sendEventBean(bean);
                    }
                }
            }
            SupportBean bean = new SupportBean("c1", 2);
            bean.setBoolPrimitive(true);
            env.sendEventBean(bean);

            EventBean[] received = EPAssertionUtil.iteratorToArray(env.iterator("create"));
            assertEquals(19, received.length);

            // create select stmt
            String stmtTextSelect = "@name('select') select theString, intPrimitive, count(boolPrimitive) as cntBool, symbol " +
                "from MyWindowFO full outer join SupportMarketDataBean#keepall " +
                "on theString = symbol " +
                "group by theString, intPrimitive, symbol order by theString, intPrimitive, symbol";
            env.compileDeploy(stmtTextSelect, path);

            // send outer join events
            sendMarketBean(env, "c0");
            sendMarketBean(env, "c3");

            // get iterator results
            received = EPAssertionUtil.iteratorToArray(env.iterator("select"));
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

            env.undeployModuleContaining("select");
            env.undeployModuleContaining("create");
        }
    }

    private static class InfraJoinNamedAndStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('create') create window MyWindowJNS#keepall as select theString as a, intPrimitive as b from SupportBean;\n" +
                "on SupportBean_A delete from MyWindowJNS where id = a;\n" +
                "insert into MyWindowJNS select theString as a, intPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl, path);

            // create consumer
            String[] fields = new String[]{"symbol", "a", "b"};
            epl = "@name('s0') select irstream symbol, a, b " +
                " from SupportMarketDataBean#length(10) as s0," +
                "MyWindowJNS as s1 where s1.a = symbol";
            env.compileDeploy(epl, path).addListener("s0");

            EPAssertionUtil.assertEqualsAnyOrder(env.statement("s0").getEventType().getPropertyNames(), new String[]{"symbol", "a", "b"});
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("a"));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("b"));

            sendMarketBean(env, "S1");
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "S1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S1", "S1", 1});

            sendSupportBean_A(env, "S1"); // deletes from window
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"S1", "S1", 1});

            sendMarketBean(env, "S1");
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, "S2", 2);
            assertFalse(env.listener("s0").isInvoked());

            sendMarketBean(env, "S2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S2", "S2", 2});

            sendSupportBean(env, "S3", 3);
            sendSupportBean(env, "S3", 4);
            assertFalse(env.listener("s0").isInvoked());

            sendMarketBean(env, "S3");
            assertEquals(2, env.listener("s0").getLastNewData().length);
            env.listener("s0").reset();

            sendSupportBean_A(env, "S3"); // deletes from window
            assertEquals(2, env.listener("s0").getLastOldData().length);
            env.listener("s0").reset();

            sendMarketBean(env, "S3");
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraJoinBetweenNamed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a1", "b1", "a2", "b2"};

            String epl = "@name('createOne') create window MyWindowOne#keepall as select theString as a1, intPrimitive as b1 from SupportBean;\n" +
                "@name('createTwo') create window MyWindowTwo#keepall as select theString as a2, intPrimitive as b2 from SupportBean;\n" +
                "on SupportMarketDataBean(volume=1) delete from MyWindowOne where symbol = a1;\n" +
                "on SupportMarketDataBean(volume=0) delete from MyWindowTwo where symbol = a2;\n" +
                "insert into MyWindowOne select theString as a1, intPrimitive as b1 from SupportBean(boolPrimitive = true);\n" +
                "insert into MyWindowTwo select theString as a2, intPrimitive as b2 from SupportBean(boolPrimitive = false);\n" +
                "@name('s0') select irstream a1, b1, a2, b2 from MyWindowOne as s0, MyWindowTwo as s1 where s0.a1 = s1.a2;\n";
            env.compileDeploy(epl).addListener("s0");

            sendSupportBean(env, true, "S0", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, false, "S0", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

            sendSupportBean(env, false, "S1", 3);
            assertFalse(env.listener("s0").isInvoked());

            sendSupportBean(env, true, "S1", 4);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S1", 4, "S1", 3});

            sendSupportBean(env, true, "S1", 5);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S1", 5, "S1", 3});

            sendSupportBean(env, false, "S1", 6);
            assertEquals(2, env.listener("s0").getLastNewData().length);
            env.listener("s0").reset();

            // delete and insert back in
            sendMarketBean(env, "S0", 0);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

            sendSupportBean(env, false, "S0", 7);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

            // delete and insert back in
            sendMarketBean(env, "S0", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

            sendSupportBean(env, true, "S0", 8);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0", 8, "S0", 7});

            env.undeployAll();
        }
    }

    private static class InfraJoinBetweenSameNamed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a0", "b0", "a1", "b1"};

            String epl = "@name('create') create window MyWindowJSN#keepall as select theString as a, intPrimitive as b from SupportBean;\n" +
                "on SupportMarketDataBean delete from MyWindowJSN where symbol = a;\n" +
                "insert into MyWindowJSN select theString as a, intPrimitive as b from SupportBean;\n" +
                "@name('s0') select irstream s0.a as a0, s0.b as b0, s1.a as a1, s1.b as b1 from MyWindowJSN as s0, MyWindowJSN as s1 where s0.a = s1.a;\n";
            env.compileDeploy(epl).addListener("s0");

            sendSupportBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "E1", 1});

            sendSupportBean(env, "E2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2, "E2", 2});

            sendMarketBean(env, "E1", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetOldAndReset(), fields, new Object[]{"E1", 1, "E1", 1});

            sendMarketBean(env, "E0", 0);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class InfraJoinSingleInsertOneWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"a1", "b1", "a2", "b2"};

            String epl = "@name('create') create window MyWindowJSIOne#keepall as select theString as a1, intPrimitive as b1 from SupportBean;\n" +
                "@name('createTwo') create window MyWindowJSITwo#keepall as select theString as a2, intPrimitive as b2 from SupportBean;\n" +
                "on SupportMarketDataBean(volume=1) delete from MyWindowJSIOne where symbol = a1;\n" +
                "on SupportMarketDataBean(volume=0) delete from MyWindowJSITwo where symbol = a2;\n" +
                "insert into MyWindowJSIOne select theString as a1, intPrimitive as b1 from SupportBean(boolPrimitive = true);\n" +
                "insert into MyWindowJSITwo select theString as a2, intPrimitive as b2 from SupportBean(boolPrimitive = false);\n" +
                "@name('select') select irstream a1, b1, a2, b2 from MyWindowJSIOne as s0, MyWindowJSITwo as s1 where s0.a1 = s1.a2;\n";
            env.compileDeploy(epl).addListener("select");

            sendSupportBean(env, true, "S0", 1);
            assertFalse(env.listener("select").isInvoked());

            sendSupportBean(env, false, "S0", 2);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

            sendSupportBean(env, false, "S1", 3);
            assertFalse(env.listener("select").isInvoked());

            sendSupportBean(env, true, "S1", 4);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S1", 4, "S1", 3});

            sendSupportBean(env, true, "S1", 5);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S1", 5, "S1", 3});

            sendSupportBean(env, false, "S1", 6);
            assertEquals(2, env.listener("select").getLastNewData().length);
            env.listener("select").reset();

            // delete and insert back in
            sendMarketBean(env, "S0", 0);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 2});

            sendSupportBean(env, false, "S0", 7);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

            // delete and insert back in
            sendMarketBean(env, "S0", 1);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetOldAndReset(), fields, new Object[]{"S0", 1, "S0", 7});

            sendSupportBean(env, true, "S0", 8);
            EPAssertionUtil.assertProps(env.listener("select").assertOneGetNewAndReset(), fields, new Object[]{"S0", 8, "S0", 7});

            env.undeployAll();
        }
    }

    private static class InfraUnidirectional implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindowU#keepall select * from SupportBean;\n" +
                "insert into MyWindowU select * from SupportBean;\n" +
                "@name('select') select w.* from MyWindowU w unidirectional, SupportBean_A#lastevent s where s.id = w.theString;\n";
            env.compileDeploy(epl).addListener("select");

            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("select").isInvoked());
            env.sendEventBean(new SupportBean_A("E1"));
            assertFalse(env.listener("select").isInvoked());
            env.sendEventBean(new SupportBean_A("E2"));
            assertFalse(env.listener("select").isInvoked());

            env.sendEventBean(new SupportBean("E2", 1));
            assertTrue(env.listener("select").isInvoked());

            env.undeployAll();
        }
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, boolean boolPrimitive, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setBoolPrimitive(boolPrimitive);
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static void assertReceived(SupportListener listenerStmtOne, SupportBean[] beans, int[] indexesAll, int[] indexesWhere, String[] mapKeys, Object[] mapValues) {
        EventBean received = listenerStmtOne.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder(SupportBean.getBeansPerIndex(beans, indexesAll), (Object[]) received.get("c0"));
        EPAssertionUtil.assertEqualsExactOrder(SupportBean.getBeansPerIndex(beans, indexesWhere), (Collection) received.get("c1"));
        EPAssertionUtil.assertPropsMap((Map) received.get("c2"), mapKeys, mapValues);
    }

    public static class MyLocalJsonProvidedProduct implements Serializable {
        public String product;
        public int size;
    }

    public static class MyLocalJsonProvidedPortfolio implements Serializable {
        public String portfolio;
        public String product;
    }
}
