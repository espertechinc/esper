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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class EPLInsertIntoPopulateEventTypeColumnNonBean {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoColNonBeanFromSubquerySingle("objectarray", false));
        execs.add(new EPLInsertIntoColNonBeanFromSubquerySingle("objectarray", true));
        execs.add(new EPLInsertIntoColNonBeanFromSubquerySingle("map", false));
        execs.add(new EPLInsertIntoColNonBeanFromSubquerySingle("map", true));
        execs.add(new EPLInsertIntoColNonBeanFromSubqueryMulti("objectarray"));
        execs.add(new EPLInsertIntoColNonBeanFromSubqueryMulti("map"));
        execs.add(new EPLInsertIntoColNonBeanFromSubqueryMultiFilter("objectarray"));
        execs.add(new EPLInsertIntoColNonBeanFromSubqueryMultiFilter("map"));
        execs.add(new EPLInsertIntoColNonBeanNewOperatorDocSample("objectarray"));
        execs.add(new EPLInsertIntoColNonBeanNewOperatorDocSample("map"));
        execs.add(new EPLInsertIntoColNonBeanCaseNew(EventRepresentationChoice.MAP));
        execs.add(new EPLInsertIntoColNonBeanCaseNew(EventRepresentationChoice.OBJECTARRAY));
        execs.add(new EPLInsertIntoColNonBeanCaseNew(EventRepresentationChoice.JSON));
        execs.add(new EPLInsertIntoColNonBeanSingleColNamedWindow());
        execs.add(new EPLInsertIntoColNonBeanSingleToMulti());
        execs.add(new EPLInsertIntoColNonBeanBeanInvalid());
        return execs;
    }

    private static class EPLInsertIntoColNonBeanSingleToMulti implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create schema EventA(id string);\n" +
                    "select * from EventA#keepall;\n" +
                    "@public create schema EventB(aArray EventA[]);\n" +
                    "insert into EventB select maxby(id) as aArray from EventA;\n" +
                    "@name('s0') select * from EventB#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            Map<String, Object> aOne = Collections.singletonMap("id", "x1");
            env.sendEventMap(aOne, "EventA");
            env.assertEventNew("s0", event -> {
                EventBean[] events = (EventBean[]) event.get("aArray");
                assertEquals(1, events.length);
                assertSame(aOne, events[0].getUnderlying());
            });

            env.milestone(0);

            env.assertPropsPerRowIterator("s0", "aArray[0].id".split(","), new Object[][] {{"x1"}});

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoColNonBeanFromSubqueryMultiFilter implements RegressionExecution {
        private final String typeType;

        public EPLInsertIntoColNonBeanFromSubqueryMultiFilter(String typeType) {
            this.typeType = typeType;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
            env.compileDeploy("@public create " + typeType + " schema EventOne(ez EventZero[])", path);

            String[] fields = "e0_0".split(",");
            String epl = "@name('s0') insert into EventOne select " +
                "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall where id between 10 and 20) as ez " +
                "from SupportBean;\n" +
                "@name('s1') select * from EventOne#keepall";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> EPAssertionUtil.assertPropsPerRow((EventBean[]) event.get("ez"), fields, null));

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(10, "x2"));
            env.sendEventBean(new SupportBean_S0(20, "x3"));
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertEventNew("s0", event -> EPAssertionUtil.assertPropsPerRow((EventBean[]) event.get("ez"), fields, new Object[][]{{"x2"}, {"x3"}}));

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "typeType='" + typeType + '\'' +
                '}';
        }
    }

    private static class EPLInsertIntoColNonBeanNewOperatorDocSample implements RegressionExecution {
        private final String typeType;

        public EPLInsertIntoColNonBeanNewOperatorDocSample(String typeType) {
            this.typeType = typeType;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create " + typeType + " schema Item(name string, price double)", path);
            env.compileDeploy("@public create " + typeType + " schema PurchaseOrder(orderId string, items Item[])", path);
            env.compileDeploy("@public @buseventtype create schema TriggerEvent()", path);
            env.compileDeploy("@name('s0') insert into PurchaseOrder select '001' as orderId, new {name= 'i1', price=10} as items from TriggerEvent", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from PurchaseOrder#keepall", path);

            env.sendEventMap(Collections.emptyMap(), "TriggerEvent");
            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertProps(event, "orderId,items[0].name,items[0].price".split(","), new Object[]{"001", "i1", 10d});

                EventBean[] underlying = (EventBean[]) event.get("items");
                assertEquals(1, underlying.length);
                assertEquals("i1", underlying[0].get("name"));
                assertEquals(10d, underlying[0].get("price"));
            });

            env.milestone(0);

            env.sendEventMap(Collections.emptyMap(), "TriggerEvent");

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "typeType='" + typeType + '\'' +
                '}';
        }
    }

    private static class EPLInsertIntoColNonBeanCaseNew implements RegressionExecution {
        private final EventRepresentationChoice representation;

        public EPLInsertIntoColNonBeanCaseNew(EventRepresentationChoice representation) {
            this.representation = representation;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(representation.getAnnotationTextWJsonProvided(MyLocalJsonProvidedNested.class) + "@public create schema Nested(p0 string, p1 int)", path);
            env.compileDeploy(representation.getAnnotationTextWJsonProvided(MyLocalJsonProvidedOuterType.class) + "@public create schema OuterType(n0 Nested)", path);

            String[] fields = "n0.p0,n0.p1".split(",");
            String epl = "@Name('out') " +
                "expression computeNested {\n" +
                "  sb => case\n" +
                "  when intPrimitive = 1 \n" +
                "    then new { p0 = 'a', p1 = 1}\n" +
                "  else new { p0 = 'b', p1 = 2 }\n" +
                "  end\n" +
                "}\n" +
                "insert into OuterType select computeNested(sb) as n0 from SupportBean as sb;\n" +
                "@name('s1') select * from OuterType#keepall;\n";
            env.compileDeploy(epl, path).addListener("out");

            env.sendEventBean(new SupportBean("E1", 2));
            env.assertPropsNew("out", fields, new Object[]{"b", 2});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 1));
            env.assertPropsNew("out", fields, new Object[]{"a", 1});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                "representation=" + representation +
                '}';
        }
    }

    private static class EPLInsertIntoColNonBeanBeanInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema N1_1(p0 int)", path);
            env.compileDeploy("@public create schema N1_2(p1 N1_1)", path);

            // typable - selected column type is incompatible
            env.tryInvalidCompile(path, "insert into N1_2 select new {p0='a'} as p1 from SupportBean",
                "Invalid assignment of column 'p0' of type 'String' to event property 'p0' typed as 'Integer', column and parameter types mismatch");

            // typable - selected column type is not matching anything
            env.tryInvalidCompile(path, "insert into N1_2 select new {xxx='a'} as p1 from SupportBean",
                "Failed to find property 'xxx' among properties for target event type 'N1_1'");

            env.undeployAll();
        }
    }

    public static class EPLInsertIntoColNonBeanSingleColNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public @buseventtype create schema AEvent (symbol string)", path);

            env.compileDeploy("@public create window MyEventWindow#lastevent (e AEvent)", path);
            env.compileDeploy("insert into MyEventWindow select (select * from AEvent#lastevent) as e from SupportBean(theString = 'A')", path);
            env.compileDeploy("@public create schema BEvent (e AEvent)", path);
            env.compileDeploy("@name('s0') insert into BEvent select (select e from MyEventWindow) as e from SupportBean(theString = 'B')", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from BEvent#keepall", path);

            env.sendEventMap(Collections.singletonMap("symbol", "GE"), "AEvent");
            env.sendEventBean(new SupportBean("A", 1));

            env.milestone(0);

            env.sendEventBean(new SupportBean("B", 2));
            env.assertEventNew("s0", result -> {
                EventBean fragment = (EventBean) result.get("e");
                assertEquals("AEvent", fragment.getEventType().getName());
                assertEquals("GE", fragment.get("symbol"));
            });

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoColNonBeanFromSubquerySingle implements RegressionExecution {
        private final String typeType;
        private final boolean filter;

        public EPLInsertIntoColNonBeanFromSubquerySingle(String typeType, boolean filter) {
            this.typeType = typeType;
            this.filter = filter;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
            env.compileDeploy("@public create " + typeType + " schema EventOne(ez EventZero)", path);

            String[] fields = "ez.e0_0,ez.e0_1".split(",");
            String epl = "@name('s0') insert into EventOne select " +
                "(select p00 as e0_0, p01 as e0_1 from SupportBean_S0#lastevent" +
                (filter ? " where id >= 100" : "") + ") as ez " +
                "from SupportBean;\n" +
                "@name('s1') select * from EventOne#keepall;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
            env.sendEventBean(new SupportBean("E1", 1));
            Object[] expected = filter ? new Object[]{null, null} : new Object[]{"x1", "y1"};
            env.assertPropsNew("s0", fields, expected);

            env.sendEventBean(new SupportBean_S0(100, "x2", "y2"));
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertPropsNew("s0", fields, new Object[]{"x2", "y2"});

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2, "x3", "y3"));
            env.sendEventBean(new SupportBean("E3", 3));
            expected = filter ? new Object[]{null, null} : new Object[]{"x3", "y3"};
            env.assertPropsNew("s0", fields, expected);
            if (!filter) {
                env.assertPropsPerRowIterator("s1", "ez.e0_0".split(","), new Object[][]{{"x1"}, {"x2"}, {"x3"}});
            }

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName()  + "{" +
                "typeType='" + typeType + '\'' +
                ", filter=" + filter +
                '}';
        }
    }

    private static class EPLInsertIntoColNonBeanFromSubqueryMulti implements RegressionExecution {
        private final String typeType;

        public EPLInsertIntoColNonBeanFromSubqueryMulti(String typeType) {
            this.typeType = typeType;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create " + typeType + " schema EventZero(e0_0 string, e0_1 string)", path);
            env.compileDeploy("@public create " + typeType + " schema EventOne(e1_0 string, ez EventZero[])", path);

            String[] fields = "e1_0,ez[0].e0_0,ez[0].e0_1,ez[1].e0_0,ez[1].e0_1".split(",");
            String epl = "@name('s0')" +
                "expression thequery {" +
                "  (select p00 as e0_0, p01 as e0_1 from SupportBean_S0#keepall)" +
                "} " +
                "insert into EventOne select theString as e1_0, thequery() as ez from SupportBean;\n" +
                "@name('s1') select * from EventOne#keepall;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "x1", "y1"));
            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertProps(event, fields, new Object[]{"E1", "x1", "y1", null, null});
                SupportEventTypeAssertionUtil.assertConsistency(event);
            });

            env.milestone(0);

            env.sendEventBean(new SupportBean_S0(2, "x2", "y2"));
            env.sendEventBean(new SupportBean("E2", 2));
            env.assertPropsNew("s0", fields, new Object[]{"E2", "x1", "y1", "x2", "y2"});
            env.assertPropsPerRowIterator("s1", "ez[0].e0_0,ez[1].e0_0".split(","), new Object[][]{{"x1", null}, {"x1", "x2"}});

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{type=" + typeType + "}";
        }
    }

    public static class MyLocalJsonProvidedNested implements Serializable {
        private static final long serialVersionUID = 7686625266568140928L;
        public String p0;
        public int p1;
    }

    public static class MyLocalJsonProvidedOuterType implements Serializable {
        private static final long serialVersionUID = 9111321466824957997L;
        public MyLocalJsonProvidedNested n0;
    }

}
