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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class EPLOuterJoin2Stream {
    private final static String[] FIELDS = new String[]{"s0.id", "s0.p00", "s1.id", "s1.p10"};

    private final static SupportBean_S0[] EVENTS_S0;
    private final static SupportBean_S1[] EVENTS_S1;

    static {
        EVENTS_S0 = new SupportBean_S0[15];
        EVENTS_S1 = new SupportBean_S1[15];
        int count = 100;
        for (int i = 0; i < EVENTS_S0.length; i++) {
            EVENTS_S0[i] = new SupportBean_S0(count++, Integer.toString(i));
        }
        count = 200;
        for (int i = 0; i < EVENTS_S1.length; i++) {
            EVENTS_S1[i] = new SupportBean_S1(count++, Integer.toString(i));
        }
    }

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinRangeOuterJoin());
        execs.add(new EPLJoinFullOuterIteratorGroupBy());
        execs.add(new EPLJoinFullOuterJoin());
        execs.add(new EPLJoinMultiColumnLeftOM());
        execs.add(new EPLJoinMultiColumnLeft());
        execs.add(new EPLJoinMultiColumnRight());
        execs.add(new EPLJoinMultiColumnRightCoercion());
        execs.add(new EPLJoinRightOuterJoin());
        execs.add(new EPLJoinLeftOuterJoin());
        execs.add(new EPLJoinEventType());
        execs.add(new EPLJoinFullOuterMultikeyWArrayPrimitive());
        return execs;
    }

    private static class EPLJoinFullOuterMultikeyWArrayPrimitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * " +
                "from SupportEventWithIntArray#keepall one " +
                "full outer join " +
                "SupportEventWithManyArray#keepall two " +
                "on array = intOne";
            env.compileDeploy(epl).addListener("s0");

            sendIntArrayAssert(env, "IA1", new int[] {1, 2}, new Object[][] {{"IA1", null}});
            sendManyArrayAssert(env, "MA1", new int[] {3, 4}, new Object[][] {{null, "MA1"}});
            sendIntArrayAssert(env, "IA2", new int[] {1}, new Object[][] {{"IA2", null}});
            sendManyArrayAssert(env, "MA2", new int[] {2}, new Object[][] {{null, "MA2"}});

            env.milestone(0);

            sendManyArrayAssert(env, "MA3", new int[] {1}, new Object[][] {{"IA2", "MA3"}});
            sendIntArrayAssert(env, "IA3", new int[] {3, 4}, new Object[][] {{"IA3", "MA1"}});
            sendManyArrayAssert(env, "MA4", new int[] {3, 4}, new Object[][] {{"IA3", "MA4"}});
            sendIntArrayAssert(env, "IA4", new int[] {3, 4}, new Object[][] {{"IA4", "MA1"}, {"IA4", "MA4"}});

            env.undeployAll();
        }

        private void sendIntArrayAssert(RegressionEnvironment env, String id, int[] array, Object[][] expected) {
            env.sendEventBean(new SupportEventWithIntArray(id, array));
            assertEvents(env, expected);
        }

        private void sendManyArrayAssert(RegressionEnvironment env, String id, int[] intOne, Object[][] expected) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(intOne));
            assertEvents(env, expected);
        }

        private void assertEvents(RegressionEnvironment env, Object[][] expected) {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "one.id,two.id".split(","), expected);
        }
    }

    private static class EPLJoinRangeOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String stmtOne = "@name('s0') select sb.theString as sbstr, sb.intPrimitive as sbint, sbr.key as sbrk, sbr.rangeStart as sbrs, sbr.rangeEnd as sbre " +
                "from SupportBean#keepall sb " +
                "full outer join " +
                "SupportBeanRange#keepall sbr " +
                "on theString = key " +
                "where intPrimitive between rangeStart and rangeEnd " +
                "order by rangeStart asc, intPrimitive asc";
            tryAssertion(env, stmtOne, milestone);

            String stmtTwo = "@name('s0') select sb.theString as sbstr, sb.intPrimitive as sbint, sbr.key as sbrk, sbr.rangeStart as sbrs, sbr.rangeEnd as sbre " +
                "from SupportBeanRange#keepall sbr " +
                "full outer join " +
                "SupportBean#keepall sb " +
                "on theString = key " +
                "where intPrimitive between rangeStart and rangeEnd " +
                "order by rangeStart asc, intPrimitive asc";
            tryAssertion(env, stmtTwo, milestone);

            String stmtThree = "@name('s0') select sb.theString as sbstr, sb.intPrimitive as sbint, sbr.key as sbrk, sbr.rangeStart as sbrs, sbr.rangeEnd as sbre " +
                "from SupportBeanRange#keepall sbr " +
                "full outer join " +
                "SupportBean#keepall sb " +
                "on theString = key " +
                "where intPrimitive >= rangeStart and intPrimitive <= rangeEnd " +
                "order by rangeStart asc, intPrimitive asc";
            tryAssertion(env, stmtThree, milestone);
        }

        private static void tryAssertion(RegressionEnvironment env, String epl, AtomicInteger milestone) {

            String[] fields = "sbstr,sbint,sbrk,sbrs,sbre".split(",");
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean("K1", 10));
            env.sendEventBean(new SupportBeanRange("R1", "K1", 20, 30));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("K1", 30));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"K1", 30, "K1", 20, 30}});

            env.sendEventBean(new SupportBean("K1", 40));
            env.sendEventBean(new SupportBean("K1", 31));
            env.sendEventBean(new SupportBean("K1", 19));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBeanRange("R2", "K1", 39, 41));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"K1", 40, "K1", 39, 41}});

            env.sendEventBean(new SupportBeanRange("R2", "K1", 38, 40));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"K1", 40, "K1", 38, 40}});

            env.sendEventBean(new SupportBeanRange("R2", "K1", 40, 42));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"K1", 40, "K1", 40, 42}});

            env.sendEventBean(new SupportBeanRange("R2", "K1", 41, 42));
            env.sendEventBean(new SupportBeanRange("R2", "K1", 38, 39));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("K1", 41));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{
                {"K1", 41, "K1", 39, 41}, {"K1", 41, "K1", 40, 42}, {"K1", 41, "K1", 41, 42}});

            env.sendEventBean(new SupportBeanRange("R2", "K1", 35, 42));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]
                {{"K1", 40, "K1", 35, 42}, {"K1", 41, "K1", 35, 42}});

            env.undeployAll();
        }
    }

    private static class EPLJoinFullOuterIteratorGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString, intPrimitive, symbol, volume " +
                "from SupportMarketDataBean#keepall " +
                "full outer join SupportBean#groupwin(theString, intPrimitive)#length(2) " +
                "on theString = symbol " +
                "group by theString, intPrimitive, symbol " +
                "order by theString, intPrimitive, symbol, volume";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEventMD(env, "c0", 200L);
            sendEventMD(env, "c3", 400L);

            sendEvent(env, "c0", 0);
            sendEvent(env, "c0", 1);
            sendEvent(env, "c0", 2);
            sendEvent(env, "c1", 0);
            sendEvent(env, "c1", 1);
            sendEvent(env, "c1", 2);
            sendEvent(env, "c2", 0);
            sendEvent(env, "c2", 1);
            sendEvent(env, "c2", 2);

            Iterator iterator = env.statement("s0").iterator();
            EventBean[] events = EPAssertionUtil.iteratorToArray(iterator);
            assertEquals(10, events.length);

        /* For debugging, comment in
        for (int i = 0; i < events.length; i++)
        {
            System.out.println(
                   "string=" + events[i].get("string") +
                   "  int=" + events[i].get("intPrimitive") +
                   "  symbol=" + events[i].get("symbol") +
                   "  volume="  + events[i].get("volume")
                );
        }
        */

            EPAssertionUtil.assertPropsPerRow(events, "theString,intPrimitive,symbol,volume".split(","),
                new Object[][]{
                    {null, null, "c3", 400L},
                    {"c0", 0, "c0", 200L},
                    {"c0", 1, "c0", 200L},
                    {"c0", 2, "c0", 200L},
                    {"c1", 0, null, null},
                    {"c1", 1, null, null},
                    {"c1", 2, null, null},
                    {"c2", 0, null, null},
                    {"c2", 1, null, null},
                    {"c2", 2, null, null}
                });

            env.undeployAll();
        }
    }

    private static class EPLJoinFullOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "full");

            // Send S0[0]
            sendEvent(EVENTS_S0[0], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), 100, "0", null, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null}});

            // Send S1[1]
            sendEvent(EVENTS_S1[1], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 201, "1");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null},
                    {null, null, 201, "1"}});

            // Send S1[2] and S0[2]
            sendEvent(EVENTS_S1[2], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null},
                    {null, null, 201, "1"},
                    {null, null, 202, "2"}});

            sendEvent(EVENTS_S0[2], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), 102, "2", 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null},
                    {null, null, 201, "1"},
                    {102, "2", 202, "2"}});

            // Send S0[3] and S1[3]
            sendEvent(EVENTS_S0[3], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), 103, "3", null, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null},
                    {null, null, 201, "1"},
                    {102, "2", 202, "2"},
                    {103, "3", null, null}});
            sendEvent(EVENTS_S1[3], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), 103, "3", 203, "3");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{100, "0", null, null},
                    {null, null, 201, "1"},
                    {102, "2", 202, "2"},
                    {103, "3", 203, "3"}});

            // Send S0[4], pushes S0[0] out of window
            sendEvent(EVENTS_S0[4], env);
            EventBean oldEvent = env.listener("s0").getLastOldData()[0];
            EventBean newEvent = env.listener("s0").getLastNewData()[0];
            compareEvent(oldEvent, 100, "0", null, null);
            compareEvent(newEvent, 104, "4", null, null);
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 201, "1"},
                    {102, "2", 202, "2"},
                    {103, "3", 203, "3"},
                    {104, "4", null, null}});

            // Send S1[4]
            sendEvent(EVENTS_S1[4], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), 104, "4", 204, "4");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 201, "1"},
                    {102, "2", 202, "2"},
                    {103, "3", 203, "3"},
                    {104, "4", 204, "4"}});

            // Send S1[5]
            sendEvent(EVENTS_S1[5], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 205, "5");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 201, "1"},
                    {102, "2", 202, "2"},
                    {103, "3", 203, "3"},
                    {104, "4", 204, "4"},
                    {null, null, 205, "5"}});

            // Send S1[6], pushes S1[1] out of window
            sendEvent(EVENTS_S1[5], env);
            oldEvent = env.listener("s0").getLastOldData()[0];
            newEvent = env.listener("s0").getLastNewData()[0];
            compareEvent(oldEvent, null, null, 201, "1");
            compareEvent(newEvent, null, null, 205, "5");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"},
                    {103, "3", 203, "3"},
                    {104, "4", 204, "4"},
                    {null, null, 205, "5"},
                    {null, null, 205, "5"}});

            env.undeployAll();
        }
    }

    private static class EPLJoinMultiColumnLeftOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11".split(",")));
            FromClause fromClause = FromClause.create(
                FilterStream.create(SupportBean_S0.class.getSimpleName(), "s0").addView("keepall"),
                FilterStream.create(SupportBean_S1.class.getSimpleName(), "s1").addView("keepall"));
            fromClause.add(OuterJoinQualifier.create("s0.p00", OuterJoinType.LEFT, "s1.p10").add("s1.p11", "s0.p01"));
            model.setFromClause(fromClause);
            model = SerializableObjectCopier.copyMayFail(model);

            String stmtText = "select s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11 from SupportBean_S0#keepall as s0 left outer join SupportBean_S1#keepall as s1 on s0.p00 = s1.p10 and s1.p11 = s0.p01";
            assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            assertMultiColumnLeft(env);

            EPStatementObjectModel modelReverse = env.eplToModel(stmtText);
            assertEquals(stmtText, modelReverse.toEPL());

            env.undeployAll();
        }
    }

    private static class EPLJoinMultiColumnLeft implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11 from " +
                "SupportBean_S0#length(3) as s0 " +
                "left outer join " +
                "SupportBean_S1#length(5) as s1" +
                " on s0.p00 = s1.p10 and s0.p01 = s1.p11";
            env.compileDeploy(epl).addListener("s0");

            assertMultiColumnLeft(env);

            env.undeployAll();
        }
    }

    private static class EPLJoinMultiColumnRight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11".split(",");
            String epl = "@name('s0') select s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11 from " +
                "SupportBean_S0#length(3) as s0 " +
                "right outer join " +
                "SupportBean_S1#length(5) as s1" +
                " on s0.p00 = s1.p10 and s1.p11 = s0.p01";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "A_1", "B_1"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(2, "A_1", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A_1", "B_1", 2, "A_1", "B_1"});

            env.sendEventBean(new SupportBean_S1(3, "A_2", "B_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 3, "A_2", "B_1"});

            env.sendEventBean(new SupportBean_S1(4, "A_1", "B_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, 4, "A_1", "B_2"});

            env.undeployAll();
        }
    }

    private static class EPLJoinMultiColumnRightCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "s0.theString, s1.theString".split(",");
            String epl = "@name('s0') select s0.theString, s1.theString from " +
                "SupportBean(theString like 'S0%')#keepall as s0 " +
                "right outer join " +
                "SupportBean(theString like 'S1%')#keepall as s1" +
                " on s0.intPrimitive = s1.doublePrimitive and s1.intPrimitive = s0.doublePrimitive";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "S1_1", 10, 20d);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "S1_1"});

            sendEvent(env, "S0_2", 11, 22d);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "S0_3", 11, 21d);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "S0_4", 12, 21d);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "S1_2", 11, 22d);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, "S1_2"});

            sendEvent(env, "S1_3", 22, 11d);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_2", "S1_3"});

            sendEvent(env, "S0_5", 22, 11d);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"S0_5", "S1_2"});

            env.undeployAll();
        }
    }

    private static class EPLJoinRightOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "right");

            // Send S0 events, no events expected
            sendEvent(EVENTS_S0[0], env);
            sendEvent(EVENTS_S0[1], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS, null);

            // Send S1[2]
            sendEvent(EVENTS_S1[2], env);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, null, null, 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 202, "2"}});

            // Send S0[2] events, joined event expected
            sendEvent(EVENTS_S0[2], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 102, "2", 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"}});

            // Send S1[3]
            sendEvent(EVENTS_S1[3], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, null, null, 203, "3");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"},
                    {null, null, 203, "3"}});

            // Send some more S0 events
            sendEvent(EVENTS_S0[3], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 103, "3", 203, "3");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"},
                    {103, "3", 203, "3"}});

            // Send some more S0 events
            sendEvent(EVENTS_S0[4], env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"},
                    {103, "3", 203, "3"}});

            // Push S0[2] out of the window
            sendEvent(EVENTS_S0[5], env);
            theEvent = env.listener("s0").assertOneGetOldAndReset();
            compareEvent(theEvent, 102, "2", 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 202, "2"},
                    {103, "3", 203, "3"}});

            // Some more S1 events
            sendEvent(EVENTS_S1[6], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 206, "6");
            sendEvent(EVENTS_S1[7], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 207, "7");
            sendEvent(EVENTS_S1[8], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), null, null, 208, "8");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{null, null, 202, "2"},
                    {103, "3", 203, "3"},
                    {null, null, 206, "6"},
                    {null, null, 207, "7"},
                    {null, null, 208, "8"}});

            // Push S1[2] out of the window
            sendEvent(EVENTS_S1[9], env);
            EventBean oldEvent = env.listener("s0").getLastOldData()[0];
            EventBean newEvent = env.listener("s0").getLastNewData()[0];
            compareEvent(oldEvent, null, null, 202, "2");
            compareEvent(newEvent, null, null, 209, "9");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{103, "3", 203, "3"},
                    {null, null, 206, "6"},
                    {null, null, 207, "7"},
                    {null, null, 208, "8"},
                    {null, null, 209, "9"}});

            env.undeployAll();
        }
    }

    private static class EPLJoinLeftOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "left");

            // Send S1 events, no events expected
            sendEvent(EVENTS_S1[0], env);
            sendEvent(EVENTS_S1[1], env);
            sendEvent(EVENTS_S1[3], env);
            assertNull(env.listener("s0").getLastNewData());    // No events expected
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS, null);

            // Send S0 event, expect event back from outer join
            sendEvent(EVENTS_S0[2], env);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 102, "2", null, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", null, null}});

            // Send S1 event matching S0, expect event back
            sendEvent(EVENTS_S1[2], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 102, "2", 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"}});

            // Send some more unmatched events
            sendEvent(EVENTS_S1[4], env);
            sendEvent(EVENTS_S1[5], env);
            sendEvent(EVENTS_S1[6], env);
            assertNull(env.listener("s0").getLastNewData());    // No events expected
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"}});

            // Send event, expect a join result
            sendEvent(EVENTS_S0[5], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 105, "5", 205, "5");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", 202, "2"},
                    {105, "5", 205, "5"}});

            // Let S1[2] go out of the window (lenght 5), expected old join event
            sendEvent(EVENTS_S1[7], env);
            sendEvent(EVENTS_S1[8], env);
            theEvent = env.listener("s0").assertOneGetOldAndReset();
            compareEvent(theEvent, 102, "2", 202, "2");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", null, null},
                    {105, "5", 205, "5"}});

            // S0[9] should generate an outer join event
            sendEvent(EVENTS_S0[9], env);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            compareEvent(theEvent, 109, "9", null, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{102, "2", null, null},
                    {109, "9", null, null},
                    {105, "5", 205, "5"}});

            // S0[2] Should leave the window (length 3), should get OLD and NEW event
            sendEvent(EVENTS_S0[10], env);
            EventBean oldEvent = env.listener("s0").getLastOldData()[0];
            EventBean newEvent = env.listener("s0").getLastNewData()[0];
            compareEvent(oldEvent, 102, "2", null, null);     // S1[2] has left the window already
            compareEvent(newEvent, 110, "10", null, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), FIELDS,
                new Object[][]{{110, "10", null, null},
                    {109, "9", null, null},
                    {105, "5", 205, "5"}});

            env.undeployAll();
        }
    }

    private static class EPLJoinEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "left");

            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("s0.p00"));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("s0.id"));
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("s1.p10"));
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("s1.id"));
            assertEquals(4, env.statement("s0").getEventType().getPropertyNames().length);

            env.undeployAll();
        }
    }

    private static void compareEvent(EventBean receivedEvent, Integer idS0, String p00, Integer idS1, String p10) {
        assertEquals(idS0, receivedEvent.get("s0.id"));
        assertEquals(idS1, receivedEvent.get("s1.id"));
        assertEquals(p00, receivedEvent.get("s0.p00"));
        assertEquals(p10, receivedEvent.get("s1.p10"));
    }

    private static void sendEvent(RegressionEnvironment env, String s, int intPrimitive, double doublePrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        bean.setDoublePrimitive(doublePrimitive);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, String s, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendEventMD(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static void assertMultiColumnLeft(RegressionEnvironment env) {
        String[] fields = "s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11".split(",");
        env.sendEventBean(new SupportBean_S0(1, "A_1", "B_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A_1", "B_1", null, null, null});

        env.sendEventBean(new SupportBean_S1(2, "A_1", "B_1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A_1", "B_1", 2, "A_1", "B_1"});

        env.sendEventBean(new SupportBean_S1(3, "A_2", "B_1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(4, "A_1", "B_2"));
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void setupStatement(RegressionEnvironment env, String outerJoinType) {
        String joinStatement = "@name('s0') select irstream s0.id, s0.p00, s1.id, s1.p10 from " +
            "SupportBean_S0#length(3) as s0 " +
            outerJoinType + " outer join " +
            "SupportBean_S1#length(5) as s1" +
            " on s0.p00 = s1.p10";
        env.compileDeployAddListenerMileZero(joinStatement, "s0");
    }

    private static void sendEvent(Object theEvent, RegressionEnvironment env) {
        env.sendEventBean(theEvent);
    }
}
