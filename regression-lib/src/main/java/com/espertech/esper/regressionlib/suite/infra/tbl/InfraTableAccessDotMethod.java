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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableAccessDotMethod {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraPlainPropDatetimeAndEnumerationAndMethod());
        execs.add(new InfraAggDatetimeAndEnumerationAndMethod());
        execs.add(new InfraNestedDotMethod());
        return execs;
    }

    private static class InfraAggDatetimeAndEnumerationAndMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runAggregationWDatetimeEtc(env, false, false, milestone);
            runAggregationWDatetimeEtc(env, true, false, milestone);
            runAggregationWDatetimeEtc(env, false, true, milestone);
            runAggregationWDatetimeEtc(env, true, true, milestone);
        }
    }

    private static class InfraPlainPropDatetimeAndEnumerationAndMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            runPlainPropertyWDatetimeEtc(env, false, false, milestone);
            runPlainPropertyWDatetimeEtc(env, true, false, milestone);
            runPlainPropertyWDatetimeEtc(env, false, true, milestone);
            runPlainPropertyWDatetimeEtc(env, true, true, milestone);
        }

        private static void runPlainPropertyWDatetimeEtc(RegressionEnvironment env, boolean grouped, boolean soda, AtomicInteger milestone) {

            String myBean = MyBean.class.getName();
            RegressionPath path = new RegressionPath();
            String eplType = "create objectarray schema MyEvent as (p0 string);" +
                "create objectarray schema PopulateEvent as (" +
                "key string, ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])";
            env.compileDeployWBusPublicType(eplType, path);

            String eplDeclare = "create table varaggPWD (key string" + (grouped ? " primary key" : "") +
                ", ts long" +
                ", mb " + myBean +
                ", mbarr " + myBean + "[]" +
                ", me MyEvent, mearr MyEvent[])";
            env.compileDeploy(soda, eplDeclare, path);

            String key = grouped ? "[\"E1\"]" : "";
            String eplSelect = "@name('s0') select " +
                "varaggPWD" + key + ".ts.getMinuteOfHour() as c0, " +
                "varaggPWD" + key + ".mb.getMyProperty() as c1, " +
                "varaggPWD" + key + ".mbarr.takeLast(1) as c2, " +
                "varaggPWD" + key + ".me.p0 as c3, " +
                "varaggPWD" + key + ".mearr.selectFrom(i => i.p0) as c4 " +
                "from SupportBean_S0";
            env.compileDeploy(eplSelect, path);
            env.addListener("s0");

            String eplMerge = "on PopulateEvent merge varaggPWD " +
                "when not matched then insert " +
                "select key, ts, mb, mbarr, me, mearr";
            env.compileDeploy(soda, eplMerge, path);

            env.milestoneInc(milestone);

            Object[] event = makePopulateEvent();
            env.sendEventObjectArray(event, "PopulateEvent");
            env.sendEventBean(new SupportBean_S0(0, "E1"));
            EventBean output = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(output, "c0,c1,c3".split(","),
                new Object[]{55, "x", "p0value"});
            assertEquals(1, ((Collection) output.get("c2")).size());
            assertEquals("[0_p0, 1_p0]", output.get("c4").toString());

            env.undeployAll();
        }
    }

    private static class InfraNestedDotMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionNestedDotMethod(env, true, false, milestone);
            tryAssertionNestedDotMethod(env, false, false, milestone);
            tryAssertionNestedDotMethod(env, true, true, milestone);
            tryAssertionNestedDotMethod(env, false, true, milestone);
        }
    }

    private static void tryAssertionNestedDotMethod(RegressionEnvironment env, boolean grouped, boolean soda, AtomicInteger milestone) {

        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggNDM (" +
            (grouped ? "key string primary key, " : "") +
            "windowSupportBean window(*) @type('SupportBean'))";
        env.compileDeploy(soda, eplDeclare, path);

        String eplInto = "into table varaggNDM " +
            "select window(*) as windowSupportBean from SupportBean#length(2)" +
            (grouped ? " group by theString" : "");
        env.compileDeploy(soda, eplInto, path);

        String key = grouped ? "[\"E1\"]" : "";
        String eplSelect = "@name('s0') select " +
            "varaggNDM" + key + ".windowSupportBean.last(*).intPrimitive as c0, " +
            "varaggNDM" + key + ".windowSupportBean.window(*).countOf() as c1, " +
            "varaggNDM" + key + ".windowSupportBean.window(intPrimitive).take(1) as c2" +
            " from SupportBean_S0";
        env.compileDeploy(soda, eplSelect, path).addListener("s0");
        Object[][] expectedAggType = new Object[][]{{"c0", Integer.class}, {"c1", Integer.class}, {"c2", Collection.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1,c2".split(",");
        makeSendBean(env, "E1", 10, 0);
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 1, Collections.singletonList(10)});

        makeSendBean(env, "E1", 20, 0);
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 2, Collections.singletonList(10)});

        env.milestoneInc(milestone);

        makeSendBean(env, "E1", 30, 0);
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{30, 2, Collections.singletonList(20)});

        env.undeployAll();
    }

    private static Object[] makePopulateEvent() {
        return new Object[]{
            "E1",
            DateTime.parseDefaultMSec("2002-05-30T09:55:00.000"), // ts
            new MyBean(),   // mb
            new MyBean[]{new MyBean(), new MyBean()},   // mbarr
            new Object[]{"p0value"},   // me
            new Object[][]{{"0_p0"}, {"1_p0"}}    // mearr
        };
    }

    private static void runAggregationWDatetimeEtc(RegressionEnvironment env, boolean grouped, boolean soda, AtomicInteger milestone) {

        RegressionPath path = new RegressionPath();
        String eplDeclare = "create table varaggWDE (" + (grouped ? "key string primary key, " : "") +
            "a1 lastever(long), a2 window(*) @type('SupportBean'))";
        env.compileDeploy(soda, eplDeclare, path);

        String eplInto = "@name('into') into table varaggWDE " +
            "select lastever(longPrimitive) as a1, window(*) as a2 from SupportBean#time(10 seconds)" +
            (grouped ? " group by theString" : "");
        env.compileDeploy(soda, eplInto, path);
        Object[][] expectedAggType = new Object[][]{{"a1", Long.class}, {"a2", SupportBean[].class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, env.statement("into").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String key = grouped ? "[\"E1\"]" : "";
        String eplGet = "@name('s0') select varaggWDE" + key + ".a1.after(150L) as c0, " +
            "varaggWDE" + key + ".a2.countOf() as c1 from SupportBean_S0";
        env.compileDeploy(soda, eplGet, path).addListener("s0");
        Object[][] expectedGetType = new Object[][]{{"c0", Boolean.class}, {"c1", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedGetType, env.statement("s0").getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        String[] fields = "c0,c1".split(",");
        makeSendBean(env, "E1", 10, 100);
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, 1});

        env.milestoneInc(milestone);

        makeSendBean(env, "E1", 20, 200);
        env.sendEventBean(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, 2});

        env.undeployAll();
    }

    private static void makeSendBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    public static class MyBean {
        public String getMyProperty() {
            return "x";
        }
    }
}
