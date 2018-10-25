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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.ArrayHandlingUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLOuterJoin7Stream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinKeyPerStream());
        execs.add(new EPLJoinRootS0());
        execs.add(new EPLJoinRootS1());
        execs.add(new EPLJoinRootS2());
        execs.add(new EPLJoinRootS3());
        execs.add(new EPLJoinRootS4());
        execs.add(new EPLJoinRootS5());
        execs.add(new EPLJoinRootS6());
        return execs;
    }

    private static class EPLJoinKeyPerStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p01 = s2.p20 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p11 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p11 = s4.p40 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p21 = s5.p50 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p21 = s6.p60 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertsKeysPerStream(env);
        }

        private static void tryAssertsKeysPerStream(RegressionEnvironment env) {
            Object[] s0Events, s1Events, s2Events, s3Events, s4Events, s5Events, s6Events;

            // Test s0
            //
            s2Events = SupportBean_S2.makeS2("A-s0-1", new String[]{"A-s2-1", "A-s2-2", "A-s2-3"});
            sendEventsAndReset(env, s2Events);

            Object[] s6Events1 = SupportBean_S6.makeS6("A-s2-1", new String[]{"A-s6-1", "A-s6-2"});
            sendEventsAndReset(env, s6Events1);
            Object[] s6Events2 = SupportBean_S6.makeS6("A-s2-3", new String[]{"A-s6-1"});
            sendEventsAndReset(env, s6Events2);

            s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
            sendEvent(env, s0Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, null, s6Events1[0]},
                {s0Events[0], null, s2Events[0], null, null, null, s6Events1[1]},
                {s0Events[0], null, s2Events[2], null, null, null, s6Events2[0]}}, getAndResetNewEvents(env));

            // Test s0
            //
            s1Events = SupportBean_S1.makeS1("B", new String[]{"B-s1-1", "B-s1-2", "B-s1-3"});
            sendEventsAndReset(env, s1Events);

            s2Events = SupportBean_S2.makeS2("B-s0-1", new String[]{"B-s2-1", "B-s2-2", "B-s2-3", "B-s2-4"});
            sendEventsAndReset(env, s2Events);

            Object[] s5Events1 = SupportBean_S5.makeS5("B-s2-3", new String[]{"B-s6-1"});
            sendEventsAndReset(env, s5Events1);
            Object[] s5Events2 = SupportBean_S5.makeS5("B-s2-4", new String[]{"B-s5-1", "B-s5-2"});
            sendEventsAndReset(env, s5Events2);

            s6Events = SupportBean_S6.makeS6("B-s2-4", new String[]{"B-s6-1"});
            sendEventsAndReset(env, s6Events);

            s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
            sendEvent(env, s0Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[3], null, null, s5Events2[1], s6Events[0]},
                {s0Events[0], null, s2Events[3], null, null, s5Events2[0], s6Events[0]}}, getAndResetNewEvents(env));

            // Test s0
            //
            s1Events = SupportBean_S1.makeS1("C", new String[]{"C-s1-1", "C-s1-2", "C-s1-3"});
            sendEventsAndReset(env, s1Events);

            s2Events = SupportBean_S2.makeS2("C-s0-1", new String[]{"C-s2-1", "C-s2-2"});
            sendEventsAndReset(env, s2Events);

            s3Events = SupportBean_S3.makeS3("C-s1-2", new String[]{"C-s3-1"});
            sendEventsAndReset(env, s3Events);

            s5Events1 = SupportBean_S5.makeS5("C-s2-1", new String[]{"C-s5-1"});
            sendEventsAndReset(env, s5Events1);
            s5Events2 = SupportBean_S5.makeS5("C-s2-2", new String[]{"C-s5-1", "C-s5-2"});
            sendEventsAndReset(env, s5Events2);

            s6Events1 = SupportBean_S6.makeS6("C-s2-1", new String[]{"C-s6-1"});
            sendEventsAndReset(env, s6Events1);
            s6Events2 = SupportBean_S6.makeS6("C-s2-2", new String[]{"C-s6-2"});
            sendEventsAndReset(env, s6Events2);

            s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
            sendEvent(env, s0Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], null, s5Events1[0], s6Events1[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], null, s5Events2[0], s6Events2[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], null, s5Events2[1], s6Events2[0]},
            }, getAndResetNewEvents(env));

            // Test s0
            //
            s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-3", "D-s1-2", "D-s1-1"});
            sendEventsAndReset(env, s1Events);

            s2Events = SupportBean_S2.makeS2("D-s0-1", new String[]{"D-s2-2", "D-s2-1"});
            sendEventsAndReset(env, s2Events);

            Object[] s3Events1 = SupportBean_S3.makeS3("D-s1-1", new String[]{"D-s3-1", "D-s3-2"});
            sendEventsAndReset(env, s3Events1);
            Object[] s3Events2 = SupportBean_S3.makeS3("D-s1-3", new String[]{"D-s3-3", "D-s3-4"});
            sendEventsAndReset(env, s3Events2);

            s4Events = SupportBean_S4.makeS4("D-s1-2", new String[]{"D-s4-1", "D-s4-2"});
            sendEventsAndReset(env, s4Events);
            s4Events = SupportBean_S4.makeS4("D-s1-3", new String[]{"D-s4-3", "D-s4-4"});
            sendEventsAndReset(env, s4Events);

            s5Events = SupportBean_S5.makeS5("D-s2-1", new String[]{"D-s5-1", "D-s5-2"});
            sendEventsAndReset(env, s5Events);

            s6Events = SupportBean_S6.makeS6("D-s2-2", new String[]{"D-s6-1"});
            sendEventsAndReset(env, s6Events);

            s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
            sendEvent(env, s0Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events2[0], s4Events[0], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events2[0], s4Events[1], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events2[1], s4Events[0], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events2[1], s4Events[1], null, s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[0], s3Events1[0], null, null, s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[0], s3Events1[1], null, null, s6Events[0]},
            }, getAndResetNewEvents(env));

            // Test s1
            //
            s3Events = SupportBean_S3.makeS3("E-s1-1", new String[]{"E-s3-1"});
            sendEventsAndReset(env, s3Events);

            s4Events = SupportBean_S4.makeS4("E-s1-1", new String[]{"E-s4-1"});
            sendEventsAndReset(env, s4Events);

            s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1", "E-s0-2", "E-s0-3", "E-s0-4"});
            sendEvent(env, s0Events);

            Object[] s2Events1 = SupportBean_S2.makeS2("E-s0-1", new String[]{"E-s2-1", "E-s2-2"});
            sendEventsAndReset(env, s2Events1);
            Object[] s2Events2 = SupportBean_S2.makeS2("E-s0-3", new String[]{"E-s2-3", "E-s2-4"});
            sendEventsAndReset(env, s2Events2);
            Object[] s2Events3 = SupportBean_S2.makeS2("E-s0-4", new String[]{"E-s2-5", "E-s2-6"});
            sendEventsAndReset(env, s2Events3);

            s5Events1 = SupportBean_S5.makeS5("E-s2-2", new String[]{"E-s5-1", "E-s5-2"});
            sendEventsAndReset(env, s5Events1);
            s5Events2 = SupportBean_S5.makeS5("E-s2-4", new String[]{"E-s5-3"});
            sendEventsAndReset(env, s5Events2);

            s6Events1 = SupportBean_S6.makeS6("E-s2-2", new String[]{"E-s6-1"});
            sendEventsAndReset(env, s6Events1);
            s6Events2 = SupportBean_S6.makeS6("E-s2-5", new String[]{"E-s6-2"});
            sendEventsAndReset(env, s6Events2);

            s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
            sendEvent(env, s1Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events1[1], s3Events[0], s4Events[0], s5Events1[0], s6Events1[0]},
                {s0Events[0], s1Events[0], s2Events1[1], s3Events[0], s4Events[0], s5Events1[1], s6Events1[0]},
                {s0Events[3], s1Events[0], s2Events3[0], s3Events[0], s4Events[0], null, s6Events2[0]},
            }, getAndResetNewEvents(env));

            // Test s2
            //
            s5Events = SupportBean_S5.makeS5("F-s2-1", new String[]{"F-s5-1"});
            sendEventsAndReset(env, s5Events);

            s6Events = SupportBean_S6.makeS6("F-s2-1", new String[]{"F-s6-1"});
            sendEventsAndReset(env, s6Events);

            s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s2-1", "F-s2-2"});
            sendEventsAndReset(env, s0Events);

            s3Events1 = SupportBean_S3.makeS3("F-s1-1", new String[]{"F-s3-1"});
            sendEventsAndReset(env, s3Events1);
            s3Events2 = SupportBean_S3.makeS3("F-s1-3", new String[]{"F-s3-2"});
            sendEventsAndReset(env, s3Events2);

            s4Events = SupportBean_S4.makeS4("F-s1-1", new String[]{"F-s4-1"});
            sendEventsAndReset(env, s4Events);

            Object[] s1Events1 = SupportBean_S1.makeS1("F", new String[]{"F-s1-1"});
            sendEventsAndReset(env, s1Events1);
            Object[] s1Events2 = SupportBean_S1.makeS1("F", new String[]{"F-s1-2"});
            sendEventsAndReset(env, s1Events2);
            Object[] s1Events3 = SupportBean_S1.makeS1("F", new String[]{"F-s1-3"});
            sendEventsAndReset(env, s1Events3);

            s2Events = SupportBean_S2.makeS2("F-s2-1", new String[]{"F-s2-1"});
            sendEvent(env, s2Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events1[0], s2Events[0], s3Events1[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events3[0], s2Events[0], s3Events2[0], null, s5Events[0], s6Events[0]}
            }, getAndResetNewEvents(env));

            // Test s3
            //
            s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-3", "G-s1-2", "G-s1-3"});
            sendEventsAndReset(env, s1Events);

            s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s2-1", "G-s2-2"});
            sendEventsAndReset(env, s0Events);

            s6Events = SupportBean_S6.makeS6("G-s2-2", new String[]{"G-s6-1"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("G-s2-2", new String[]{"G-s2-2"});
            sendEvent(env, s2Events);

            s4Events = SupportBean_S4.makeS4("G-s1-2", new String[]{"G-s4-1"});
            sendEventsAndReset(env, s4Events);

            s3Events = SupportBean_S3.makeS3("G-s1-2", new String[]{"G-s3-1"});
            sendEvent(env, s3Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}
            }, getAndResetNewEvents(env));

            // Test s3
            //
            s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-3", "H-s1-2", "H-s1-3"});
            sendEventsAndReset(env, s1Events);

            s4Events = SupportBean_S4.makeS4("H-s1-2", new String[]{"H-s4-1"});
            sendEventsAndReset(env, s4Events);

            s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s2-1", "H-s2-2", "H-s2-3"});
            sendEventsAndReset(env, s0Events);

            s2Events1 = SupportBean_S2.makeS2("H-s2-2", new String[]{"H-s2-20"});
            sendEvent(env, s2Events1);
            s2Events2 = SupportBean_S2.makeS2("H-s2-3", new String[]{"H-s2-30", "H-s2-31"});
            sendEvent(env, s2Events2);

            s6Events1 = SupportBean_S6.makeS6("H-s2-20", new String[]{"H-s6-1"});
            sendEventsAndReset(env, s6Events1);
            s6Events2 = SupportBean_S6.makeS6("H-s2-31", new String[]{"H-s6-3", "H-s6-4"});
            sendEventsAndReset(env, s6Events2);

            s3Events = SupportBean_S3.makeS3("H-s1-2", new String[]{"H-s3-1"});
            sendEvent(env, s3Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[1], s2Events1[0], s3Events[0], s4Events[0], null, s6Events1[0]},
                {s0Events[2], s1Events[1], s2Events2[1], s3Events[0], s4Events[0], null, s6Events2[0]},
                {s0Events[2], s1Events[1], s2Events2[1], s3Events[0], s4Events[0], null, s6Events2[1]},
            }, getAndResetNewEvents(env));

            // Test s4
            //
            s3Events = SupportBean_S3.makeS3("I-s1-3", new String[]{"I-s3-1"});
            sendEvent(env, s3Events);

            s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1", "I-s1-2", "I-s1-3"});
            sendEventsAndReset(env, s1Events);

            s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s2-1", "I-s2-2", "I-s2-3"});
            sendEventsAndReset(env, s0Events);

            s2Events1 = SupportBean_S2.makeS2("I-s2-1", new String[]{"I-s2-20"});
            sendEvent(env, s2Events1);
            s2Events2 = SupportBean_S2.makeS2("I-s2-2", new String[]{"I-s2-30", "I-s2-31"});
            sendEvent(env, s2Events2);

            s5Events = SupportBean_S5.makeS5("I-s2-30", new String[]{"I-s5-1", "I-s5-2"});
            sendEventsAndReset(env, s5Events);

            s6Events = SupportBean_S6.makeS6("I-s2-30", new String[]{"I-s6-1", "I-s6-2"});
            sendEventsAndReset(env, s6Events);

            s4Events = SupportBean_S4.makeS4("I-s1-3", new String[]{"I-s4-1"});
            sendEvent(env, s4Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[2], s2Events2[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[1], s1Events[2], s2Events2[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[1], s1Events[2], s2Events2[0], s3Events[0], s4Events[0], s5Events[0], s6Events[1]},
                {s0Events[1], s1Events[2], s2Events2[0], s3Events[0], s4Events[0], s5Events[1], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("J-s2-30", new String[]{"J-s6-1", "J-s6-2"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("J-s2-1", new String[]{"J-s2-30", "J-s2-31"});
            sendEvent(env, s2Events);

            s5Events = SupportBean_S5.makeS5("J-s2-30", new String[]{"J-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, s2Events[0], null, null, s5Events[0], s6Events[0]},
                {null, null, s2Events[0], null, null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("K-s2-31", new String[]{"K-s6-1", "K-s6-2"});
            sendEventsAndReset(env, s6Events);

            s0Events = SupportBean_S0.makeS0("K", new String[]{"K-s2-1"});
            sendEventsAndReset(env, s0Events);

            s2Events = SupportBean_S2.makeS2("K-s2-1", new String[]{"K-s2-30", "K-s2-31"});
            sendEvent(env, s2Events);

            s5Events = SupportBean_S5.makeS5("K-s2-31", new String[]{"K-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("L-s2-31", new String[]{"L-s6-1", "L-s6-2"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("L-s2-1", new String[]{"L-s2-30", "L-s2-31"});
            sendEvent(env, s2Events);

            s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s2-1"});
            sendEventsAndReset(env, s0Events);

            s1Events = SupportBean_S1.makeS1("L", new String[]{"L-s1-1"});
            sendEventsAndReset(env, s1Events);

            s5Events = SupportBean_S5.makeS5("L-s2-31", new String[]{"L-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("M-s2-31", new String[]{"M-s6-1", "M-s6-2"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("M-s2-1", new String[]{"M-s2-30", "M-s2-31"});
            sendEvent(env, s2Events);

            s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s2-1"});
            sendEventsAndReset(env, s0Events);

            s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
            sendEventsAndReset(env, s1Events);

            s3Events = SupportBean_S3.makeS3("M-s1-1", new String[]{"M-s3-1"});
            sendEventsAndReset(env, s3Events);

            s5Events = SupportBean_S5.makeS5("M-s2-31", new String[]{"M-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("N-s2-31", new String[]{"N-s6-1", "N-s6-2"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("N-s2-1", new String[]{"N-s2-30", "N-s2-31"});
            sendEvent(env, s2Events);

            s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s2-1"});
            sendEventsAndReset(env, s0Events);

            s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1", "N-s1-2", "N-s1-3"});
            sendEventsAndReset(env, s1Events);

            s3Events = SupportBean_S3.makeS3("N-s1-3", new String[]{"N-s3-1"});
            sendEventsAndReset(env, s3Events);

            s5Events = SupportBean_S5.makeS5("N-s2-31", new String[]{"N-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[2], s2Events[1], s3Events[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events[0], null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s5
            //
            s6Events = SupportBean_S6.makeS6("O-s2-31", new String[]{"O-s6-1", "O-s6-2"});
            sendEventsAndReset(env, s6Events);

            s2Events = SupportBean_S2.makeS2("O-s2-1", new String[]{"O-s2-30", "O-s2-31"});
            sendEvent(env, s2Events);

            s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s2-1"});
            sendEventsAndReset(env, s0Events);

            s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1", "O-s1-2", "O-s1-3"});
            sendEventsAndReset(env, s1Events);

            s3Events1 = SupportBean_S3.makeS3("O-s1-2", new String[]{"O-s3-1", "O-s3-2"});
            sendEventsAndReset(env, s3Events1);
            s3Events2 = SupportBean_S3.makeS3("O-s1-3", new String[]{"O-s3-3"});
            sendEventsAndReset(env, s3Events2);

            s5Events = SupportBean_S5.makeS5("O-s2-31", new String[]{"O-s5-1"});
            sendEvent(env, s5Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[1], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events2[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[0], null, s5Events[0], s6Events[1]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[1], null, s5Events[0], s6Events[1]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events2[0], null, s5Events[0], s6Events[1]}
            }, getAndResetNewEvents(env));

            // Test s6
            //
            s5Events = SupportBean_S5.makeS5("P-s2-31", new String[]{"P-s5-1"});
            sendEvent(env, s5Events);

            s2Events = SupportBean_S2.makeS2("P-s2-1", new String[]{"P-s2-30", "P-s2-31"});
            sendEvent(env, s2Events);

            s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s2-1"});
            sendEventsAndReset(env, s0Events);

            s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1", "P-s1-2", "P-s1-3"});
            sendEventsAndReset(env, s1Events);

            s3Events1 = SupportBean_S3.makeS3("P-s1-2", new String[]{"P-s3-1", "P-s3-2"});
            sendEventsAndReset(env, s3Events1);
            s3Events2 = SupportBean_S3.makeS3("P-s1-3", new String[]{"P-s3-3"});
            sendEventsAndReset(env, s3Events2);

            s6Events = SupportBean_S6.makeS6("P-s2-31", new String[]{"P-s6-1"});
            sendEvent(env, s6Events);
            EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events1[1], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events2[0], null, s5Events[0], s6Events[0]}
            }, getAndResetNewEvents(env));

            env.undeployAll();
        }
    }

    private static class EPLJoinRootS0 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S0#length(1000) as s0 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS1 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S1#length(1000) as s1 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 " +
                " right outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S2#length(1000) as s2 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 " +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS3 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S3#length(1000) as s3 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s3.p30 = s1.p10 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 " +
                " right outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS4 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S4#length(1000) as s4 " +
                " right outer join SupportBean_S1#length(1000) as s1 on s4.p40 = s1.p10 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " right outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS5 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S5#length(1000) as s5 " +
                " right outer join SupportBean_S2#length(1000) as s2 on s2.p20 = s5.p50 " +
                " right outer join SupportBean_S6#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static class EPLJoinRootS6 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            /**
             * Query:
             *          s0 -> s1
             *                  <- s3
             *                  -> s4
             *             <- s2
             *                  -> s5
             *                  <- s6
             */
            String epl = "@name('s0') select * from " +
                "SupportBean_S6#length(1000) as s6 " +
                " left outer join SupportBean_S2#length(1000) as s2 on s2.p20 = s6.p60 " +
                " left outer join SupportBean_S5#length(1000) as s5 on s2.p20 = s5.p50 " +
                " left outer join " + "SupportBean_S0#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join SupportBean_S3#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join SupportBean_S4#length(1000) as s4 on s1.p10 = s4.p40 ";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertion(env);
        }
    }

    private static void tryAssertion(RegressionEnvironment env) {
        Object[] s0Events, s1Events, s2Events, s3Events, s4Events, s5Events, s6Events;

        // Test s0 and s1=0, s2=0, s3=0, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s0 and s1=0, s2=1, s3=0, s4=0, s5=0, s6=0
        //
        s2Events = SupportBean_S2.makeS2("B", new String[]{"B-s2-1"});
        sendEvent(env, s2Events);
        assertFalse(env.listener("s0").isInvoked());

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s0 and s1=0, s2=1, s3=0, s4=0, s5=0, s6=1
        //
        s2Events = SupportBean_S2.makeS2("C", new String[]{"C-s2-1"});
        sendEvent(env, s2Events);
        assertFalse(env.listener("s0").isInvoked());

        s6Events = SupportBean_S6.makeS6("C", new String[]{"C-s6-1"});
        sendEvent(env, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(env));

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(env));

        // Test s0 and s1=1, s2=1, s3=1, s4=0, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1"});
        sendEvent(env, s1Events);
        assertFalse(env.listener("s0").isInvoked());

        s2Events = SupportBean_S2.makeS2("D", new String[]{"D-s2-1"});
        sendEvent(env, s2Events);
        assertFalse(env.listener("s0").isInvoked());

        s3Events = SupportBean_S3.makeS3("D", new String[]{"D-s3-1"});
        sendEvent(env, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(env));

        s5Events = SupportBean_S5.makeS5("D", new String[]{"D-s5-1"});
        sendEvent(env, s5Events);
        assertFalse(env.listener("s0").isInvoked());

        s6Events = SupportBean_S6.makeS6("D", new String[]{"D-s6-1"});
        sendEvent(env, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s0 and s1=1, s2=1, s3=1, s4=2, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("E", new String[]{"E-s2-1"});
        sendEventsAndReset(env, s3Events);

        s4Events = SupportBean_S4.makeS4("E", new String[]{"E-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        s5Events = SupportBean_S5.makeS5("E", new String[]{"E-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("E", new String[]{"E-s6-1"});
        sendEventsAndReset(env, s6Events);

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s0 and s1=2, s2=2, s3=1, s4=2, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("F", new String[]{"F-s1-1", "F-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1", "F-s2-2"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("F", new String[]{"F-s3-1"});
        sendEventsAndReset(env, s3Events);

        s4Events = SupportBean_S4.makeS4("F", new String[]{"F-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        s5Events = SupportBean_S5.makeS5("F", new String[]{"F-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("F", new String[]{"F-s6-1"});
        sendEventsAndReset(env, s6Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s0 and s1=1, s2=1, s3=2, s4=2, s5=1, s6=2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("G", new String[]{"G-s3-1", "G-s3-2"});
        sendEventsAndReset(env, s3Events);

        s4Events = SupportBean_S4.makeS4("G", new String[]{"G-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        s5Events = SupportBean_S5.makeS5("G", new String[]{"G-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("G", new String[]{"G-s6-1", "G-s6-2"});
        sendEventsAndReset(env, s6Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[1]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0], s5Events[0], s6Events[1]}}, getAndResetNewEvents(env));

        // Test s0 and s1=2, s2=2, s3=1, s4=1, s5=2, s6=1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1", "H-s1-2"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("H", new String[]{"H-s2-1", "H-s2-2"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("H", new String[]{"H-s3-1"});
        sendEventsAndReset(env, s3Events);

        s4Events = SupportBean_S4.makeS4("H", new String[]{"H-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        s5Events = SupportBean_S5.makeS5("H", new String[]{"H-s5-1", "H-s5-2"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("H", new String[]{"H-s6-1"});
        sendEventsAndReset(env, s6Events);

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s0-1"});
        sendEvent(env, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[1], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s1 and s0=1, s2=1, s3=1, s4=0, s5=1, s6=0
        //
        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-1"});
        sendEvent(env, s0Events);

        s2Events = SupportBean_S2.makeS2("I", new String[]{"I-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("I", new String[]{"I-s3-1"});
        sendEventsAndReset(env, s3Events);

        s5Events = SupportBean_S5.makeS5("I", new String[]{"I-s5-1"});
        sendEventsAndReset(env, s5Events);

        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1", "I-s1-2"});
        sendEvent(env, s1Events);
        assertFalse(env.listener("s0").isInvoked());    // no s6

        // Test s1 and s0=1, s2=1, s3=1, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEvent(env, s0Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("J", new String[]{"J-s3-1"});
        sendEventsAndReset(env, s3Events);

        s5Events = SupportBean_S5.makeS5("J", new String[]{"J-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("J", new String[]{"J-s6-1"});
        sendEventsAndReset(env, s6Events);

        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s1 and s0=1, s2=1, s3=1, s4=1, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("K", new String[]{"K-s0-1"});
        sendEvent(env, s0Events);

        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("K", new String[]{"K-s3-1"});
        sendEventsAndReset(env, s3Events);

        s4Events = SupportBean_S4.makeS4("K", new String[]{"K-s4-1"});
        sendEventsAndReset(env, s4Events);

        s6Events = SupportBean_S6.makeS6("K", new String[]{"K-s6-1"});
        sendEventsAndReset(env, s6Events);

        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1"});
        sendEvent(env, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}}, getAndResetNewEvents(env));

        // Test s2 and s0=1, s1=0, s3=0, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s0-1"});
        sendEventsAndReset(env, s0Events);

        s6Events = SupportBean_S6.makeS6("L", new String[]{"L-s6-1"});
        sendEventsAndReset(env, s6Events);

        s2Events = SupportBean_S2.makeS2("L", new String[]{"L-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(env));

        // Test s2 and s0=1, s1=1, s3=0, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEventsAndReset(env, s1Events);

        s5Events = SupportBean_S5.makeS5("M", new String[]{"M-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("M", new String[]{"M-s6-1"});
        sendEventsAndReset(env, s6Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s2 and s0=1, s1=1, s3=1, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1"});
        sendEventsAndReset(env, s1Events);

        s3Events = SupportBean_S3.makeS3("N", new String[]{"N-s3-1"});
        sendEventsAndReset(env, s3Events);

        s5Events = SupportBean_S5.makeS5("N", new String[]{"N-s5-1"});
        sendEventsAndReset(env, s5Events);

        s6Events = SupportBean_S6.makeS6("N", new String[]{"N-s6-1"});
        sendEventsAndReset(env, s6Events);

        s2Events = SupportBean_S2.makeS2("N", new String[]{"N-s2-1"});
        sendEvent(env, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("O", new String[]{"O-s3-1"});
        sendEvent(env, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(env));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("O", new String[]{"O-s3-1"});
        sendEvent(env, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(env));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s0-1"});
        sendEventsAndReset(env, s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1"});
        sendEventsAndReset(env, s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[]{"P-s2-1"});
        sendEventsAndReset(env, s2Events);

        s6Events = SupportBean_S6.makeS6("P", new String[]{"P-s6-1"});
        sendEventsAndReset(env, s6Events);

        s3Events = SupportBean_S3.makeS3("P", new String[]{"P-s3-1"});
        sendEvent(env, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, null, s6Events[0]}}, getAndResetNewEvents(env));

        // Test s3 and s0=1, s1=1, s2=1, s4=2, s5=2, s6=1
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[]{"Q-s0-1"});
        sendEvent(env, s0Events);
        assertFalse(env.listener("s0").isInvoked());

        s1Events = SupportBean_S1.makeS1("Q", new String[]{"Q-s1-1"});
        sendEvent(env, s1Events);
        assertFalse(env.listener("s0").isInvoked());

        s2Events = SupportBean_S2.makeS2("Q", new String[]{"Q-s2-1"});
        sendEvent(env, s2Events);
        assertFalse(env.listener("s0").isInvoked());

        s4Events = SupportBean_S4.makeS4("Q", new String[]{"Q-s4-1", "Q-s4-2"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        s5Events = SupportBean_S5.makeS5("Q", new String[]{"Q-s5-1", "Q-s5-2"});
        sendEvent(env, s5Events);
        assertFalse(env.listener("s0").isInvoked());

        s6Events = SupportBean_S6.makeS6("Q", new String[]{"Q-s6-1"});
        sendEvent(env, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]},
            {s0Events[0], null, s2Events[0], null, null, s5Events[1], s6Events[0]}}, getAndResetNewEvents(env));

        s3Events = SupportBean_S3.makeS3("Q", new String[]{"Q-s3-1"});
        sendEvent(env, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1], s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1], s5Events[1], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s4 and s0=1, s1=1, s2=0, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("R", new String[]{"R-s0-1"});
        sendEvent(env, s0Events);

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEvent(env, s1Events);

        s4Events = SupportBean_S4.makeS4("R", new String[]{"R-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s4 and s0=2, s1=1, s2=1, s4=0, s5=0, s6=2
        //
        s0Events = SupportBean_S0.makeS0("S", new String[]{"S-s0-1", "S-s0-2"});
        sendEvent(env, s0Events);

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1"});
        sendEvent(env, s1Events);

        s2Events = SupportBean_S2.makeS2("S", new String[]{"S-s2-1"});
        sendEventsAndReset(env, s2Events);

        s6Events = SupportBean_S6.makeS6("S", new String[]{"S-s6-1"});
        sendEvent(env, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]},
            {s0Events[1], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(env));

        s4Events = SupportBean_S4.makeS4("S", new String[]{"S-s4-1"});
        sendEvent(env, s4Events);
        assertFalse(env.listener("s0").isInvoked());

        // Test s4 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("T", new String[]{"T-s0-1"});
        sendEvent(env, s0Events);

        s1Events = SupportBean_S1.makeS1("T", new String[]{"T-s1-1"});
        sendEvent(env, s1Events);

        s2Events = SupportBean_S2.makeS2("T", new String[]{"T-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("T", new String[]{"T-s3-1"});
        sendEventsAndReset(env, s3Events);

        s6Events = SupportBean_S6.makeS6("T", new String[]{"T-s6-1"});
        sendEvent(env, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, null, s6Events[0]}}, getAndResetNewEvents(env));

        s4Events = SupportBean_S4.makeS4("T", new String[]{"T-s4-1"});
        sendEvent(env, s4Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}}, getAndResetNewEvents(env));

        // Test s5 and s0=1, s1=0, s2=1, s3=0, s4=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1"});
        sendEvent(env, s0Events);

        s2Events = SupportBean_S2.makeS2("U", new String[]{"U-s2-1"});
        sendEventsAndReset(env, s2Events);

        s6Events = SupportBean_S6.makeS6("U", new String[]{"U-s6-1"});
        sendEventsAndReset(env, s6Events);

        s5Events = SupportBean_S5.makeS5("U", new String[]{"U-s5-1"});
        sendEvent(env, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        // Test s6 and s0=1, s1=2, s2=1, s3=0, s4=0, s6=2
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1"});
        sendEvent(env, s0Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1"});
        sendEvent(env, s1Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s2-1"});
        sendEventsAndReset(env, s2Events);

        s6Events = SupportBean_S6.makeS6("V", new String[]{"V-s6-1", "V-s6-2"});
        sendEventsAndReset(env, s6Events);

        s5Events = SupportBean_S5.makeS5("V", new String[]{"V-s5-1"});
        sendEvent(env, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]},
            {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[1]}}, getAndResetNewEvents(env));

        // Test s5 and s0=1, s1=2, s2=1, s3=1, s4=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("W", new String[]{"W-s0-1"});
        sendEvent(env, s0Events);

        s1Events = SupportBean_S1.makeS1("W", new String[]{"W-s1-1", "W-s1-2"});
        sendEvent(env, s1Events);

        s2Events = SupportBean_S2.makeS2("W", new String[]{"W-s2-1"});
        sendEventsAndReset(env, s2Events);

        s3Events = SupportBean_S3.makeS3("W", new String[]{"W-s3-1"});
        sendEventsAndReset(env, s3Events);

        s6Events = SupportBean_S6.makeS6("W", new String[]{"W-s6-1"});
        sendEventsAndReset(env, s6Events);

        s5Events = SupportBean_S5.makeS5("W", new String[]{"W-s5-1"});
        sendEvent(env, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
            {s0Events[0], s1Events[1], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]},
            {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(env));

        env.undeployAll();
    }

    private static void sendEventsAndReset(RegressionEnvironment env, Object[] events) {
        sendEvent(env, events);
        env.listener("s0").reset();
    }

    private static void sendEvent(RegressionEnvironment env, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            env.sendEventBean(events[i]);
        }
    }

    private static Object[][] getAndResetNewEvents(RegressionEnvironment env) {
        EventBean[] newEvents = env.listener("s0").getLastNewData();
        env.listener("s0").reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2", "s3", "s4", "s5", "s6"});
    }
}
