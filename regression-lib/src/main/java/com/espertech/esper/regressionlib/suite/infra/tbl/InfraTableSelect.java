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
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.subscriber.SupportSubscriberMultirowObjectArrayNStmt;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableSelect {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraTableSelectStarPublicTypeVisibility());
        execs.add(new InfraTableSelectEnum());
        return execs;
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
