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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableAccessAggregationState {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        execs.add(new InfraAccessAggShare());
        execs.add(new InfraTableAccessGroupedMixed());
        execs.add(new InfraTableAccessGroupedThreeKey());
        execs.add(new InfraNestedMultivalueAccess(false, false));
        execs.add(new InfraNestedMultivalueAccess(true, false));
        execs.add(new InfraNestedMultivalueAccess(false, true));
        execs.add(new InfraNestedMultivalueAccess(true, true));

        return execs;
    }

    private static class InfraNestedMultivalueAccess implements RegressionExecution {
        private final boolean grouped;
        private final boolean soda;

        public InfraNestedMultivalueAccess(boolean grouped, boolean soda) {
            this.grouped = grouped;
            this.soda = soda;
        }

        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varagg (" +
                (grouped ? "key string primary key, " : "") + "windowSupportBean window(*) @type('SupportBean'))";
            env.compileDeploy(soda, eplDeclare, path);

            String eplInto = "into table varagg " +
                "select window(*) as windowSupportBean from SupportBean#length(2)" +
                (grouped ? " group by theString" : "");
            env.compileDeploy(soda, eplInto, path);

            String key = grouped ? "[\"E1\"]" : "";
            String eplSelect = "@name('s0') select " +
                "varagg" + key + ".windowSupportBean.last(*) as c0, " +
                "varagg" + key + ".windowSupportBean.window(*) as c1, " +
                "varagg" + key + ".windowSupportBean.first(*) as c2, " +
                "varagg" + key + ".windowSupportBean.last(intPrimitive) as c3, " +
                "varagg" + key + ".windowSupportBean.window(intPrimitive) as c4, " +
                "varagg" + key + ".windowSupportBean.first(intPrimitive) as c5" +
                " from SupportBean_S0";
            env.compileDeploy(soda, eplSelect, path).addListener("s0");

            Object[][] expectedAggType = new Object[][]{
                {"c0", SupportBean.class}, {"c1", SupportBean[].class}, {"c2", SupportBean.class},
                {"c3", Integer.class}, {"c4", Integer[].class}, {"c5", Integer.class}};
            SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportBean b1 = makeSendBean(env, "E1", 10);
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{b1, new Object[]{b1}, b1, 10, new int[]{10}, 10});

            SupportBean b2 = makeSendBean(env, "E1", 20);
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{b2, new Object[]{b1, b2}, b1, 20, new int[]{10, 20}, 10});

            env.milestone(0);

            SupportBean b3 = makeSendBean(env, "E1", 30);
            env.sendEventBean(new SupportBean_S0(0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{b3, new Object[]{b2, b3}, b2, 30, new int[]{20, 30}, 20});

            env.undeployAll();
        }
    }

    private static class InfraAccessAggShare implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varagg (mywin window(*) @type(SupportBean))", path);

            env.compileDeploy("@name('into') into table varagg " +
                "select window(sb.*) as mywin from SupportBean#time(10 sec) as sb", path).addListener("into");
            assertEquals(SupportBean[].class, env.statement("into").getEventType().getPropertyType("mywin"));

            env.compileDeploy("@name('s0') select varagg.mywin as c0 from SupportBean_S0", path).addListener("s0");
            assertEquals(SupportBean[].class, env.statement("s0").getEventType().getPropertyType("c0"));

            SupportBean b1 = makeSendBean(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("into").assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[]{b1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{new Object[]{b1}});

            SupportBean b2 = makeSendBean(env, "E2", 20);
            EPAssertionUtil.assertProps(env.listener("into").assertOneGetNewAndReset(), "mywin".split(","), new Object[]{new SupportBean[]{b1, b2}});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{new Object[]{b1, b2}});

            env.undeployAll();
        }
    }

    public static class InfraTableAccessGroupedThreeKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varTotal (key0 string primary key, key1 int primary key," +
                "key2 long primary key, total sum(double), cnt count(*))";
            env.compileDeploy(eplDeclare, path);

            String eplBind = "into table varTotal " +
                "select sum(doublePrimitive) as total, count(*) as cnt " +
                "from SupportBean group by theString, intPrimitive, longPrimitive";
            env.compileDeploy(eplBind, path);

            env.milestone(0);

            String[] fields = "c0,c1".split(",");
            String eplUse = "@name('s0') select varTotal[p00, id, 100L].total as c0, varTotal[p00, id, 100L].cnt as c1 from SupportBean_S0";
            env.compileDeploy(eplUse, path).addListener("s0");

            makeSendBean(env, "E1", 10, 100, 1000);

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1000.0, 1L});

            env.milestone(2);

            makeSendBean(env, "E1", 10, 100, 1001);

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(10, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2001.0, 2L});

            env.undeployAll();
        }
    }

    private static class InfraTableAccessGroupedMixed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // create table
            RegressionPath path = new RegressionPath();
            String eplDeclare = "create table varMyAgg (" +
                "key string primary key, " +
                "c0 count(*), " +
                "c1 count(distinct int), " +
                "c2 window(*) @type('SupportBean'), " +
                "c3 sum(long)" +
                ")";
            env.compileDeploy(eplDeclare, path);

            env.milestone(0);

            // create into-table aggregation
            String eplBind = "into table varMyAgg select " +
                "count(*) as c0, " +
                "count(distinct intPrimitive) as c1, " +
                "window(*) as c2, " +
                "sum(longPrimitive) as c3 " +
                "from SupportBean#length(3) group by theString";
            env.compileDeploy(eplBind, path);

            env.milestone(1);

            // create query for state
            String eplSelect = "@name('s0') select " +
                "varMyAgg[p00].c0 as c0, " +
                "varMyAgg[p00].c1 as c1, " +
                "varMyAgg[p00].c2 as c2, " +
                "varMyAgg[p00].c3 as c3" +
                " from SupportBean_S0";
            env.compileDeploy(eplSelect, path).addListener("s0");
            String[] fields = "c0,c1,c2,c3".split(",");

            env.milestone(2);

            SupportBean b1 = makeSendBean(env, "E1", 10, 100);
            SupportBean b2 = makeSendBean(env, "E1", 11, 101);

            env.milestone(3);

            SupportBean b3 = makeSendBean(env, "E1", 10, 102);

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(0, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{3L, 2L, new SupportBean[]{b1, b2, b3}, 303L});

            env.milestone(5);

            env.sendEventBean(new SupportBean_S0(0, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{null, null, null, null});

            env.milestone(6);

            SupportBean b4 = makeSendBean(env, "E2", 20, 200);
            env.sendEventBean(new SupportBean_S0(0, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 1L, new SupportBean[]{b4}, 200L});

            env.undeployAll();
        }
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        return makeSendBean(env, theString, intPrimitive, longPrimitive, -1);
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        return makeSendBean(env, theString, intPrimitive, longPrimitive, doublePrimitive, -1);
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive, double doublePrimitive, float floatPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setFloatPrimitive(floatPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean makeSendBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }
}
