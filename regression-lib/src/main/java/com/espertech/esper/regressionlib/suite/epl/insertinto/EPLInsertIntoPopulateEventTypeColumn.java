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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EPLInsertIntoPopulateEventTypeColumn {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoTypableSubquery());
        execs.add(new EPLInsertIntoTypableNewOperatorDocSample());
        execs.add(new EPLInsertIntoTypableAndCaseNew(EventRepresentationChoice.MAP));
        execs.add(new EPLInsertIntoTypableAndCaseNew(EventRepresentationChoice.OBJECTARRAY));
        execs.add(new EPLInsertIntoTypableAndCaseNew(EventRepresentationChoice.JSON));
        execs.add(new EPLInsertIntoInvalid());
        execs.add(new EPLInsertIntoEnumerationSubquery());
        return execs;
    }

    private static class EPLInsertIntoTypableSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionTypableSubqueryMulti(env, "objectarray");
            tryAssertionTypableSubqueryMulti(env, "map");

            tryAssertionTypableSubquerySingleMayFilter(env, "objectarray", true);
            tryAssertionTypableSubquerySingleMayFilter(env, "map", true);

            tryAssertionTypableSubquerySingleMayFilter(env, "objectarray", false);
            tryAssertionTypableSubquerySingleMayFilter(env, "map", false);

            tryAssertionTypableSubqueryMultiFilter(env, "objectarray");
            tryAssertionTypableSubqueryMultiFilter(env, "map");
        }
    }

    private static class EPLInsertIntoEnumerationSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionEnumerationSubqueryMultiMayFilter(env, "objectarray", true);
            tryAssertionEnumerationSubqueryMultiMayFilter(env, "map", true);

            tryAssertionEnumerationSubqueryMultiMayFilter(env, "objectarray", false);
            tryAssertionEnumerationSubqueryMultiMayFilter(env, "map", false);

            tryAssertionEnumerationSubquerySingleMayFilter(env, "objectarray", true);
            tryAssertionEnumerationSubquerySingleMayFilter(env, "map", true);

            tryAssertionEnumerationSubquerySingleMayFilter(env, "objectarray", false);
            tryAssertionEnumerationSubquerySingleMayFilter(env, "map", false);

            tryAssertionFragmentSingeColNamedWindow(env);
        }
    }

    private static class EPLInsertIntoTypableNewOperatorDocSample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionTypableNewOperatorDocSample(env, "objectarray");
            tryAssertionTypableNewOperatorDocSample(env, "map");
        }
    }

    private static class EPLInsertIntoTypableAndCaseNew implements RegressionExecution {
        private final EventRepresentationChoice representation;

        public EPLInsertIntoTypableAndCaseNew(EventRepresentationChoice representation) {
            this.representation = representation;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(representation.getAnnotationTextWJsonProvided(MyLocalJsonProvidedNested.class) + "create schema Nested(p0 string, p1 int)", path);
            env.compileDeploy(representation.getAnnotationTextWJsonProvided(MyLocalJsonProvidedOuterType.class) + "create schema OuterType(n0 Nested)", path);

            String[] fields = "n0.p0,n0.p1".split(",");
            env.compileDeploy("@Name('out') " +
                "expression computeNested {\n" +
                "  sb => case\n" +
                "  when intPrimitive = 1 \n" +
                "    then new { p0 = 'a', p1 = 1}\n" +
                "  else new { p0 = 'b', p1 = 2 }\n" +
                "  end\n" +
                "}\n" +
                "insert into OuterType select computeNested(sb) as n0 from SupportBean as sb", path).addListener("out");

            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("out").assertOneGetNewAndReset(), fields, new Object[]{"b", 2});

            env.sendEventBean(new SupportBean("E2", 1));
            EPAssertionUtil.assertProps(env.listener("out").assertOneGetNewAndReset(), fields, new Object[]{"a", 1});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema N1_1(p0 int)", path);
            env.compileDeploy("create schema N1_2(p1 N1_1)", path);

            // enumeration type is incompatible
            env.compileDeploy("create schema TypeOne(sbs SupportBean[])", path);
            tryInvalidCompile(env, path, "insert into TypeOne select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Incompatible type detected attempting to insert into column 'sbs' type '" + SupportBean.class.getName() + "' compared to selected type 'SupportBean_S0'");

            env.compileDeploy("create schema TypeTwo(sbs SupportBean)", path);
            tryInvalidCompile(env, path, "insert into TypeTwo select (select * from SupportBean_S0#keepall) as sbs from SupportBean_S1",
                "Incompatible type detected attempting to insert into column 'sbs' type '" + SupportBean.class.getName() + "' compared to selected type 'SupportBean_S0'");

            // typable - selected column type is incompatible
            tryInvalidCompile(env, path, "insert into N1_2 select new {p0='a'} as p1 from SupportBean",
                "Invalid assignment of column 'p0' of type 'java.lang.String' to event property 'p0' typed as 'java.lang.Integer', column and parameter types mismatch");

            // typable - selected column type is not matching anything
            tryInvalidCompile(env, path, "insert into N1_2 select new {xxx='a'} as p1 from SupportBean",
                "Failed to find property 'xxx' among properties for target event type 'N1_1'");

            env.undeployAll();
        }
    }

    private static void tryAssertionFragmentSingeColNamedWindow(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType("create schema AEvent (symbol string)", path);

        env.compileDeploy("create window MyEventWindow#lastevent (e AEvent)", path);
        env.compileDeploy("insert into MyEventWindow select (select * from AEvent#lastevent) as e from SupportBean(theString = 'A')", path);
        env.compileDeploy("create schema BEvent (e AEvent)", path);
        env.compileDeploy("@name('s0') insert into BEvent select (select e from MyEventWindow) as e from SupportBean(theString = 'B')", path).addListener("s0");

        env.sendEventMap(Collections.singletonMap("symbol", "GE"), "AEvent");
        env.sendEventBean(new SupportBean("A", 1));
        env.sendEventBean(new SupportBean("B", 2));
        EventBean result = env.listener("s0").assertOneGetNewAndReset();
        EventBean fragment = (EventBean) result.get("e");
        assertEquals("AEvent", fragment.getEventType().getName());
        assertEquals("GE", fragment.get("symbol"));

        env.undeployAll();
    }

    private static void tryAssertionTypableSubquerySingleMayFilter(RegressionEnvironment env, String typeType, boolean filter) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
        env.compileDeploy("create " + typeType + " schema EventOne(ez EventZero)", path);

        String[] fields = "ez.e0_0,ez.e0_1".split(",");
        env.compileDeploy("@name('s0') insert into EventOne select " +
            "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#lastevent" +
            (filter ? " where id >= 100" : "") + ") as ez " +
            "from SupportBean", path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
        env.sendEventBean(new SupportBean("E1", 1));
        Object[] expected = filter ? new Object[]{null, null} : new Object[]{"x1", "y1"};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

        env.sendEventBean(new SupportBean_S0(100, "x2", "y2"));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"x2", "y2"});

        env.sendEventBean(new SupportBean_S0(2, "x3", "y3"));
        env.sendEventBean(new SupportBean("E3", 3));
        expected = filter ? new Object[]{null, null} : new Object[]{"x3", "y3"};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

        env.undeployAll();
    }

    private static void tryAssertionTypableSubqueryMulti(RegressionEnvironment env, String typeType) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
        env.compileDeploy("create " + typeType + " schema EventOne(e1_0 string, ez EventZero[])", path);

        String[] fields = "e1_0,ez[0].e0_0,ez[0].e0_1,ez[1].e0_0,ez[1].e0_1".split(",");
        env.compileDeploy("@name('s0')" +
            "expression thequery {" +
            "  (select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall)" +
            "} " +
            "insert into EventOne select " +
            "theString as e1_0, " +
            "thequery() as ez " +
            "from SupportBean", path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
        env.sendEventBean(new SupportBean("E1", 1));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{"E1", "x1", "y1", null, null});
        SupportEventTypeAssertionUtil.assertConsistency(event);

        env.sendEventBean(new SupportBean_S0(2, "x2", "y2"));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "x1", "y1", "x2", "y2"});

        env.undeployAll();
    }

    private static void tryAssertionTypableSubqueryMultiFilter(RegressionEnvironment env, String typeType) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
        env.compileDeploy("create " + typeType + " schema EventOne(ez EventZero[])", path);

        String[] fields = "e0_0".split(",");
        env.compileDeploy("@name('s0') insert into EventOne select " +
            "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall where id between 10 and 20) as ez " +
            "from SupportBean", path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
        env.sendEventBean(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow((EventBean[]) env.listener("s0").assertOneGetNewAndReset().get("ez"), fields, null);

        env.sendEventBean(new SupportBean_S0(10, "x2"));
        env.sendEventBean(new SupportBean_S0(20, "x3"));
        env.sendEventBean(new SupportBean("E2", 2));
        EPAssertionUtil.assertPropsPerRow((EventBean[]) env.listener("s0").assertOneGetNewAndReset().get("ez"), fields, new Object[][]{{"x2"}, {"x3"}});

        env.undeployAll();
    }

    private static void tryAssertionEnumerationSubqueryMultiMayFilter(RegressionEnvironment env, String typeType, boolean filter) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema EventOne(sbarr SupportBean_S0[])", path);

        String[] fields = "p00".split(",");
        env.compileDeploy("@name('s0') insert into EventOne select " +
            "(select * from SupportBean_S0#keepall " +
            (filter ? "where 1=1" : "") + ") as sbarr " +
            "from SupportBean", path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "x1"));
        env.sendEventBean(new SupportBean("E1", 1));
        EventBean[] inner = (EventBean[]) env.listener("s0").assertOneGetNewAndReset().get("sbarr");
        EPAssertionUtil.assertPropsPerRow(inner, fields, new Object[][]{{"x1"}});

        env.sendEventBean(new SupportBean_S0(2, "x2", "y2"));
        env.sendEventBean(new SupportBean("E2", 2));
        inner = (EventBean[]) env.listener("s0").assertOneGetNewAndReset().get("sbarr");
        EPAssertionUtil.assertPropsPerRow(inner, fields, new Object[][]{{"x1"}, {"x2"}});

        env.undeployAll();
    }

    private static void tryAssertionEnumerationSubquerySingleMayFilter(RegressionEnvironment env, String typeType, boolean filter) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema EventOne(sb SupportBean_S0)", path);

        String[] fields = "sb.p00".split(",");
        env.compileDeploy("@name('s0') insert into EventOne select " +
            "(select * from SupportBean_S0#length(2) " +
            (filter ? "where id >= 100" : "") + ") as sb " +
            "from SupportBean", path).addListener("s0");

        env.sendEventBean(new SupportBean_S0(1, "x1"));
        env.sendEventBean(new SupportBean("E1", 1));
        Object[] expected = filter ? new Object[]{null} : new Object[]{"x1"};
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);

        env.sendEventBean(new SupportBean_S0(100, "x2"));
        env.sendEventBean(new SupportBean("E2", 2));
        String received = (String) env.listener("s0").assertOneGetNewAndReset().get(fields[0]);
        if (filter) {
            assertEquals("x2", received);
        } else {
            assertNull(received); // this should not take the first event and according to SQL standard returns null
        }

        env.undeployAll();
    }

    private static void tryAssertionTypableNewOperatorDocSample(RegressionEnvironment env, String typeType) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create " + typeType + " schema Item(name string, price double)", path);
        env.compileDeploy("create " + typeType + " schema PurchaseOrder(orderId string, items Item[])", path);
        env.compileDeployWBusPublicType("create schema TriggerEvent()", path);
        env.compileDeploy("@name('s0') insert into PurchaseOrder select '001' as orderId, new {name= 'i1', price=10} as items from TriggerEvent", path).addListener("s0");

        env.sendEventMap(Collections.emptyMap(), "TriggerEvent");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "orderId,items[0].name,items[0].price".split(","), new Object[]{"001", "i1", 10d});

        EventBean[] underlying = (EventBean[]) event.get("items");
        assertEquals(1, underlying.length);
        assertEquals("i1", underlying[0].get("name"));
        assertEquals(10d, underlying[0].get("price"));

        env.undeployAll();
    }

    public static class MyLocalJsonProvidedNested implements Serializable {
        public String p0;
        public int p1;
    }

    public static class MyLocalJsonProvidedOuterType implements Serializable {
        public MyLocalJsonProvidedNested n0;
    }

}
