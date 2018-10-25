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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLOuterJoinLeftWWhere {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinWhereNotNullIs());
        execs.add(new EPLJoinWhereNotNullNE());
        execs.add(new EPLJoinWhereNullIs());
        execs.add(new EPLJoinWhereNullEq());
        execs.add(new EPLJoinWhereJoinOrNull());
        execs.add(new EPLJoinWhereJoin());
        execs.add(new EPLJoinEventType());
        return execs;
    }

    private static class EPLJoinWhereNotNullIs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s1.p11 is not null");
            tryWhereNotNull(env);
            env.undeployAll();
        }
    }

    private static class EPLJoinWhereNotNullNE implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s1.p11 is not null");
            tryWhereNotNull(env);
            env.undeployAll();
        }
    }

    private static class EPLJoinWhereNullIs implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s1.p11 is null");
            tryWhereNull(env);
            env.undeployAll();
        }
    }

    private static class EPLJoinWhereNullEq implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s1.p11 is null");
            tryWhereNull(env);
            env.undeployAll();
        }
    }

    private static class EPLJoinWhereJoinOrNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s0.p01 = s1.p11 or s1.p11 is null");

            SupportBean_S0 eventS0 = new SupportBean_S0(0, "0", "[a]");
            sendEvent(eventS0, env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), eventS0, null);

            // Send events to test the join for multiple rows incl. null value
            SupportBean_S1 s1Bean1 = new SupportBean_S1(1000, "5", "X");
            SupportBean_S1 s1Bean2 = new SupportBean_S1(1001, "5", "Y");
            SupportBean_S1 s1Bean3 = new SupportBean_S1(1002, "5", "X");
            SupportBean_S1 s1Bean4 = new SupportBean_S1(1003, "5", null);
            SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
            sendEvent(env, new Object[]{s1Bean1, s1Bean2, s1Bean3, s1Bean4, s0});

            assertEquals(3, env.listener("s0").getLastNewData().length);
            Object[] received = new Object[3];
            for (int i = 0; i < 3; i++) {
                assertSame(s0, env.listener("s0").getLastNewData()[i].get("s0"));
                received[i] = env.listener("s0").getLastNewData()[i].get("s1");
            }
            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{s1Bean1, s1Bean3, s1Bean4}, received);

            env.undeployAll();
        }
    }

    private static class EPLJoinWhereJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "where s0.p01 = s1.p11");

            SupportBean_S0[] eventsS0 = new SupportBean_S0[15];
            SupportBean_S1[] eventsS1 = new SupportBean_S1[15];
            int count = 100;
            for (int i = 0; i < eventsS0.length; i++) {
                eventsS0[i] = new SupportBean_S0(count++, Integer.toString(i));
            }
            count = 200;
            for (int i = 0; i < eventsS1.length; i++) {
                eventsS1[i] = new SupportBean_S1(count++, Integer.toString(i));
            }

            // Send S0[0] p01=a
            eventsS0[0].setP01("[a]");
            sendEvent(eventsS0[0], env);
            assertFalse(env.listener("s0").isInvoked());

            // Send S1[1] p11=b
            eventsS1[1].setP11("[b]");
            sendEvent(eventsS1[1], env);
            assertFalse(env.listener("s0").isInvoked());

            // Send S0[1] p01=c, no match expected
            eventsS0[1].setP01("[c]");
            sendEvent(eventsS0[1], env);
            assertFalse(env.listener("s0").isInvoked());

            // Send S1[2] p11=d
            eventsS1[2].setP11("[d]");
            sendEvent(eventsS1[2], env);
            // Send S0[2] p01=d
            eventsS0[2].setP01("[d]");
            sendEvent(eventsS0[2], env);
            compareEvent(env.listener("s0").assertOneGetNewAndReset(), eventsS0[2], eventsS1[2]);

            // Send S1[3] and S0[3] with differing props, no match expected
            eventsS1[3].setP11("[e]");
            sendEvent(eventsS1[3], env);
            eventsS0[3].setP01("[e1]");
            sendEvent(eventsS0[3], env);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLJoinEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            setupStatement(env, "");
            EventType type = env.statement("s0").getEventType();
            assertEquals(SupportBean_S0.class, type.getPropertyType("s0"));
            assertEquals(SupportBean_S1.class, type.getPropertyType("s1"));
            env.undeployAll();
        }
    }

    private static void tryWhereNotNull(RegressionEnvironment env) {
        SupportBean_S1 s1Bean1 = new SupportBean_S1(1000, "5", "X");
        SupportBean_S1 s1Bean2 = new SupportBean_S1(1001, "5", null);
        SupportBean_S1 s1Bean3 = new SupportBean_S1(1002, "6", null);
        sendEvent(env, new Object[]{s1Bean1, s1Bean2, s1Bean3});
        assertFalse(env.listener("s0").isInvoked());

        SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
        sendEvent(s0, env);
        compareEvent(env.listener("s0").assertOneGetNewAndReset(), s0, s1Bean1);
    }

    private static void tryWhereNull(RegressionEnvironment env) {
        SupportBean_S1 s1Bean1 = new SupportBean_S1(1000, "5", "X");
        SupportBean_S1 s1Bean2 = new SupportBean_S1(1001, "5", null);
        SupportBean_S1 s1Bean3 = new SupportBean_S1(1002, "6", null);
        sendEvent(env, new Object[]{s1Bean1, s1Bean2, s1Bean3});
        assertFalse(env.listener("s0").isInvoked());

        SupportBean_S0 s0 = new SupportBean_S0(1, "5", "X");
        sendEvent(s0, env);
        compareEvent(env.listener("s0").assertOneGetNewAndReset(), s0, s1Bean2);
    }

    private static void compareEvent(EventBean receivedEvent, SupportBean_S0 expectedS0, SupportBean_S1 expectedS1) {
        assertSame(expectedS0, receivedEvent.get("s0"));
        assertSame(expectedS1, receivedEvent.get("s1"));
    }

    private static void sendEvent(RegressionEnvironment env, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            sendEvent(events[i], env);
        }
    }

    private static void setupStatement(RegressionEnvironment env, String whereClause) {
        String joinStatement = "@name('s0') select * from " +
            "SupportBean_S0#length(5) as s0 " +
            "left outer join " +
            "SupportBean_S1#length(5) as s1" +
            " on s0.p00 = s1.p10 " +
            whereClause;
        env.compileDeployAddListenerMileZero(joinStatement, "s0");
    }

    private static void sendEvent(Object theEvent, RegressionEnvironment env) {
        env.sendEventBean(theEvent);
    }
}
