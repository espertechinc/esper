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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.subscriber.SupportSubscriberMultirowObjectArrayNStmt;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableSelect {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableSelectStarPublicTypeVisibility());
        execs.add(new InfraTableSelectEnum());
        execs.add(new InfraTableSelectMultikeyWArraySingleArray());
        execs.add(new InfraTableSelectMultikeyWArrayTwoArray());
        execs.add(new InfraTableSelectMultikeyWArrayComposite());
        return execs;
    }

    private static class InfraTableSelectMultikeyWArrayComposite implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(k0 string primary key, k1 string primary key, k2 string primary key, v string);\n" +
                "create index MyIndex on MyTable(k0, k1, v btree);\n" +
                "insert into MyTable select p00 as k0, p01 as k1, p02 as k2, p03 as v from SupportBean_S0;\n" +
                "@name('s0') select t.v as v from SupportBean_S1, MyTable as t where k0 = p10 and k1 = p11 and v > p12;\n";
            env.compileDeploy(epl, path).addListener("s0");

            sendS0(env, "A", "BB", "CCC", "X1");
            sendS0(env, "A", "BB", "DDDD", "X4");
            sendS0(env, "A", "CC", "CCC", "X3");
            sendS0(env, "C", "CC", "CCC", "X4");

            env.milestone(0);

            sendS1Assert(env, "A", "CC", "", "X3");
            sendS1Assert(env, "C", "CC", "", "X4");
            sendS1Assert(env, "A", "BB", "X3", "X4");
            sendS1Assert(env, "A", "BB", "Z", null);

            env.undeployAll();
        }

        private void sendS0(RegressionEnvironment env, String p00, String p01, String p02, String p03) {
            env.sendEventBean(new SupportBean_S0(0, p00, p01, p02, p03));
        }

        private void sendS1Assert(RegressionEnvironment env, String p10, String p11, String p12, String expected) {
            env.sendEventBean(new SupportBean_S1(0, p10, p11, p12));
            if (expected == null) {
                assertFalse(expected, env.listener("s0").isInvoked());
            } else {
                assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("v"));
            }
        }
    }

    private static class InfraTableSelectMultikeyWArrayTwoArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(k1 int[primitive] primary key, k2 int[primitive] primary key, value int);\n" +
                "insert into MyTable select intOne as k1, intTwo as k2, value from SupportEventWithManyArray(id = 'I');\n" +
                "@name('s0') select t.value as c0 from SupportEventWithManyArray(id='Q'), MyTable as t where k1 = intOne and k2 = intTwo;\n";
            env.compileDeploy(epl, path).addListener("s0");

            sendManyArray(env, "I", new int[]{1, 2}, new int[]{3, 4}, 10);
            sendManyArray(env, "I", new int[]{1, 3}, new int[]{1}, 20);
            sendManyArray(env, "I", new int[]{2}, new int[]{}, 30);

            env.milestone(0);

            sendManyArrayAssert(env, "Q", new int[]{2}, new int[0], 30);
            sendManyArrayAssert(env, "Q", new int[]{1, 2}, new int[]{3, 4}, 10);
            sendManyArrayAssert(env, "Q", new int[]{1, 3}, new int[]{1}, 20);

            env.undeployAll();
        }

        private void sendManyArrayAssert(RegressionEnvironment env, String id, int[] intOne, int[] intTwo, int expected) {
            sendManyArray(env, id, intOne, intTwo, -1);
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] intOne, int[] intTwo, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne).withIntTwo(intTwo).withValue(value));
        }
    }

    private static class InfraTableSelectMultikeyWArraySingleArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(k int[primitive] primary key, value int);\n" +
                "insert into MyTable select array as k, value from SupportEventWithIntArray;\n" +
                "@name('s0') select t.value as c0 from SupportEventWithManyArray, MyTable as t where k = intOne;\n";
            env.compileDeploy(epl, path).addListener("s0");

            sendIntArray(env, "E1", new int[]{1, 2}, 10);
            sendIntArray(env, "E2", new int[]{1, 3}, 20);
            sendIntArray(env, "E3", new int[]{2}, 30);

            env.milestone(0);

            sendAssertManyArray(env, new int[]{2}, 30);
            sendAssertManyArray(env, new int[]{1, 3}, 20);
            sendAssertManyArray(env, new int[]{1, 2}, 10);

            env.undeployAll();
        }

        private void sendAssertManyArray(RegressionEnvironment env, int[] ints, int expected) {
            env.sendEventBean(new SupportEventWithManyArray().withIntOne(ints));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }

        private void sendIntArray(RegressionEnvironment env, String id, int[] ints, int value) {
            env.sendEventBean(new SupportEventWithIntArray(id, ints, value));
        }
    }

    private static class InfraTableSelectEnum implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable(p string);\n" +
                "@name('s0') select t.firstOf() as c0 from MyTable as t;\n";
            env.compileDeploy(epl, path);
            env.compileExecuteFAF("insert into MyTable select 'a' as p", path);

            EventBean event = env.iterator("s0").next();
            Object[] row = (Object[]) event.get("c0");
            assertEquals("a", row[0]);

            env.undeployAll();
        }
    }

    private static class InfraTableSelectStarPublicTypeVisibility implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            AtomicLong currentTime = new AtomicLong(0);
            env.advanceTime(currentTime.get());
            RegressionPath path = new RegressionPath();

            env.compileDeploy("@name('create') create table MyTable as (\n" +
                "key string primary key,\n" +
                "totalInt sum(int),\n" +
                "p0 string,\n" +
                "winsb window(*) @type(SupportBean),\n" +
                "totalLong sum(long),\n" +
                "p1 string,\n" +
                "winsb0 window(*) @type(SupportBean_S0)\n" +
                ")", path);
            Object[][] expectedType = new Object[][]{
                {"key", String.class},
                {"totalInt", Integer.class},
                {"p0", String.class},
                {"winsb", SupportBean[].class},
                {"totalLong", Long.class},
                {"p1", String.class},
                {"winsb0", SupportBean_S0[].class},
            };

            env.compileDeploy("into table MyTable " +
                "select sum(intPrimitive) as totalInt, sum(longPrimitive) as totalLong," +
                "window(*) as winsb from SupportBean#keepall group by theString", path);
            env.compileDeploy("into table MyTable " +
                "select window(*) as winsb0 from SupportBean_S0#keepall group by p00", path);
            env.compileDeploy("on SupportBean_S1 " +
                "merge MyTable where p10 = key when matched then " +
                "update set p0 = p11, p1 = p12", path);

            SupportBean e1Sb = makeSupportBean("G1", 10, 100);
            env.sendEventBean(e1Sb); // update some aggs

            SupportBean_S0 e2Sb0 = new SupportBean_S0(5, "G1");
            env.sendEventBean(e2Sb0); // update more aggs

            env.sendEventBean(new SupportBean_S1(6, "G1", "a", "b")); // merge more values

            Object[] rowValues = {"G1", 10, "a", new SupportBean[]{e1Sb}, 100L, "b", new SupportBean_S0[]{e2Sb0}};
            runAssertionSubqueryWindowAgg(env, path, rowValues);
            runAssertionOnSelectWindowAgg(env, path, expectedType, rowValues);
            runAssertionSubquerySelectStar(env, path, rowValues);
            runAssertionSubquerySelectWEnumMethod(env, path, rowValues);
            runAssertionIterateCreateTable(env, expectedType, rowValues, env.statement("create"));
            runAssertionJoinSelectStar(env, path, expectedType, rowValues);
            runAssertionJoinSelectStreamName(env, path, expectedType, rowValues);
            runAssertionJoinSelectStreamStarNamed(env, path, expectedType, rowValues);
            runAssertionJoinSelectStreamStarUnnamed(env, path, expectedType, rowValues);
            runAssertionInsertIntoBean(env, path, rowValues);
            runAssertionSingleRowFunc(env, path, rowValues);
            runAssertionOutputSnapshot(env, path, expectedType, rowValues, currentTime);
            runAssertionFireAndForgetSelectStar(env, path, expectedType, rowValues);
            runAssertionFireAndForgetInsertUpdateDelete(env, path, expectedType);

            env.undeployAll();
        }
    }

    private static void runAssertionSubqueryWindowAgg(RegressionEnvironment env, RegressionPath path, Object[] rowValues) {
        env.compileDeploy("@name('s0') select " +
            "(select window(mt.*) from MyTable as mt) as c0," +
            "(select first(mt.*) from MyTable as mt) as c1" +
            " from SupportBean_S2", path).addListener("s0");

        env.sendEventBean(new SupportBean_S2(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventUnd(((Object[][]) event.get("c0"))[0], rowValues);
        assertEventUnd(event.get("c1"), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionOnSelectWindowAgg(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        env.compileDeploy("@name('s0') on SupportBean_S2 select " +
            "window(win.*) as c0," +
            "last(win.*) as c1, " +
            "first(win.*) as c2, " +
            "first(p1) as c3," +
            "window(p1) as c4," +
            "sorted(p1) as c5," +
            "minby(p1) as c6" +
            " from MyTable as win", path).addListener("s0");

        env.sendEventBean(new SupportBean_S2(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        for (String col : "c1,c2,c6".split(",")) {
            assertEventUnd(event.get(col), rowValues);
        }
        for (String col : "c0,c5".split(",")) {
            assertEventUnd(((Object[][]) event.get(col))[0], rowValues);
        }
        assertEquals("b", event.get("c3"));
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"b"}, (String[]) event.get("c4"));

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionOutputSnapshot(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues, AtomicLong currentTime) {
        env.compileDeploy("@name('s0') select * from MyTable output snapshot every 1 second", path).addListener("s0");
        assertEventType(env.statement("s0").getEventType(), expectedType);

        currentTime.set(currentTime.get() + 1000L);
        env.advanceTime(currentTime.get());
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType(), expectedType, event.getUnderlying(), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionFireAndForgetInsertUpdateDelete(RegressionEnvironment env, RegressionPath path, Object[][] expectedType) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF("insert into MyTable(key) values ('dummy')", path);
        assertEventType(result.getEventType(), expectedType);

        result = env.compileExecuteFAF("delete from MyTable where key = 'dummy'", path);
        assertEventType(result.getEventType(), expectedType);

        result = env.compileExecuteFAF("update MyTable set key='dummy' where key='dummy'", path);
        assertEventType(result.getEventType(), expectedType);
    }

    private static void runAssertionIterateCreateTable(RegressionEnvironment env, Object[][] expectedType, Object[] rowValues, EPStatement stmtCreate) {
        assertEventTypeAndEvent(stmtCreate.getEventType(), expectedType, stmtCreate.iterator().next().getUnderlying(), rowValues);
    }

    private static void runAssertionSingleRowFunc(RegressionEnvironment env, RegressionPath path, Object[] rowValues) {
        // try join passing of params
        String eplJoin = "@name('s0') select " +
            InfraTableSelect.class.getName() + ".myServiceEventBean(mt) as c0, " +
            InfraTableSelect.class.getName() + ".myServiceObjectArray(mt) as c1 " +
            "from SupportBean_S2, MyTable as mt";
        env.compileDeploy(eplJoin, path).addListener("s0");

        env.sendEventBean(new SupportBean_S2(0));
        EventBean result = env.listener("s0").assertOneGetNewAndReset();
        assertEventUnd(result.get("c0"), rowValues);
        assertEventUnd(result.get("c1"), rowValues);
        env.undeployModuleContaining("s0");

        // try subquery
        String eplSubquery = "@name('s0') select (select pluginServiceEventBean(mt) from MyTable as mt) as c0 " +
            "from SupportBean_S2";
        env.compileDeploy(eplSubquery, path).addListener("s0");

        env.sendEventBean(new SupportBean_S2(0));
        result = env.listener("s0").assertOneGetNewAndReset();
        assertEventUnd(result.get("c0"), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionInsertIntoBean(RegressionEnvironment env, RegressionPath path, Object[] rowValues) {
        String epl = "@name('s0') insert into SupportCtorSB2WithObjectArray select * from SupportBean_S2, MyTable";
        env.compileDeploy(epl, path).addListener("s0");

        env.sendEventBean(new SupportBean_S2(0));
        assertEventUnd(env.listener("s0").assertOneGetNewAndReset().get("arr"), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionSubquerySelectWEnumMethod(RegressionEnvironment env, RegressionPath path, Object[] rowValues) {
        String epl = "@name('s0') select (select * from MyTable).where(v=>v.key = 'G1') as mt from SupportBean_S2";
        env.compileDeploy(epl, path).addListener("s0");

        assertEquals(Collection.class, env.statement("s0").getEventType().getPropertyType("mt"));

        env.sendEventBean(new SupportBean_S2(0));
        Collection coll = (Collection) env.listener("s0").assertOneGetNewAndReset().get("mt");
        assertEventUnd(coll.iterator().next(), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionSubquerySelectStar(RegressionEnvironment env, RegressionPath path, Object[] rowValues) {
        String eplFiltered = "@name('s0') select (select * from MyTable where key = 'G1') as mt from SupportBean_S2";
        runAssertionSubquerySelectStar(env, path, rowValues, eplFiltered);

        String eplUnfiltered = "@name('s0') select (select * from MyTable) as mt from SupportBean_S2";
        runAssertionSubquerySelectStar(env, path, rowValues, eplUnfiltered);

        // With @eventbean
        String eplEventBean = "@name('s0') select (select * from MyTable) @eventbean as mt from SupportBean_S2";
        env.compileDeploy(eplEventBean, path).addListener("s0");
        assertEquals(Object[][].class, env.statement("s0").getEventType().getPropertyType("mt"));
        assertSame(env.statement("create").getEventType(), env.statement("s0").getEventType().getFragmentType("mt").getFragmentType());

        env.sendEventBean(new SupportBean_S2(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        Object[][] value = (Object[][]) event.get("mt");
        assertEventUnd(value[0], rowValues);
        assertSame(env.statement("create").getEventType(), ((EventBean[]) event.getFragment("mt"))[0].getEventType());

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionSubquerySelectStar(RegressionEnvironment env, RegressionPath path, Object[] rowValues, String epl) {
        env.compileDeploy(epl, path).addListener("s0");

        assertEquals(Object[].class, env.statement("s0").getEventType().getPropertyType("mt"));

        env.sendEventBean(new SupportBean_S2(0));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventUnd(event.get("mt"), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionJoinSelectStreamStarUnnamed(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "@name('s0') select mt.* from MyTable as mt, SupportBean_S2 where key = p20";
        env.compileDeploy(joinEpl, path).addListener("s0");
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        env.statement("s0").setSubscriber(subscriber);

        assertEventType(env.statement("s0").getEventType(), expectedType);

        // listener assertion
        env.sendEventBean(new SupportBean_S2(0, "G1"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType(), expectedType, event.getUnderlying(), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionJoinSelectStreamStarNamed(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "@name('s0') select mt.* as mymt from MyTable as mt, SupportBean_S2 where key = p20";
        env.compileDeploy(joinEpl, path).addListener("s0");
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        env.statement("s0").setSubscriber(subscriber);

        assertEventType(env.statement("s0").getEventType().getFragmentType("mymt").getFragmentType(), expectedType);

        // listener assertion
        env.sendEventBean(new SupportBean_S2(0, "G1"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("mymt").getFragmentType(),
            expectedType, event.get("mymt"), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionJoinSelectStreamName(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "@name('s0') select mt from MyTable as mt, SupportBean_S2 where key = p20";
        env.compileDeploy(joinEpl, path).addListener("s0");

        assertEventType(env.statement("s0").getEventType().getFragmentType("mt").getFragmentType(), expectedType);

        env.sendEventBean(new SupportBean_S2(0, "G1"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("mt").getFragmentType(),
            expectedType, event.get("mt"), rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionJoinSelectStar(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        String joinEpl = "@name('s0') select * from MyTable, SupportBean_S2 where key = p20";
        env.compileDeploy(joinEpl, path).addListener("s0");
        SupportSubscriberMultirowObjectArrayNStmt subscriber = new SupportSubscriberMultirowObjectArrayNStmt();
        env.statement("s0").setSubscriber(subscriber);

        assertEventType(env.statement("s0").getEventType().getFragmentType("stream_0").getFragmentType(), expectedType);

        // listener assertion
        env.sendEventBean(new SupportBean_S2(0, "G1"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEventTypeAndEvent(event.getEventType().getFragmentType("stream_0").getFragmentType(),
            expectedType, event.get("stream_0"), rowValues);

        // subscriber assertion
        Object[][] newData = subscriber.getAndResetIndicateArr().get(0).getFirst();
        assertEventUnd(newData[0][0], rowValues);

        env.undeployModuleContaining("s0");
    }

    private static void runAssertionFireAndForgetSelectStar(RegressionEnvironment env, RegressionPath path, Object[][] expectedType, Object[] rowValues) {
        EPFireAndForgetQueryResult result = env.compileExecuteFAF("select * from MyTable where key = 'G1'", path);
        assertEventTypeAndEvent(result.getEventType(), expectedType, result.getArray()[0].getUnderlying(), rowValues);
    }

    private static void assertEventTypeAndEvent(EventType eventType, Object[][] expectedType, Object underlying, Object[] expectedValues) {
        assertEventType(eventType, expectedType);
        assertEventUnd(underlying, expectedValues);
    }

    private static void assertEventUnd(Object underlying, Object[] expectedValues) {
        Object[] und = (Object[]) underlying;
        EPAssertionUtil.assertEqualsExactOrder(expectedValues, und);
    }

    private static void assertEventType(EventType eventType, Object[][] expectedType) {
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, int longPrimitive) {
        SupportBean supportBean = new SupportBean(theString, intPrimitive);
        supportBean.setLongPrimitive(longPrimitive);
        return supportBean;
    }

    public static Object[] myServiceEventBean(EventBean event) {
        return (Object[]) event.getUnderlying();
    }

    public static Object[] myServiceObjectArray(Object[] data) {
        return data;
    }

}
