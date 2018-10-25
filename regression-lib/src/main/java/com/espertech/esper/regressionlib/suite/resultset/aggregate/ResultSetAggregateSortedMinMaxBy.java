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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;

public class ResultSetAggregateSortedMinMaxBy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateGroupedSortedMinMax());
        execs.add(new ResultSetAggregateMultipleOverlappingCategories());
        execs.add(new ResultSetAggregateMinByMaxByOverWindow());
        execs.add(new ResultSetAggregateNoAlias());
        execs.add(new ResultSetAggregateMultipleCriteriaSimple());
        execs.add(new ResultSetAggregateMultipleCriteria());
        execs.add(new ResultSetAggregateNoDataWindow());
        execs.add(new ResultSetAggregateInvalid());
        return execs;
    }

    public static class ResultSetAggregateMultipleCriteriaSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sorted(theString desc, intPrimitive desc) as c0 from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("C", 10));
            assertExpected(env, new Object[][]{{"C", 10}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("D", 20));
            assertExpected(env, new Object[][]{{"D", 20}, {"C", 10}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("C", 15));
            assertExpected(env, new Object[][]{{"D", 20}, {"C", 15}, {"C", 10}});

            env.milestone(2);

            env.sendEventBean(new SupportBean("D", 19));
            assertExpected(env, new Object[][]{{"D", 20}, {"D", 19}, {"C", 15}, {"C", 10}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateGroupedSortedMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select " +
                "window(*) as c0, " +
                "sorted(intPrimitive desc) as c1, " +
                "sorted(intPrimitive asc) as c2, " +
                "maxby(intPrimitive) as c3, " +
                "minby(intPrimitive) as c4, " +
                "maxbyever(intPrimitive) as c5, " +
                "minbyever(intPrimitive) as c6 " +
                "from SupportBean#groupwin(longPrimitive)#length(3) " +
                "group by longPrimitive";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionGroupedSortedMinMax(env, milestone);

            env.undeployAll();

            // test SODA
            env.eplToModelCompileDeploy(epl).addListener("s0");
            tryAssertionGroupedSortedMinMax(env, milestone);
            env.undeployAll();

            // test join
            String eplJoin = "@name('s0') select " +
                "window(sb.*) as c0, " +
                "sorted(intPrimitive desc) as c1, " +
                "sorted(intPrimitive asc) as c2, " +
                "maxby(intPrimitive) as c3, " +
                "minby(intPrimitive) as c4, " +
                "maxbyever(intPrimitive) as c5, " +
                "minbyever(intPrimitive) as c6 " +
                "from SupportBean_S0#lastevent, SupportBean#groupwin(longPrimitive)#length(3) as sb " +
                "group by longPrimitive";
            env.compileDeploy(eplJoin).addListener("s0");
            env.sendEventBean(new SupportBean_S0(1, "p00"));
            tryAssertionGroupedSortedMinMax(env, milestone);
            env.undeployAll();

            // test join multirow
            String[] fields = "c0".split(",");
            String joinMultirow = "@name('s0') select sorted(intPrimitive desc) as c0 from SupportBean_S0#keepall, SupportBean#length(2)";
            env.compileDeploy(joinMultirow).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "S1"));
            env.sendEventBean(new SupportBean_S0(2, "S2"));
            env.sendEventBean(new SupportBean_S0(3, "S3"));

            env.milestoneInc(milestone);

            SupportBean eventOne = new SupportBean("E1", 1);
            env.sendEventBean(eventOne);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventOne}});

            env.milestoneInc(milestone);

            SupportBean eventTwo = new SupportBean("E2", 2);
            env.sendEventBean(eventTwo);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventTwo, eventOne}});

            env.milestoneInc(milestone);

            SupportBean eventThree = new SupportBean("E3", 0);
            env.sendEventBean(eventThree);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{new Object[]{eventTwo, eventThree}});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMinByMaxByOverWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9".split(",");
            String epl = "@name('s0') select " +
                "maxbyever(longPrimitive) as c0, " +
                "minbyever(longPrimitive) as c1, " +
                "maxby(longPrimitive).longPrimitive as c2, " +
                "maxby(longPrimitive).theString as c3, " +
                "maxby(longPrimitive).intPrimitive as c4, " +
                "maxby(longPrimitive) as c5, " +
                "minby(longPrimitive).longPrimitive as c6, " +
                "minby(longPrimitive).theString as c7, " +
                "minby(longPrimitive).intPrimitive as c8, " +
                "minby(longPrimitive) as c9 " +
                "from SupportBean#length(5)";
            env.compileDeploy(epl).addListener("s0");

            SupportBean eventOne = makeEvent("E1", 1, 10);
            env.sendEventBean(eventOne);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{eventOne, eventOne, 10L, "E1", 1, eventOne, 10L, "E1", 1, eventOne});

            SupportBean eventTwo = makeEvent("E2", 2, 20);
            env.sendEventBean(eventTwo);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{eventTwo, eventOne, 20L, "E2", 2, eventTwo, 10L, "E1", 1, eventOne});

            env.milestone(0);

            SupportBean eventThree = makeEvent("E3", 3, 5);
            env.sendEventBean(eventThree);
            Object[] resultThree = new Object[]{eventTwo, eventThree, 20L, "E2", 2, eventTwo, 5L, "E3", 3, eventThree};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultThree);

            SupportBean eventFour = makeEvent("E4", 4, 5);
            env.sendEventBean(eventFour); // same as E3
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultThree);

            env.milestone(1);

            SupportBean eventFive = makeEvent("E5", 5, 20);
            env.sendEventBean(eventFive); // same as E2
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultThree);

            SupportBean eventSix = makeEvent("E6", 6, 10);
            env.sendEventBean(eventSix); // expires E1
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultThree);

            SupportBean eventSeven = makeEvent("E7", 7, 20);
            env.sendEventBean(eventSeven); // expires E2
            Object[] resultSeven = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 5L, "E3", 3, eventThree};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultSeven);

            env.milestone(2);

            env.sendEventBean(makeEvent("E8", 8, 20)); // expires E3
            Object[] resultEight = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 5L, "E4", 4, eventFour};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultEight);

            env.sendEventBean(makeEvent("E9", 9, 19)); // expires E4
            Object[] resultNine = new Object[]{eventTwo, eventThree, 20L, "E5", 5, eventFive, 10L, "E6", 6, eventSix};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultNine);

            env.sendEventBean(makeEvent("E10", 10, 12)); // expires E5
            Object[] resultTen = new Object[]{eventTwo, eventThree, 20L, "E7", 7, eventSeven, 10L, "E6", 6, eventSix};
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, resultTen);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "maxby(intPrimitive).theString, " +
                "minby(intPrimitive)," +
                "maxbyever(intPrimitive).theString, " +
                "minbyever(intPrimitive)," +
                "sorted(intPrimitive asc, theString desc)" +
                " from SupportBean#time(10)";
            env.compileDeploy(epl).addListener("s0");

            EventPropertyDescriptor[] props = env.statement("s0").getEventType().getPropertyDescriptors();
            assertEquals("maxby(intPrimitive).theString()", props[0].getPropertyName());
            assertEquals("minby(intPrimitive)", props[1].getPropertyName());
            assertEquals("maxbyever(intPrimitive).theString()", props[2].getPropertyName());
            assertEquals("minbyever(intPrimitive)", props[3].getPropertyName());
            assertEquals("sorted(intPrimitive,theString desc)", props[4].getPropertyName());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMultipleOverlappingCategories implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            String epl = "@name('s0') select " +
                "maxbyever(intPrimitive).longPrimitive as c0," +
                "maxbyever(theString).longPrimitive as c1," +
                "minbyever(intPrimitive).longPrimitive as c2," +
                "minbyever(theString).longPrimitive as c3," +
                "maxby(intPrimitive).longPrimitive as c4," +
                "maxby(theString).longPrimitive as c5," +
                "minby(intPrimitive).longPrimitive as c6," +
                "minby(theString).longPrimitive as c7 " +
                "from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("C", 10, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L});

            env.sendEventBean(makeEvent("P", 5, 2L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

            env.milestone(0);

            env.sendEventBean(makeEvent("G", 7, 3L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

            env.sendEventBean(makeEvent("A", 7, 4L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 2L, 4L, 1L, 2L, 2L, 4L});

            env.milestone(1);

            env.sendEventBean(makeEvent("G", 1, 5L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 2L, 5L, 4L, 1L, 2L, 5L, 4L});

            env.sendEventBean(makeEvent("X", 7, 6L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{1L, 6L, 5L, 4L, 1L, 6L, 5L, 4L});

            env.milestone(2);

            env.sendEventBean(makeEvent("G", 100, 7L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{7L, 6L, 5L, 4L, 7L, 6L, 5L, 4L});

            env.sendEventBean(makeEvent("Z", 1000, 8L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
                new Object[]{8L, 8L, 5L, 4L, 8L, 8L, 5L, 4L});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateMultipleCriteria implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String epl;

            // test sorted multiple criteria
            String[] fields = "c0,c1,c2,c3".split(",");
            epl = "@name('s0') select " +
                "sorted(theString desc, intPrimitive desc) as c0," +
                "sorted(theString, intPrimitive) as c1," +
                "sorted(theString asc, intPrimitive asc) as c2," +
                "sorted(theString desc, intPrimitive asc) as c3 " +
                "from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            SupportBean eventOne = new SupportBean("C", 10);
            env.sendEventBean(eventOne);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[][]{
                new Object[]{eventOne},
                new Object[]{eventOne},
                new Object[]{eventOne},
                new Object[]{eventOne}});

            env.milestoneInc(milestone);

            SupportBean eventTwo = new SupportBean("D", 20);
            env.sendEventBean(eventTwo);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[][]{
                new Object[]{eventTwo, eventOne},
                new Object[]{eventOne, eventTwo},
                new Object[]{eventOne, eventTwo},
                new Object[]{eventTwo, eventOne}});

            SupportBean eventThree = new SupportBean("C", 15);
            env.sendEventBean(eventThree);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[][]{
                new Object[]{eventTwo, eventThree, eventOne},
                new Object[]{eventOne, eventThree, eventTwo},
                new Object[]{eventOne, eventThree, eventTwo},
                new Object[]{eventTwo, eventOne, eventThree}});

            env.milestoneInc(milestone);

            SupportBean eventFour = new SupportBean("D", 19);
            env.sendEventBean(eventFour);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[][]{
                new Object[]{eventTwo, eventFour, eventThree, eventOne},
                new Object[]{eventOne, eventThree, eventFour, eventTwo},
                new Object[]{eventOne, eventThree, eventFour, eventTwo},
                new Object[]{eventFour, eventTwo, eventOne, eventThree}});

            env.undeployAll();

            // test min/max
            String[] fieldsTwo = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");
            epl = "@name('s0') select " +
                "maxbyever(intPrimitive, theString).longPrimitive as c0," +
                "minbyever(intPrimitive, theString).longPrimitive as c1," +
                "maxbyever(theString, intPrimitive).longPrimitive as c2," +
                "minbyever(theString, intPrimitive).longPrimitive as c3," +
                "maxby(intPrimitive, theString).longPrimitive as c4," +
                "minby(intPrimitive, theString).longPrimitive as c5," +
                "maxby(theString, intPrimitive).longPrimitive as c6," +
                "minby(theString, intPrimitive).longPrimitive as c7 " +
                "from SupportBean#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeEvent("C", 10, 1L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L});

            env.sendEventBean(makeEvent("P", 5, 2L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("C", 9, 3L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{1L, 2L, 2L, 3L, 1L, 2L, 2L, 3L});

            env.sendEventBean(makeEvent("C", 11, 4L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{4L, 2L, 2L, 3L, 4L, 2L, 2L, 3L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeEvent("X", 11, 5L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{5L, 2L, 5L, 3L, 5L, 2L, 5L, 3L});

            env.sendEventBean(makeEvent("X", 0, 6L));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsTwo,
                new Object[]{5L, 6L, 5L, 3L, 5L, 6L, 5L, 3L});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateNoDataWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "maxbyever(intPrimitive).theString as c0, " +
                "minbyever(intPrimitive).theString as c1, " +
                "maxby(intPrimitive).theString as c2, " +
                "minby(intPrimitive).theString as c3 " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", "E1", "E1"});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E1", "E2", "E1"});

            env.sendEventBean(new SupportBean("E3", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E3", "E2", "E3"});

            env.sendEventBean(new SupportBean("E4", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4", "E3", "E4", "E3"});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select maxBy(p00||p10) from SupportBean_S0#lastevent, SupportBean_S1#lastevent",
                "Failed to validate select-clause expression 'maxby(p00||p10)': The 'maxby' aggregation function requires that any parameter expressions evaluate properties of the same stream");

            tryInvalidCompile(env, "select sorted(p00) from SupportBean_S0",
                "Failed to validate select-clause expression 'sorted(p00)': The 'sorted' aggregation function requires that a data window is declared for the stream");
        }
    }

    private static void tryAssertionGroupedSortedMinMax(RegressionEnvironment env, AtomicInteger milestone) {

        String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
        SupportBean eventOne = makeEvent("E1", 1, 1);
        env.sendEventBean(eventOne);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventOne},
                new Object[]{eventOne},
                new Object[]{eventOne},
                eventOne, eventOne, eventOne, eventOne});

        env.milestoneInc(milestone);

        SupportBean eventTwo = makeEvent("E2", 2, 1);
        env.sendEventBean(eventTwo);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventOne, eventTwo},
                new Object[]{eventTwo, eventOne},
                new Object[]{eventOne, eventTwo},
                eventTwo, eventOne, eventTwo, eventOne});

        env.milestoneInc(milestone);

        SupportBean eventThree = makeEvent("E3", 0, 1);
        env.sendEventBean(eventThree);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventOne, eventTwo, eventThree},
                new Object[]{eventTwo, eventOne, eventThree},
                new Object[]{eventThree, eventOne, eventTwo},
                eventTwo, eventThree, eventTwo, eventThree});

        env.milestoneInc(milestone);

        SupportBean eventFour = makeEvent("E4", 3, 1);   // pushes out E1
        env.sendEventBean(eventFour);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventTwo, eventThree, eventFour},
                new Object[]{eventFour, eventTwo, eventThree},
                new Object[]{eventThree, eventTwo, eventFour},
                eventFour, eventThree, eventFour, eventThree});

        SupportBean eventFive = makeEvent("E5", -1, 2);   // group 2
        env.sendEventBean(eventFive);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventFive},
                new Object[]{eventFive},
                new Object[]{eventFive},
                eventFive, eventFive, eventFive, eventFive});

        SupportBean eventSix = makeEvent("E6", -1, 1);   // pushes out E2
        env.sendEventBean(eventSix);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventThree, eventFour, eventSix},
                new Object[]{eventFour, eventThree, eventSix},
                new Object[]{eventSix, eventThree, eventFour},
                eventFour, eventSix, eventFour, eventSix});

        env.milestoneInc(milestone);

        SupportBean eventSeven = makeEvent("E7", 2, 2);   // group 2
        env.sendEventBean(eventSeven);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields,
            new Object[]{
                new Object[]{eventFive, eventSeven},
                new Object[]{eventSeven, eventFive},
                new Object[]{eventFive, eventSeven},
                eventSeven, eventFive, eventSeven, eventFive});

    }

    private static SupportBean makeEvent(String string, int intPrimitive, long longPrimitive) {
        SupportBean event = new SupportBean(string, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        return event;
    }

    private static void assertExpected(RegressionEnvironment env, Object[][] expected) {
        SupportBean[] und = (SupportBean[]) env.listener("s0").assertOneGetNewAndReset().get("c0");
        for (int i = 0; i < und.length; i++) {
            assertEquals(expected[i][0], und[i].getTheString());
            assertEquals(expected[i][1], und[i].getIntPrimitive());
        }
    }
}

