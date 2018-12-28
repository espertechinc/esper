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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ResultSetAggregationMethodSorted {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateSortedNonTable());
        execs.add(new ResultSetAggregateSortedTableAccess());
        execs.add(new ResultSetAggregateSortedTableIdent());
        execs.add(new ResultSetAggregateSortedCFHL());
        execs.add(new ResultSetAggregateSortedCFHLEnumerationAndDot());
        execs.add(new ResultSetAggregateSortedFirstLast());
        execs.add(new ResultSetAggregateSortedFirstLastEnumerationAndDot());
        execs.add(new ResultSetAggregateSortedGetContainsCounts());
        execs.add(new ResultSetAggregateSortedSubmapEventsBetween());
        execs.add(new ResultSetAggregateSortedNavigableMapReference());
        execs.add(new ResultSetAggregateSortedMultiCriteria());
        execs.add(new ResultSetAggregateSortedGrouped());
        execs.add(new ResultSetAggregateSortedInvalid());
        execs.add(new ResultSetAggregateSortedDocSample());
        return execs;
    }

    private static class ResultSetAggregateSortedDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@buseventtype @public create schema OrderEvent(orderId string, price double);\n" +
                "@name('a') select sorted(price).lowerKey(price) as lowerPrice from OrderEvent#time(10 minutes);\n" +
                "@name('b') select sorted(price).lowerEvent(price).orderId as lowerPriceOrderId from OrderEvent#time(10 minutes);\n" +
                "create table OrderPrices(prices sorted(price) @type('OrderEvent'));\n" +
                "into table OrderPrices select sorted(*) as prices from OrderEvent#time(10 minutes);\n" +
                "@name('c') select OrderPrices.prices.firstKey() as lowestPrice, OrderPrices.prices.lastKey() as highestPrice from OrderEvent;\n" +
                "@name('d') select (select prices.firstKey() from OrderPrices) as lowestPrice, * from OrderEvent;\n";
            env.compileDeploy(epl).addListener("a").addListener("b").addListener("c").addListener("d");

            env.sendEventMap(CollectionUtil.buildMap("orderId", "A", "price", 10d), "OrderEvent");
            EPAssertionUtil.assertProps(env.listener("a").assertOneGetNewAndReset(), "lowerPrice".split(","), new Object[] {null});
            EPAssertionUtil.assertProps(env.listener("b").assertOneGetNewAndReset(), "lowerPriceOrderId".split(","), new Object[] {null});
            EPAssertionUtil.assertProps(env.listener("c").assertOneGetNewAndReset(), "lowestPrice,highestPrice".split(","), new Object[] {10d, 10d});
            EPAssertionUtil.assertProps(env.listener("d").assertOneGetNewAndReset(), "lowestPrice".split(","), new Object[] {10d});

            env.milestone(0);

            env.sendEventMap(CollectionUtil.buildMap("orderId", "B", "price", 20d), "OrderEvent");
            EPAssertionUtil.assertProps(env.listener("a").assertOneGetNewAndReset(), "lowerPrice".split(","), new Object[] {10d});
            EPAssertionUtil.assertProps(env.listener("b").assertOneGetNewAndReset(), "lowerPriceOrderId".split(","), new Object[] {"A"});
            EPAssertionUtil.assertProps(env.listener("c").assertOneGetNewAndReset(), "lowestPrice,highestPrice".split(","), new Object[] {10d, 20d});
            EPAssertionUtil.assertProps(env.listener("d").assertOneGetNewAndReset(), "lowestPrice".split(","), new Object[] {10d});

            env.undeployAll();
        }

    }

    private static class ResultSetAggregateSortedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n", path);

            tryInvalidCompile(env, path, "select MyTable.sortcol.notAnAggMethod() from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.notAnAggMethod()': Could not find event property or method named 'notAnAggMethod' in collection of events of type ");

            tryInvalidCompile(env, path, "select MyTable.sortcol.floorKey() from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.floorKey()': Parameters mismatch for aggregation method 'floorKey', the method requires an expression providing the key value");
            tryInvalidCompile(env, path, "select MyTable.sortcol.floorKey('a') from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.floorKey('a')()': Method 'floorKey' for parameter 0 requires a key of type 'java.lang.Integer' but receives 'java.lang.String'");

            tryInvalidCompile(env, path, "select MyTable.sortcol.firstKey(id) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.firstKey(id)': Parameters mismatch for aggregation method 'firstKey', the method requires no parameters");

            tryInvalidCompile(env, path, "select MyTable.sortcol.submap(1, 2, 3, true) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.submap(1,2,3,true)': Error validating aggregation method 'submap', expected a boolean-type result for expression parameter 1 but received int");
            tryInvalidCompile(env, path, "select MyTable.sortcol.submap('a', true, 3, true) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.submap(\"a\",true,3,true)': Method 'submap' for parameter 0 requires a key of type 'java.lang.Integer' but receives 'java.lang.String'");

            tryInvalidCompile(env, path, "select MyTable.sortcol.submap(1, true, 'a', true) from SupportBean_S0",
                "Failed to validate select-clause expression 'MyTable.sortcol.submap(1,true,\"a\",true)': Method 'submap' for parameter 2 requires a key of type 'java.lang.Integer' but receives 'java.lang.String'");

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(k0 string primary key, sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean group by theString;\n" +
                "@name('s0') select " +
                "MyTable[p00].sortcol.sorted() as sortcol," +
                "MyTable[p00].sortcol.firstKey() as firstkey," +
                "MyTable[p00].sortcol.lastKey() as lastkey" +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            sendAssertGrouped(env, "A", null, null);

            env.sendEventBean(new SupportBean("A", 10));
            env.sendEventBean(new SupportBean("A", 20));
            sendAssertGrouped(env, "A", 10, 20);

            env.sendEventBean(new SupportBean("A", 10));
            env.sendEventBean(new SupportBean("A", 21));
            sendAssertGrouped(env, "A", 10, 21);

            env.sendEventBean(new SupportBean("B", 100));
            sendAssertGrouped(env, "A", 10, 21);
            sendAssertGrouped(env, "B", 100, 100);

            env.undeployAll();
        }

        private static void sendAssertGrouped(RegressionEnvironment env, String p00, Integer firstKey, Integer lastKey) {
            final String[] fields = "firstkey,lastkey".split(",");
            env.sendEventBean(new SupportBean_S0(-1, p00));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{firstKey, lastKey});
        }
    }

    private static class ResultSetAggregateSortedMultiCriteria implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(theString, intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.firstKey() as firstkey," +
                "MyTable.sortcol.lastKey() as lastkey," +
                "MyTable.sortcol.lowerKey(new HashableMultiKey('E4', 1)) as lowerkey," +
                "MyTable.sortcol.higherKey(new HashableMultiKey('E4b', -1)) as higherkey" +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, HashableMultiKey.class, "firstkey,lastkey,lowerkey");

            prepareTestData(env, new TreeMap<>()); // 1, 1, 4, 6, 6, 8, 9

            env.sendEventBean(new SupportBean_S0(-1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            compareKeys(event.get("firstkey"), "E1a", 1);
            compareKeys(event.get("lastkey"), "E9", 9);
            compareKeys(event.get("lowerkey"), "E1b", 1);
            compareKeys(event.get("higherkey"), "E4b", 4);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedSubmapEventsBetween implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema MySubmapEvent as " + MySubmapEvent.class.getName() + ";\n" +
                "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.eventsBetween(fromKey, fromInclusive, toKey, toInclusive) as eb," +
                "MyTable.sortcol.eventsBetween(fromKey, fromInclusive, toKey, toInclusive).lastOf() as eblastof," +
                "MyTable.sortcol.subMap(fromKey, fromInclusive, toKey, toInclusive) as sm" +
                " from MySubmapEvent";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, SupportBean[].class, "eb");
            assertType(env, NavigableMap.class, "sm");
            assertType(env, SupportBean.class, "eblastof");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            for (int start = 0; start < 12; start++) {
                for (int end = 0; end < 12; end++) {
                    if (start > end) {
                        continue;
                    }
                    for (boolean includeStart : new boolean[]{false, true}) {
                        for (boolean includeEnd : new boolean[]{false, true}) {
                            MySubmapEvent sme = new MySubmapEvent(start, includeStart, end, includeEnd);
                            env.sendEventBean(sme);
                            EventBean event = env.listener("s0").assertOneGetNewAndReset();
                            assertEventsBetween(treemap, sme, (SupportBean[]) event.get("eb"), (SupportBean) event.get("eblastof"));
                            assertSubmap(treemap, sme, (NavigableMap<Object, SupportBean[]>) event.get("sm"));
                        }
                    }
                }
            }

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedNavigableMapReference implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.navigableMapReference() as nmr" +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, NavigableMap.class, "nmr");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            env.sendEventBean(new SupportBean_S0(-1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertNavigableMap(treemap, (NavigableMap<Object, Collection<EventBean>>) event.get("nmr"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedGetContainsCounts implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.getEvent(id) as ge," +
                "MyTable.sortcol.getEvents(id) as ges," +
                "MyTable.sortcol.containsKey(id) as ck," +
                "MyTable.sortcol.countEvents() as cnte," +
                "MyTable.sortcol.countKeys() as cntk," +
                "MyTable.sortcol.getEvent(id).theString as geid," +
                "MyTable.sortcol.getEvent(id).firstOf() as gefo," +
                "MyTable.sortcol.getEvents(id).lastOf() as geslo " +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, SupportBean.class, "ge,gefo,geslo");
            assertType(env, SupportBean[].class, "ges");
            assertType(env, Integer.class, "cnte,cntk");
            assertType(env, Boolean.class, "ck");
            assertType(env, String.class, "geid");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            for (int i = 0; i < 12; i++) {
                env.sendEventBean(new SupportBean_S0(i));
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                String message = "failed at " + i;
                assertEquals(message, firstEvent(treemap.get(i)), event.get("ge"));
                EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.get(i)), (SupportBean[]) event.get("ges"));
                assertEquals(message, treemap.containsKey(i), event.get("ck"));
                assertEquals(message, 7, event.get("cnte"));
                assertEquals(message, 5, event.get("cntk"));
                assertEquals(message, firstEventString(treemap.get(i)), event.get("geid"));
                assertEquals(message, firstEvent(treemap.get(i)), event.get("gefo"));
                assertEquals(message, lastEvent(treemap.get(i)), event.get("geslo"));
            }

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedFirstLastEnumerationAndDot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.firstEvent().theString as feid," +
                "MyTable.sortcol.firstEvent().firstOf() as fefo," +
                "MyTable.sortcol.firstEvents().lastOf() as feslo," +
                "MyTable.sortcol.lastEvent().theString() as leid," +
                "MyTable.sortcol.lastEvent().firstOf() as lefo," +
                "MyTable.sortcol.lastEvents().lastOf as leslo" +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, String.class, "feid,leid");
            assertType(env, SupportBean.class, "fefo,feslo,lefo,leslo");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            env.sendEventBean(new SupportBean_S0(-1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(firstEventString(treemap.firstEntry()), event.get("feid"));
            assertEquals(firstEvent(treemap.firstEntry()), event.get("fefo"));
            assertEquals(lastEvent(treemap.firstEntry()), event.get("feslo"));
            assertEquals(firstEventString(treemap.lastEntry()), event.get("leid"));
            assertEquals(firstEvent(treemap.lastEntry()), event.get("lefo"));
            assertEquals(lastEvent(treemap.lastEntry()), event.get("leslo"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedFirstLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.firstEvent() as fe," +
                "MyTable.sortcol.minBy() as minb," +
                "MyTable.sortcol.firstEvents() as fes," +
                "MyTable.sortcol.firstKey() as fk," +
                "MyTable.sortcol.lastEvent() as le," +
                "MyTable.sortcol.maxBy() as maxb," +
                "MyTable.sortcol.lastEvents() as les," +
                "MyTable.sortcol.lastKey() as lk" +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, SupportBean.class, "fe,le,minb,maxb");
            assertType(env, SupportBean[].class, "fes,les");
            assertType(env, Integer.class, "fk,lk");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            env.sendEventBean(new SupportBean_S0(-1));
            EventBean event = env.listener("s0").assertOneGetNewAndReset();
            assertEquals(firstEvent(treemap.firstEntry()), event.get("fe"));
            assertEquals(firstEvent(treemap.firstEntry()), event.get("minb"));
            EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.firstEntry()), (SupportBean[]) event.get("fes"));
            assertEquals(treemap.firstKey(), event.get("fk"));
            assertEquals(firstEvent(treemap.lastEntry()), event.get("le"));
            assertEquals(firstEvent(treemap.lastEntry()), event.get("maxb"));
            EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.lastEntry()), (SupportBean[]) event.get("les"));
            assertEquals(treemap.lastKey(), event.get("lk"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedCFHLEnumerationAndDot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select " +
                "MyTable.sortcol.ceilingEvent(id).theString as ceid," +
                "MyTable.sortcol.ceilingEvent(id).firstOf() as cefo," +
                "MyTable.sortcol.ceilingEvents(id).lastOf() as ceslo," +
                "MyTable.sortcol.floorEvent(id).theString as feid," +
                "MyTable.sortcol.floorEvent(id).firstOf() as fefo," +
                "MyTable.sortcol.floorEvents(id).lastOf() as feslo," +
                "MyTable.sortcol.higherEvent(id).theString as heid," +
                "MyTable.sortcol.higherEvent(id).firstOf() as hefo," +
                "MyTable.sortcol.higherEvents(id).lastOf() as heslo," +
                "MyTable.sortcol.lowerEvent(id).theString as leid," +
                "MyTable.sortcol.lowerEvent(id).firstOf() as lefo," +
                "MyTable.sortcol.lowerEvents(id).lastOf() as leslo " +
                " from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            assertType(env, String.class, "ceid,feid,heid,leid");
            assertType(env, SupportBean.class, "cefo,fefo,hefo,lefo,ceslo,feslo,heslo,leslo");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            for (int i = 0; i < 12; i++) {
                env.sendEventBean(new SupportBean_S0(i));
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                String message = "failed at " + i;
                assertEquals(message, firstEventString(treemap.ceilingEntry(i)), event.get("ceid"));
                assertEquals(message, firstEvent(treemap.ceilingEntry(i)), event.get("cefo"));
                assertEquals(message, lastEvent(treemap.ceilingEntry(i)), event.get("ceslo"));
                assertEquals(message, firstEventString(treemap.floorEntry(i)), event.get("feid"));
                assertEquals(message, firstEvent(treemap.floorEntry(i)), event.get("fefo"));
                assertEquals(message, lastEvent(treemap.floorEntry(i)), event.get("feslo"));
                assertEquals(message, firstEventString(treemap.higherEntry(i)), event.get("heid"));
                assertEquals(message, firstEvent(treemap.higherEntry(i)), event.get("hefo"));
                assertEquals(message, lastEvent(treemap.higherEntry(i)), event.get("heslo"));
                assertEquals(message, firstEventString(treemap.lowerEntry(i)), event.get("leid"));
                assertEquals(message, firstEvent(treemap.lowerEntry(i)), event.get("lefo"));
                assertEquals(message, lastEvent(treemap.lowerEntry(i)), event.get("leslo"));
            }

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedCFHL implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n";
            env.compileDeploy(epl, path);

            String select = "@name('s0') select " +
                "MyTable.sortcol as sortedItself, " +
                "MyTable.sortcol.ceilingEvent(id) as ce, " +
                "MyTable.sortcol.ceilingEvents(id) as ces, " +
                "MyTable.sortcol.ceilingKey(id) as ck, " +
                "MyTable.sortcol.floorEvent(id) as fe, " +
                "MyTable.sortcol.floorEvents(id) as fes, " +
                "MyTable.sortcol.floorKey(id) as fk, " +
                "MyTable.sortcol.higherEvent(id) as he, " +
                "MyTable.sortcol.higherEvents(id) as hes, " +
                "MyTable.sortcol.higherKey(id) as hk, " +
                "MyTable.sortcol.lowerEvent(id) as le, " +
                "MyTable.sortcol.lowerEvents(id) as les, " +
                "MyTable.sortcol.lowerKey(id) as lk" +
                " from SupportBean_S0";
            env.eplToModelCompileDeploy(select, path).addListener("s0");

            assertType(env, SupportBean.class, "ce,fe,he,le");
            assertType(env, SupportBean[].class, "ces,fes,hes,les");
            assertType(env, Integer.class, "ck,fk,hk,lk");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            prepareTestData(env, treemap); // 1, 1, 4, 6, 6, 8, 9

            for (int i = 0; i < 12; i++) {
                env.sendEventBean(new SupportBean_S0(i));
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                assertEquals(firstEvent(treemap.ceilingEntry(i)), event.get("ce"));
                EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.ceilingEntry(i)), (SupportBean[]) event.get("ces"));
                assertEquals(treemap.ceilingKey(i), event.get("ck"));
                assertEquals(firstEvent(treemap.floorEntry(i)), event.get("fe"));
                EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.floorEntry(i)), (SupportBean[]) event.get("fes"));
                assertEquals(treemap.floorKey(i), event.get("fk"));
                assertEquals(firstEvent(treemap.higherEntry(i)), event.get("he"));
                EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.higherEntry(i)), (SupportBean[]) event.get("hes"));
                assertEquals(treemap.higherKey(i), event.get("hk"));
                assertEquals(firstEvent(treemap.lowerEntry(i)), event.get("le"));
                EPAssertionUtil.assertEqualsExactOrder(allEvents(treemap.lowerEntry(i)), (SupportBean[]) event.get("les"));
                assertEquals(treemap.lowerKey(i), event.get("lk"));
            }

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedNonTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0".split(",");
            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();

            String epl = "@name('s0') select sorted(intPrimitive).floorEvent(intPrimitive-1) as c0 from SupportBean#length(3) as sb";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            makeSendBean(env, treemap, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{floorEntryFirstEvent(treemap, 10 - 1)});

            makeSendBean(env, treemap, "E2", 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{floorEntryFirstEvent(treemap, 20 - 1)});

            env.milestone(0);

            makeSendBean(env, treemap, "E3", 15);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{floorEntryFirstEvent(treemap, 15 - 1)});

            makeSendBean(env, treemap, "E3", 17);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{floorEntryFirstEvent(treemap, 17 - 1)});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedTableAccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n" +
                "@name('s0') select MyTable.sortcol.floorEvent(id) as c0 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            makeSendBean(env, treemap, "E1", 10);
            makeSendBean(env, treemap, "E2", 20);
            makeSendBean(env, treemap, "E3", 30);

            env.sendEventBean(new SupportBean_S0(15));
            assertEquals(floorEntryFirstEvent(treemap, 15), env.listener("s0").assertOneGetNewAndReset().get("c0"));

            env.milestone(0);

            for (int i = 0; i < 40; i++) {
                env.sendEventBean(new SupportBean_S0(i));
                assertEquals(floorEntryFirstEvent(treemap, i), env.listener("s0").assertOneGetNewAndReset().get("c0"));
            }

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSortedTableIdent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(sortcol sorted(intPrimitive) @type('SupportBean'));\n" +
                "into table MyTable select sorted(*) as sortcol from SupportBean;\n";
            env.compileDeploy(epl, path);

            env.eplToModelCompileDeploy("@name('s0') select sortcol.floorEvent(id) as c0 from SupportBean_S0, MyTable", path).addListener("s0");

            TreeMap<Integer, List<SupportBean>> treemap = new TreeMap<>();
            makeSendBean(env, treemap, "E1", 10);
            makeSendBean(env, treemap, "E2", 20);
            makeSendBean(env, treemap, "E3", 30);

            env.milestone(0);

            for (int i = 0; i < 40; i++) {
                env.sendEventBean(new SupportBean_S0(i));
                assertEquals(floorEntryFirstEvent(treemap, i), env.listener("s0").assertOneGetNewAndReset().get("c0"));
            }

            env.undeployAll();
        }
    }

    private static SupportBean firstEvent(Map.Entry<Integer, List<SupportBean>> entry) {
        return entry == null ? null : entry.getValue().get(0);
    }

    private static String firstEventString(Map.Entry<Integer, List<SupportBean>> entry) {
        return entry == null ? null : entry.getValue().get(0).getTheString();
    }

    private static String firstEventString(List<SupportBean> list) {
        return list == null ? null : list.get(0).getTheString();
    }

    private static SupportBean[] allEvents(Map.Entry<Integer, List<SupportBean>> entry) {
        return entry == null ? null : entry.getValue().toArray(new SupportBean[0]);
    }

    private static SupportBean[] allEvents(List<SupportBean> list) {
        return list == null ? null : list.toArray(new SupportBean[0]);
    }

    private static SupportBean lastEvent(Map.Entry<Integer, List<SupportBean>> entry) {
        return entry == null ? null : entry.getValue().get(entry.getValue().size() - 1);
    }

    private static SupportBean lastEvent(List<SupportBean> list) {
        return list == null ? null : list.get(list.size() - 1);
    }

    private static SupportBean firstEvent(List<SupportBean> list) {
        return list == null ? null : list.get(0);
    }

    private static void makeSendBean(RegressionEnvironment env, TreeMap<Integer, List<SupportBean>> treemap, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        env.sendEventBean(bean);
        List<SupportBean> existing = treemap.get(intPrimitive);
        if (existing == null) {
            existing = new ArrayList<>();
            treemap.put(intPrimitive, existing);
        }
        existing.add(bean);
        treemap.put(bean.getIntPrimitive(), existing);
    }

    private static SupportBean floorEntryFirstEvent(TreeMap<Integer, List<SupportBean>> treemap, int key) {
        return treemap.floorEntry(key) == null ? null : treemap.floorEntry(key).getValue().get(0);
    }

    private static void prepareTestData(RegressionEnvironment env, TreeMap<Integer, List<SupportBean>> treemap) {
        makeSendBean(env, treemap, "E1a", 1);
        makeSendBean(env, treemap, "E1b", 1);
        makeSendBean(env, treemap, "E4b", 4);
        makeSendBean(env, treemap, "E6a", 6);
        makeSendBean(env, treemap, "E6b", 6);
        makeSendBean(env, treemap, "E8", 8);
        makeSendBean(env, treemap, "E9", 9);
    }

    static void assertType(RegressionEnvironment env, Class expected, String csvProps) {
        String[] props = csvProps.split(",");
        EventType eventType = env.statement("s0").getEventType();
        for (String prop : props) {
            assertEquals("failed for prop '" + prop + "'", expected, eventType.getPropertyType(prop));
        }
    }

    private static void assertEventsBetween(TreeMap<Integer, List<SupportBean>> treemap, MySubmapEvent sme, SupportBean[] events, SupportBean lastOf) {
        NavigableMap<Integer, List<SupportBean>> submap = treemap.subMap(sme.fromKey, sme.fromInclusive, sme.toKey, sme.toInclusive);
        List<SupportBean> all = new ArrayList<>();
        for (Map.Entry<Integer, List<SupportBean>> entry : submap.entrySet()) {
            all.addAll(entry.getValue());
        }
        EPAssertionUtil.assertEqualsExactOrder(all.toArray(), events);
        if (all.isEmpty()) {
            assertNull(lastOf);
        } else {
            assertEquals(all.get(all.size() - 1), lastOf);
        }
    }

    private static void assertSubmap(TreeMap<Integer, List<SupportBean>> treemap, MySubmapEvent sme, NavigableMap<Object, SupportBean[]> actual) {
        NavigableMap<Integer, List<SupportBean>> expected = treemap.subMap(sme.fromKey, sme.fromInclusive, sme.toKey, sme.toInclusive);
        assertEquals(expected.size(), actual.size());
        for (Integer key : expected.keySet()) {
            SupportBean[] expectedEvents = expected.get(key).toArray(new SupportBean[0]);
            SupportBean[] actualEvents = actual.get(key);
            EPAssertionUtil.assertEqualsExactOrder(expectedEvents, actualEvents);
        }
    }

    private static void assertNavigableMap(TreeMap<Integer, List<SupportBean>> treemap, NavigableMap<Object, Collection<EventBean>> actual) {
        assertEquals(treemap.size(), actual.size());
        for (Integer key : treemap.keySet()) {
            SupportBean[] expectedEvents = treemap.get(key).toArray(new SupportBean[0]);
            EPAssertionUtil.assertEqualsExactOrder(expectedEvents, toArrayOfUnderlying(actual.get(key)));
        }

        compareEntry(treemap.firstEntry(), actual.firstEntry());
        compareEntry(treemap.lastEntry(), actual.lastEntry());
        compareEntry(treemap.floorEntry(5), actual.floorEntry(5));
        compareEntry(treemap.ceilingEntry(5), actual.ceilingEntry(5));
        compareEntry(treemap.lowerEntry(5), actual.lowerEntry(5));
        compareEntry(treemap.higherEntry(5), actual.higherEntry(5));

        assertEquals(treemap.firstKey(), actual.firstKey());
        assertEquals(treemap.lastKey(), actual.lastKey());
        assertEquals(treemap.floorKey(5), actual.floorKey(5));
        assertEquals(treemap.ceilingKey(5), actual.ceilingKey(5));
        assertEquals(treemap.lowerKey(5), actual.lowerKey(5));
        assertEquals(treemap.higherKey(5), actual.higherKey(5));

        assertEquals(treemap.containsKey(5), actual.containsKey(5));
        assertEquals(treemap.isEmpty(), actual.isEmpty());

        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 4, 6, 8, 9}, actual.keySet().toArray());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 4, 6, 8, 9}, actual.navigableKeySet().toArray());

        EPAssertionUtil.assertEqualsExactOrder(new Object[]{9, 8, 6, 4, 1}, actual.descendingMap().keySet().toArray());
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{9, 8, 6, 4, 1}, actual.descendingKeySet().toArray());

        assertEquals(1, actual.subMap(9, 10).size());
        assertEquals(1, actual.subMap(9, true, 9, true).size());
        assertEquals(1, actual.tailMap(9).size());
        assertEquals(1, actual.tailMap(9, true).size());
        assertEquals(1, actual.headMap(2).size());
        assertEquals(1, actual.headMap(2, false).size());

        assertEquals(5, actual.entrySet().size());
        assertEquals(5, actual.values().size());

        // collection tests
        Collection<Collection<EventBean>> coll = actual.values();
        assertEquals(5, coll.size());
        assertFalse(coll.isEmpty());
        Iterator<Collection<EventBean>> it = coll.iterator();
        EPAssertionUtil.assertEqualsExactOrder(treemap.get(1).toArray(), toArrayOfUnderlying(it.next()));
        assertTrue(it.hasNext());
        assertEquals(5, coll.toArray().length);
        EPAssertionUtil.assertEqualsExactOrder(treemap.get(1).toArray(), toArrayOfUnderlying((Collection<EventBean>) coll.toArray()[0]));
        assertNotNull(coll.spliterator());
        assertNotNull(coll.stream());
        assertNotNull(coll.parallelStream());
        coll.forEach(c -> {
        });

        // navigable set tests
        NavigableSet<Object> nks = actual.navigableKeySet();
        Iterator<Object> nksit = nks.iterator();
        assertEquals(1, nksit.next());
        assertTrue(nksit.hasNext());
        assertNotNull(nks.comparator());
        assertEquals(1, nks.first());
        assertEquals(9, nks.last());
        assertEquals(5, nks.size());
        assertFalse(nks.isEmpty());
        assertTrue(nks.contains(6));
        assertNotNull(nks.toArray());
        assertNotNull(nks.toArray(new Integer[0]));
        assertNotNull(nks.spliterator());
        assertNotNull(nks.stream());
        assertNotNull(nks.parallelStream());
        assertEquals(4, nks.lower(5));
        assertEquals(6, nks.higher(5));
        assertEquals(4, nks.floor(5));
        assertEquals(6, nks.ceiling(5));
        assertNotNull(nks.descendingSet());
        assertNotNull(nks.descendingIterator());
        nks.forEach(a -> {
        });
        assertNotNull(nks.subSet(1, true, 100, true));
        assertNotNull(nks.headSet(100, true));
        assertNotNull(nks.tailSet(1, true));
        assertNotNull(nks.subSet(1, 100));
        assertNotNull(nks.headSet(100));
        assertNotNull(nks.tailSet(1));

        // entry set
        Set<Map.Entry<Object, Collection<EventBean>>> set = actual.entrySet();
        assertFalse(set.isEmpty());
        Iterator<Map.Entry<Object, Collection<EventBean>>> setit = set.iterator();
        Map.Entry<Object, Collection<EventBean>> entry = setit.next();
        assertEquals(1, entry.getKey());
        assertTrue(setit.hasNext());
        EPAssertionUtil.assertEqualsExactOrder(treemap.get(1).toArray(), toArrayOfUnderlying(entry.getValue()));
        Map.Entry<Object, Collection<EventBean>>[] array = set.toArray(new Map.Entry[0]);
        assertEquals(5, array.length);
        assertEquals(1, array[0].getKey());
        EPAssertionUtil.assertEqualsExactOrder(treemap.get(1).toArray(), toArrayOfUnderlying(array[0].getValue()));
        assertNotNull(set.toArray());
        set.forEach(a -> {
        });

        // sorted map
        SortedMap<Object, Collection<EventBean>> events = actual.headMap(100);
        assertEquals(5, events.size());
    }

    private static void compareEntry(Map.Entry<Integer, List<SupportBean>> expected, Map.Entry<Object, Collection<EventBean>> actual) {
        assertEquals(expected.getKey(), actual.getKey());
        EPAssertionUtil.assertEqualsExactOrder(expected.getValue().toArray(), toArrayOfUnderlying(actual.getValue()));
    }

    private static SupportBean[] toArrayOfUnderlying(Collection<EventBean> eventBeans) {
        SupportBean[] events = new SupportBean[eventBeans.size()];
        int index = 0;
        for (EventBean event : eventBeans) {
            events[index++] = (SupportBean) event.getUnderlying();
        }
        return events;
    }

    private static void compareKeys(Object key, Object... keys) {
        EPAssertionUtil.assertEqualsExactOrder(((HashableMultiKey) key).getKeys(), keys);
    }

    public static class MySubmapEvent {
        private final int fromKey;
        private final boolean fromInclusive;
        private final int toKey;
        private final boolean toInclusive;

        public MySubmapEvent(int fromKey, boolean fromInclusive, int toKey, boolean toInclusive) {
            this.fromKey = fromKey;
            this.fromInclusive = fromInclusive;
            this.toKey = toKey;
            this.toInclusive = toInclusive;
        }

        public int getFromKey() {
            return fromKey;
        }

        public boolean isFromInclusive() {
            return fromInclusive;
        }

        public int getToKey() {
            return toKey;
        }

        public boolean isToInclusive() {
            return toInclusive;
        }
    }
}
