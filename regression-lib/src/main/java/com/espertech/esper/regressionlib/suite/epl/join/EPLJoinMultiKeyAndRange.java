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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLJoinMultiKeyAndRange {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinRangeNullAndDupAndInvalid());
        execs.add(new EPLJoinMultikeyWArrayHashJoinArray());
        execs.add(new EPLJoinMultikeyWArrayHashJoin2Prop());
        execs.add(new EPLJoinMultikeyWArrayCompositeArray());
        execs.add(new EPLJoinMultikeyWArrayComposite2Prop());
        return execs;
    }

    private static class EPLJoinMultikeyWArrayComposite2Prop implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') select * " +
                "from SupportBean_S0#keepall as s0, SupportBean_S1#keepall as s1 " +
                "where s0.p00 = s1.p10 and s0.p01 = s1.p11 and s0.p02 > s1.p12";
            env.compileDeploy(eplOne).addListener("s0");

            sendS0(env, 10, "a0", "b0", "X");
            sendS1(env, 20, "a0", "b0", "F");
            assertReceived(env, new Object[][] {{10, 20}});

            env.milestone(0);

            sendS0(env, 11, "a1", "b0", "X");
            sendS1(env, 22, "a0", "b1", "F");
            sendS0(env, 12, "a0", "b1", "A");
            assertFalse(env.listener("s0").isInvoked());

            sendS0(env, 13, "a0", "b1", "Z");
            assertReceived(env, new Object[][] {{13, 22}});

            sendS1(env, 23, "a1", "b0", "A");
            assertReceived(env, new Object[][] {{11, 23}});

            env.undeployAll();
        }

        private void assertReceived(RegressionEnvironment env, Object[][] expected) {
            final String[] fields = "s0.id,s1.id".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, expected);
        }

        private void sendS0(RegressionEnvironment env, int id, String p00, String p01, String p02) {
            env.sendEventBean(new SupportBean_S0(id, p00, p01, p02));
        }

        private void sendS1(RegressionEnvironment env, int id, String p10, String p11, String p12) {
            env.sendEventBean(new SupportBean_S1(id, p10, p11, p12));
        }
    }

    private static class EPLJoinMultikeyWArrayCompositeArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') select * " +
                "from SupportEventWithIntArray#keepall as si, SupportEventWithManyArray#keepall as sm " +
                "where si.array = sm.intOne and si.value > sm.value";
            env.compileDeploy(eplOne).addListener("s0");

            sendIntArray(env, "I1", new int[] {1, 2}, 10);
            sendManyArray(env, "M1", new int[] {1, 2}, 5);
            assertReceived(env, new Object[][] {{"I1", "M1"}});

            env.milestone(0);

            sendIntArray(env, "I2", new int[] {1, 2}, 20);
            assertReceived(env, new Object[][] {{"I2", "M1"}});

            sendManyArray(env, "M2", new int[] {1, 2}, 1);
            assertReceived(env, new Object[][] {{"I1", "M2"}, {"I2", "M2"}});

            sendManyArray(env, "M3", new int[] {1}, 1);
            assertFalse(env.listener("s0").isInvoked());

            sendIntArray(env, "I3", new int[] {2}, 30);
            assertFalse(env.listener("s0").isInvoked());

            sendIntArray(env, "I4", new int[] {1}, 40);
            assertReceived(env, new Object[][] {{"I4", "M3"}});

            sendManyArray(env, "M4", new int[] {2}, 2);
            assertReceived(env, new Object[][] {{"I3", "M4"}});

            env.undeployAll();
        }

        private void assertReceived(RegressionEnvironment env, Object[][] expected) {
            final String[] fields = "si.id,sm.id".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, expected);
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] ints, int value) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints).withValue(value));
        }

        private void sendIntArray(RegressionEnvironment env, String id, int[] array, int value) {
            env.sendEventBean(new SupportEventWithIntArray(id, array, value));
        }
    }

    private static class EPLJoinMultikeyWArrayHashJoinArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') select * " +
                "from SupportEventWithIntArray#keepall as si, SupportEventWithManyArray#keepall as sm " +
                "where si.array = sm.intOne";
            env.compileDeploy(eplOne).addListener("s0");

            sendIntArray(env, "I1", new int[] {1, 2});
            sendManyArray(env, "M1", new int[] {1, 2});
            assertReceived(env, new Object[][] {{"I1", "M1"}});

            env.milestone(0);

            sendIntArray(env, "I2", new int[] {1, 2});
            assertReceived(env, new Object[][] {{"I2", "M1"}});

            sendManyArray(env, "M2", new int[] {1, 2});
            assertReceived(env, new Object[][] {{"I1", "M2"}, {"I2", "M2"}});

            sendManyArray(env, "M3", new int[] {1});
            assertFalse(env.listener("s0").isInvoked());

            sendIntArray(env, "I3", new int[] {2});
            assertFalse(env.listener("s0").isInvoked());

            sendIntArray(env, "I4", new int[] {1});
            assertReceived(env, new Object[][] {{"I4", "M3"}});

            sendManyArray(env, "M4", new int[] {2});
            assertReceived(env, new Object[][] {{"I3", "M4"}});

            env.undeployAll();
        }

        private void assertReceived(RegressionEnvironment env, Object[][] expected) {
            final String[] fields = "si.id,sm.id".split(",");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, expected);
        }

        private void sendManyArray(RegressionEnvironment env, String id, int[] ints) {
            env.sendEventBean(new SupportEventWithManyArray(id).withIntOne(ints));
        }

        private void sendIntArray(RegressionEnvironment env, String id, int[] array) {
            env.sendEventBean(new SupportEventWithIntArray(id, array));
        }
    }

    private static class EPLJoinRangeNullAndDupAndInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eplOne = "@name('s0') select sb.* from SupportBean#keepall sb, SupportBeanRange#lastevent where intBoxed between rangeStart and rangeEnd";
            env.compileDeploy(eplOne).addListener("s0");

            String eplTwo = "@name('s1') select sb.* from SupportBean#keepall sb, SupportBeanRange#lastevent where theString = key and intBoxed in [rangeStart: rangeEnd]";
            env.compileDeploy(eplTwo).addListener("s1");

            // null join lookups
            sendEvent(env, new SupportBeanRange("R1", "G", (Integer) null, null));
            sendEvent(env, new SupportBeanRange("R2", "G", null, 10));
            sendEvent(env, new SupportBeanRange("R3", "G", 10, null));
            sendSupportBean(env, "G", -1, null);

            // range invalid
            sendEvent(env, new SupportBeanRange("R4", "G", 10, 0));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.listener("s1").isInvoked());

            // duplicates
            Object eventOne = sendSupportBean(env, "G", 100, 5);
            Object eventTwo = sendSupportBean(env, "G", 101, 5);
            sendEvent(env, new SupportBeanRange("R4", "G", 0, 10));
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{eventOne, eventTwo}, EPAssertionUtil.getUnderlying(events));
            events = env.listener("s1").getAndResetLastNewData();
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{eventOne, eventTwo}, EPAssertionUtil.getUnderlying(events));

            // test string compare
            String eplThree = "@name('s2') select sb.* from SupportBeanRange#keepall sb, SupportBean#lastevent where theString in [rangeStartStr:rangeEndStr]";
            env.compileDeploy(eplThree).addListener("s2");

            sendSupportBean(env, "P", 1, 1);
            sendEvent(env, new SupportBeanRange("R5", "R5", "O", "Q"));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLJoinMultikeyWArrayHashJoin2Prop implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String joinStatement = "@name('s0') select * from " +
                "SupportBean(theString like 'A%')#length(3) as streamA," +
                "SupportBean(theString like 'B%')#length(3) as streamB" +
                " where streamA.intPrimitive = streamB.intPrimitive " +
                "and streamA.intBoxed = streamB.intBoxed";
            env.compileDeploy(joinStatement).addListener("s0");
            String[] fields = "streamA.theString,streamB.theString".split(",");

            assertEquals(SupportBean.class, env.statement("s0").getEventType().getPropertyType("streamA"));
            assertEquals(SupportBean.class, env.statement("s0").getEventType().getPropertyType("streamB"));
            assertEquals(2, env.statement("s0").getEventType().getPropertyNames().length);

            final int[][] eventData = {{1, 100},
                {2, 100},
                {1, 200},
                {2, 200}};
            final SupportBean[] eventsA = new SupportBean[eventData.length];
            final SupportBean[] eventsB = new SupportBean[eventData.length];

            for (int i = 0; i < eventData.length; i++) {
                eventsA[i] = new SupportBean();
                eventsA[i].setTheString("A" + i);
                eventsA[i].setIntPrimitive(eventData[i][0]);
                eventsA[i].setIntBoxed(eventData[i][1]);

                eventsB[i] = new SupportBean();
                eventsB[i].setTheString("B" + i);
                eventsB[i].setIntPrimitive(eventData[i][0]);
                eventsB[i].setIntBoxed(eventData[i][1]);
            }

            sendEvent(env, eventsA[0]);
            sendEvent(env, eventsB[1]);
            sendEvent(env, eventsB[2]);
            sendEvent(env, eventsB[3]);
            assertNull(env.listener("s0").getLastNewData());    // No events expected

            env.milestone(0);

            sendSupportBean(env, "AX", 2, 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[] {"AX", "B1"});

            sendSupportBean(env, "BX", 1, 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[] {"A0", "BX"});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
        return bean;
    }
}
