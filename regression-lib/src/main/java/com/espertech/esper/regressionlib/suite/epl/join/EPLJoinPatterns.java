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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.support.bean.SupportBean_S3;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EPLJoinPatterns {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPatternFilterJoin());
        execs.add(new EPLJoin2PatternJoinSelect());
        execs.add(new EPLJoin2PatternJoinWildcard());
        return execs;
    }

    private static class EPLJoinPatternFilterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream es0a.id as es0aId, " +
                "es0a.p00 as es0ap00, " +
                "es0b.id as es0bId, " +
                "es0b.p00 as es0bp00, " +
                "s1.id as s1Id, " +
                "s1.p10 as s1p10 " +
                " from " +
                " pattern [every (es0a=SupportBean_S0(p00='a') " +
                "or es0b=SupportBean_S0(p00='b'))]#length(5) as s0," +
                "SupportBean_S1#length(5) as s1" +
                " where (es0a.id = s1.id) or (es0b.id = s1.id)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS1(env, 1, "s1A");
            sendEventS0(env, 2, "a");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 1, "b");
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, null, null, 1, "b", 1, "s1A");

            sendEventS1(env, 2, "s2A");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, 2, "a", null, null, 2, "s2A");

            sendEventS1(env, 20, "s20A");
            sendEventS1(env, 30, "s30A");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 20, "a");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, 20, "a", null, null, 20, "s20A");

            sendEventS0(env, 20, "b");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, null, null, 20, "b", 20, "s20A");

            sendEventS0(env, 30, "c");   // filtered out
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 40, "a");   // not matching id in s1
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 50, "b");   // pushing an event s0(2, "a") out the window
            theEvent = env.listener("s0").assertOneGetOldAndReset();
            assertEventData(theEvent, 2, "a", null, null, 2, "s2A");

            // stop statement
            SupportListener listener = env.listener("s0");
            env.undeployAll();

            sendEventS1(env, 60, "s20");
            sendEventS0(env, 70, "a");
            sendEventS0(env, 71, "b");
            assertFalse(listener.getAndClearIsInvoked());

            // start statement
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS1(env, 70, "s1-70");
            sendEventS0(env, 60, "a");
            sendEventS1(env, 20, "s1");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 70, "b");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, null, null, 70, "b", 70, "s1-70");

            env.undeployAll();
        }
    }

    private static class EPLJoin2PatternJoinSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream s0.es0.id as s0es0Id," +
                "s0.es1.id as s0es1Id, " +
                "s1.es2.id as s1es2Id, " +
                "s1.es3.id as s1es3Id, " +
                "es0.p00 as es0p00, " +
                "es1.p10 as es1p10, " +
                "es2.p20 as es2p20, " +
                "es3.p30 as es3p30" +
                " from " +
                " pattern [every (es0=SupportBean_S0" +
                " and es1=SupportBean_S1" + ")]#length(3) as s0," +
                " pattern [every (es2=SupportBean_S2" +
                " and es3=SupportBean_S3)]#length(3) as s1" +
                " where s0.es0.id = s1.es2.id";
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS3(env, 2, "d");
            sendEventS0(env, 3, "a");
            sendEventS2(env, 3, "c");
            sendEventS1(env, 1, "b");
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, 3, 1, 3, 2, "a", "b", "c", "d");

            sendEventS0(env, 11, "a1");
            sendEventS2(env, 13, "c1");
            sendEventS1(env, 12, "b1");
            sendEventS3(env, 15, "d1");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS3(env, 25, "d2");
            sendEventS0(env, 21, "a2");
            sendEventS2(env, 21, "c2");
            sendEventS1(env, 26, "b2");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, 21, 26, 21, 25, "a2", "b2", "c2", "d2");

            sendEventS0(env, 31, "a3");
            sendEventS1(env, 32, "b3");
            theEvent = env.listener("s0").assertOneGetOldAndReset();   // event moving out of window
            assertEventData(theEvent, 3, 1, 3, 2, "a", "b", "c", "d");
            sendEventS2(env, 33, "c3");
            sendEventS3(env, 35, "d3");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            sendEventS0(env, 41, "a4");
            sendEventS2(env, 43, "c4");
            sendEventS1(env, 42, "b4");
            sendEventS3(env, 45, "d4");
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            // stop statement
            SupportListener listener = env.listener("s0");
            env.undeployAll();

            sendEventS3(env, 52, "d5");
            sendEventS0(env, 53, "a5");
            sendEventS2(env, 53, "c5");
            sendEventS1(env, 51, "b5");
            assertFalse(listener.isInvoked());

            // start statement
            env.compileDeploy(stmtText).addListener("s0");

            sendEventS3(env, 55, "d6");
            sendEventS0(env, 51, "a6");
            sendEventS2(env, 51, "c6");
            sendEventS1(env, 56, "b6");
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            assertEventData(theEvent, 51, 56, 51, 55, "a6", "b6", "c6", "d6");

            env.undeployAll();
        }
    }

    private static class EPLJoin2PatternJoinWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * " +
                " from " +
                " pattern [every (es0=SupportBean_S0" +
                " and es1=SupportBean_S1)]#length(5) as s0," +
                " pattern [every (es2=SupportBean_S2" +
                " and es3=SupportBean_S3)]#length(5) as s1" +
                " where s0.es0.id = s1.es2.id";
            env.compileDeploy(stmtText).addListener("s0");

            SupportBean_S0 s0 = sendEventS0(env, 100, "");
            SupportBean_S1 s1 = sendEventS1(env, 1, "");
            SupportBean_S2 s2 = sendEventS2(env, 100, "");
            SupportBean_S3 s3 = sendEventS3(env, 2, "");

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();

            Map<String, EventBean> result = (Map<String, EventBean>) theEvent.get("s0");
            assertSame(s0, result.get("es0").getUnderlying());
            assertSame(s1, result.get("es1").getUnderlying());

            result = (Map<String, EventBean>) theEvent.get("s1");
            assertSame(s2, result.get("es2").getUnderlying());
            assertSame(s3, result.get("es3").getUnderlying());

            env.undeployAll();
        }
    }

    private static SupportBean_S0 sendEventS0(RegressionEnvironment env, int id, String p00) {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static SupportBean_S1 sendEventS1(RegressionEnvironment env, int id, String p10) {
        SupportBean_S1 theEvent = new SupportBean_S1(id, p10);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static SupportBean_S2 sendEventS2(RegressionEnvironment env, int id, String p20) {
        SupportBean_S2 theEvent = new SupportBean_S2(id, p20);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static SupportBean_S3 sendEventS3(RegressionEnvironment env, int id, String p30) {
        SupportBean_S3 theEvent = new SupportBean_S3(id, p30);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void assertEventData(EventBean theEvent, int s0es0Id, int s0es1Id, int s1es2Id, int s1es3Id,
                                        String p00, String p10, String p20, String p30) {
        assertEquals(s0es0Id, theEvent.get("s0es0Id"));
        assertEquals(s0es1Id, theEvent.get("s0es1Id"));
        assertEquals(s1es2Id, theEvent.get("s1es2Id"));
        assertEquals(s1es3Id, theEvent.get("s1es3Id"));
        assertEquals(p00, theEvent.get("es0p00"));
        assertEquals(p10, theEvent.get("es1p10"));
        assertEquals(p20, theEvent.get("es2p20"));
        assertEquals(p30, theEvent.get("es3p30"));
    }

    private static void assertEventData(EventBean theEvent,
                                        Integer es0aId, String es0ap00,
                                        Integer es0bId, String es0bp00,
                                        int s1Id, String s1p10
    ) {
        assertEquals(es0aId, theEvent.get("es0aId"));
        assertEquals(es0ap00, theEvent.get("es0ap00"));
        assertEquals(es0bId, theEvent.get("es0bId"));
        assertEquals(es0bp00, theEvent.get("es0bp00"));
        assertEquals(s1Id, theEvent.get("s1Id"));
        assertEquals(s1p10, theEvent.get("s1p10"));
    }
}
