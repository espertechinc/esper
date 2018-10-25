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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLJoinMultiKeyAndRange {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinRangeNullAndDupAndInvalid());
        execs.add(new EPLJoinMultiKeyed());
        return execs;
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

    private static class EPLJoinMultiKeyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String joinStatement = "@name('s0') select * from " +
                "SupportBean(theString='A')#length(3) as streamA," +
                "SupportBean(theString='B')#length(3) as streamB" +
                " where streamA.intPrimitive = streamB.intPrimitive " +
                "and streamA.intBoxed = streamB.intBoxed";
            env.compileDeploy(joinStatement).addListener("s0");

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
                eventsA[i].setTheString("A");
                eventsA[i].setIntPrimitive(eventData[i][0]);
                eventsA[i].setIntBoxed(eventData[i][1]);

                eventsB[i] = new SupportBean();
                eventsB[i].setTheString("B");
                eventsB[i].setIntPrimitive(eventData[i][0]);
                eventsB[i].setIntBoxed(eventData[i][1]);
            }

            sendEvent(env, eventsA[0]);
            sendEvent(env, eventsB[1]);
            sendEvent(env, eventsB[2]);
            sendEvent(env, eventsB[3]);
            assertNull(env.listener("s0").getLastNewData());    // No events expected

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
