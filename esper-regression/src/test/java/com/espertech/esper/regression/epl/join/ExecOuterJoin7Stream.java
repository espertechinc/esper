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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.ArrayHandlingUtil;

import static org.junit.Assert.assertFalse;

public class ExecOuterJoin7Stream implements RegressionExecution {
    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();
    private final static String EVENT_S3 = SupportBean_S3.class.getName();
    private final static String EVENT_S4 = SupportBean_S4.class.getName();
    private final static String EVENT_S5 = SupportBean_S5.class.getName();
    private final static String EVENT_S6 = SupportBean_S6.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionKeyPerStream(epService);
        runAssertionRoot_s0(epService);
        runAssertionRoot_s1(epService);
        runAssertionRoot_s2(epService);
        runAssertionRoot_s3(epService);
        runAssertionRoot_s4(epService);
        runAssertionRoot_s5(epService);
        runAssertionRoot_s6(epService);
    }

    private void runAssertionKeyPerStream(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p01 = s2.p20 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p11 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p11 = s4.p40 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p21 = s5.p50 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p21 = s6.p60 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertsKeysPerStream(epService, listener);
    }

    private void tryAssertsKeysPerStream(EPServiceProvider epService, SupportUpdateListener listener) {
        Object[] s0Events, s1Events, s2Events, s3Events, s4Events, s5Events, s6Events;

        // Test s0
        //
        s2Events = SupportBean_S2.makeS2("A-s0-1", new String[]{"A-s2-1", "A-s2-2", "A-s2-3"});
        sendEventsAndReset(epService, listener, s2Events);

        Object[] s6Events_1 = SupportBean_S6.makeS6("A-s2-1", new String[]{"A-s6-1", "A-s6-2"});
        sendEventsAndReset(epService, listener, s6Events_1);
        Object[] s6Events_2 = SupportBean_S6.makeS6("A-s2-3", new String[]{"A-s6-1"});
        sendEventsAndReset(epService, listener, s6Events_2);

        s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, null, s6Events_1[0]},
                {s0Events[0], null, s2Events[0], null, null, null, s6Events_1[1]},
                {s0Events[0], null, s2Events[2], null, null, null, s6Events_2[0]}}, getAndResetNewEvents(listener));

        // Test s0
        //
        s1Events = SupportBean_S1.makeS1("B", new String[]{"B-s1-1", "B-s1-2", "B-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("B-s0-1", new String[]{"B-s2-1", "B-s2-2", "B-s2-3", "B-s2-4"});
        sendEventsAndReset(epService, listener, s2Events);

        Object[] s5Events_1 = SupportBean_S5.makeS5("B-s2-3", new String[]{"B-s6-1"});
        sendEventsAndReset(epService, listener, s5Events_1);
        Object[] s5Events_2 = SupportBean_S5.makeS5("B-s2-4", new String[]{"B-s5-1", "B-s5-2"});
        sendEventsAndReset(epService, listener, s5Events_2);

        s6Events = SupportBean_S6.makeS6("B-s2-4", new String[]{"B-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[3], null, null, s5Events_2[1], s6Events[0]},
                {s0Events[0], null, s2Events[3], null, null, s5Events_2[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s0
        //
        s1Events = SupportBean_S1.makeS1("C", new String[]{"C-s1-1", "C-s1-2", "C-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("C-s0-1", new String[]{"C-s2-1", "C-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("C-s1-2", new String[]{"C-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events_1 = SupportBean_S5.makeS5("C-s2-1", new String[]{"C-s5-1"});
        sendEventsAndReset(epService, listener, s5Events_1);
        s5Events_2 = SupportBean_S5.makeS5("C-s2-2", new String[]{"C-s5-1", "C-s5-2"});
        sendEventsAndReset(epService, listener, s5Events_2);

        s6Events_1 = SupportBean_S6.makeS6("C-s2-1", new String[]{"C-s6-1"});
        sendEventsAndReset(epService, listener, s6Events_1);
        s6Events_2 = SupportBean_S6.makeS6("C-s2-2", new String[]{"C-s6-2"});
        sendEventsAndReset(epService, listener, s6Events_2);

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], null, s5Events_1[0], s6Events_1[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], null, s5Events_2[0], s6Events_2[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], null, s5Events_2[1], s6Events_2[0]},
        }, getAndResetNewEvents(listener));

        // Test s0
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-3", "D-s1-2", "D-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("D-s0-1", new String[]{"D-s2-2", "D-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        Object[] s3Events_1 = SupportBean_S3.makeS3("D-s1-1", new String[]{"D-s3-1", "D-s3-2"});
        sendEventsAndReset(epService, listener, s3Events_1);
        Object[] s3Events_2 = SupportBean_S3.makeS3("D-s1-3", new String[]{"D-s3-3", "D-s3-4"});
        sendEventsAndReset(epService, listener, s3Events_2);

        s4Events = SupportBean_S4.makeS4("D-s1-2", new String[]{"D-s4-1", "D-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);
        s4Events = SupportBean_S4.makeS4("D-s1-3", new String[]{"D-s4-3", "D-s4-4"});
        sendEventsAndReset(epService, listener, s4Events);

        s5Events = SupportBean_S5.makeS5("D-s2-1", new String[]{"D-s5-1", "D-s5-2"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("D-s2-2", new String[]{"D-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events_2[0], s4Events[0], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events_2[0], s4Events[1], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events_2[1], s4Events[0], null, s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events_2[1], s4Events[1], null, s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[0], s3Events_1[0], null, null, s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[0], s3Events_1[1], null, null, s6Events[0]},
        }, getAndResetNewEvents(listener));

        // Test s1
        //
        s3Events = SupportBean_S3.makeS3("E-s1-1", new String[]{"E-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("E-s1-1", new String[]{"E-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1", "E-s0-2", "E-s0-3", "E-s0-4"});
        sendEvent(epService, s0Events);

        Object[] s2Events_1 = SupportBean_S2.makeS2("E-s0-1", new String[]{"E-s2-1", "E-s2-2"});
        sendEventsAndReset(epService, listener, s2Events_1);
        Object[] s2Events_2 = SupportBean_S2.makeS2("E-s0-3", new String[]{"E-s2-3", "E-s2-4"});
        sendEventsAndReset(epService, listener, s2Events_2);
        Object[] s2Events_3 = SupportBean_S2.makeS2("E-s0-4", new String[]{"E-s2-5", "E-s2-6"});
        sendEventsAndReset(epService, listener, s2Events_3);

        s5Events_1 = SupportBean_S5.makeS5("E-s2-2", new String[]{"E-s5-1", "E-s5-2"});
        sendEventsAndReset(epService, listener, s5Events_1);
        s5Events_2 = SupportBean_S5.makeS5("E-s2-4", new String[]{"E-s5-3"});
        sendEventsAndReset(epService, listener, s5Events_2);

        s6Events_1 = SupportBean_S6.makeS6("E-s2-2", new String[]{"E-s6-1"});
        sendEventsAndReset(epService, listener, s6Events_1);
        s6Events_2 = SupportBean_S6.makeS6("E-s2-5", new String[]{"E-s6-2"});
        sendEventsAndReset(epService, listener, s6Events_2);

        s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events_1[1], s3Events[0], s4Events[0], s5Events_1[0], s6Events_1[0]},
                {s0Events[0], s1Events[0], s2Events_1[1], s3Events[0], s4Events[0], s5Events_1[1], s6Events_1[0]},
                {s0Events[3], s1Events[0], s2Events_3[0], s3Events[0], s4Events[0], null, s6Events_2[0]},
        }, getAndResetNewEvents(listener));

        // Test s2
        //
        s5Events = SupportBean_S5.makeS5("F-s2-1", new String[]{"F-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("F-s2-1", new String[]{"F-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s2-1", "F-s2-2"});
        sendEventsAndReset(epService, listener, s0Events);

        s3Events_1 = SupportBean_S3.makeS3("F-s1-1", new String[]{"F-s3-1"});
        sendEventsAndReset(epService, listener, s3Events_1);
        s3Events_2 = SupportBean_S3.makeS3("F-s1-3", new String[]{"F-s3-2"});
        sendEventsAndReset(epService, listener, s3Events_2);

        s4Events = SupportBean_S4.makeS4("F-s1-1", new String[]{"F-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        Object[] s1Events_1 = SupportBean_S1.makeS1("F", new String[]{"F-s1-1"});
        sendEventsAndReset(epService, listener, s1Events_1);
        Object[] s1Events_2 = SupportBean_S1.makeS1("F", new String[]{"F-s1-2"});
        sendEventsAndReset(epService, listener, s1Events_2);
        Object[] s1Events_3 = SupportBean_S1.makeS1("F", new String[]{"F-s1-3"});
        sendEventsAndReset(epService, listener, s1Events_3);

        s2Events = SupportBean_S2.makeS2("F-s2-1", new String[]{"F-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events_1[0], s2Events[0], s3Events_1[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events_3[0], s2Events[0], s3Events_2[0], null, s5Events[0], s6Events[0]}
        }, getAndResetNewEvents(listener));

        // Test s3
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-3", "G-s1-2", "G-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s2-1", "G-s2-2"});
        sendEventsAndReset(epService, listener, s0Events);

        s6Events = SupportBean_S6.makeS6("G-s2-2", new String[]{"G-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("G-s2-2", new String[]{"G-s2-2"});
        sendEvent(epService, s2Events);

        s4Events = SupportBean_S4.makeS4("G-s1-2", new String[]{"G-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s3Events = SupportBean_S3.makeS3("G-s1-2", new String[]{"G-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}
        }, getAndResetNewEvents(listener));

        // Test s3
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-3", "H-s1-2", "H-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s4Events = SupportBean_S4.makeS4("H-s1-2", new String[]{"H-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s2-1", "H-s2-2", "H-s2-3"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events_1 = SupportBean_S2.makeS2("H-s2-2", new String[]{"H-s2-20"});
        sendEvent(epService, s2Events_1);
        s2Events_2 = SupportBean_S2.makeS2("H-s2-3", new String[]{"H-s2-30", "H-s2-31"});
        sendEvent(epService, s2Events_2);

        s6Events_1 = SupportBean_S6.makeS6("H-s2-20", new String[]{"H-s6-1"});
        sendEventsAndReset(epService, listener, s6Events_1);
        s6Events_2 = SupportBean_S6.makeS6("H-s2-31", new String[]{"H-s6-3", "H-s6-4"});
        sendEventsAndReset(epService, listener, s6Events_2);

        s3Events = SupportBean_S3.makeS3("H-s1-2", new String[]{"H-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[1], s2Events_1[0], s3Events[0], s4Events[0], null, s6Events_1[0]},
                {s0Events[2], s1Events[1], s2Events_2[1], s3Events[0], s4Events[0], null, s6Events_2[0]},
                {s0Events[2], s1Events[1], s2Events_2[1], s3Events[0], s4Events[0], null, s6Events_2[1]},
        }, getAndResetNewEvents(listener));

        // Test s4
        //
        s3Events = SupportBean_S3.makeS3("I-s1-3", new String[]{"I-s3-1"});
        sendEvent(epService, s3Events);

        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1", "I-s1-2", "I-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s2-1", "I-s2-2", "I-s2-3"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events_1 = SupportBean_S2.makeS2("I-s2-1", new String[]{"I-s2-20"});
        sendEvent(epService, s2Events_1);
        s2Events_2 = SupportBean_S2.makeS2("I-s2-2", new String[]{"I-s2-30", "I-s2-31"});
        sendEvent(epService, s2Events_2);

        s5Events = SupportBean_S5.makeS5("I-s2-30", new String[]{"I-s5-1", "I-s5-2"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("I-s2-30", new String[]{"I-s6-1", "I-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s4Events = SupportBean_S4.makeS4("I-s1-3", new String[]{"I-s4-1"});
        sendEvent(epService, s4Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[1], s1Events[2], s2Events_2[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[1], s1Events[2], s2Events_2[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[1], s1Events[2], s2Events_2[0], s3Events[0], s4Events[0], s5Events[0], s6Events[1]},
                {s0Events[1], s1Events[2], s2Events_2[0], s3Events[0], s4Events[0], s5Events[1], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("J-s2-30", new String[]{"J-s6-1", "J-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("J-s2-1", new String[]{"J-s2-30", "J-s2-31"});
        sendEvent(epService, s2Events);

        s5Events = SupportBean_S5.makeS5("J-s2-30", new String[]{"J-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, s2Events[0], null, null, s5Events[0], s6Events[0]},
                {null, null, s2Events[0], null, null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("K-s2-31", new String[]{"K-s6-1", "K-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("K", new String[]{"K-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events = SupportBean_S2.makeS2("K-s2-1", new String[]{"K-s2-30", "K-s2-31"});
        sendEvent(epService, s2Events);

        s5Events = SupportBean_S5.makeS5("K-s2-31", new String[]{"K-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("L-s2-31", new String[]{"L-s6-1", "L-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("L-s2-1", new String[]{"L-s2-30", "L-s2-31"});
        sendEvent(epService, s2Events);

        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("L", new String[]{"L-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s5Events = SupportBean_S5.makeS5("L-s2-31", new String[]{"L-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[1], null, null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("M-s2-31", new String[]{"M-s6-1", "M-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("M-s2-1", new String[]{"M-s2-30", "M-s2-31"});
        sendEvent(epService, s2Events);

        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events = SupportBean_S3.makeS3("M-s1-1", new String[]{"M-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events = SupportBean_S5.makeS5("M-s2-31", new String[]{"M-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("N-s2-31", new String[]{"N-s6-1", "N-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("N-s2-1", new String[]{"N-s2-30", "N-s2-31"});
        sendEvent(epService, s2Events);

        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1", "N-s1-2", "N-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events = SupportBean_S3.makeS3("N-s1-3", new String[]{"N-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events = SupportBean_S5.makeS5("N-s2-31", new String[]{"N-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[2], s2Events[1], s3Events[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events[0], null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s5
        //
        s6Events = SupportBean_S6.makeS6("O-s2-31", new String[]{"O-s6-1", "O-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("O-s2-1", new String[]{"O-s2-30", "O-s2-31"});
        sendEvent(epService, s2Events);

        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1", "O-s1-2", "O-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events_1 = SupportBean_S3.makeS3("O-s1-2", new String[]{"O-s3-1", "O-s3-2"});
        sendEventsAndReset(epService, listener, s3Events_1);
        s3Events_2 = SupportBean_S3.makeS3("O-s1-3", new String[]{"O-s3-3"});
        sendEventsAndReset(epService, listener, s3Events_2);

        s5Events = SupportBean_S5.makeS5("O-s2-31", new String[]{"O-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[1], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events_2[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[0], null, s5Events[0], s6Events[1]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[1], null, s5Events[0], s6Events[1]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events_2[0], null, s5Events[0], s6Events[1]}
        }, getAndResetNewEvents(listener));

        // Test s6
        //
        s5Events = SupportBean_S5.makeS5("P-s2-31", new String[]{"P-s5-1"});
        sendEvent(epService, s5Events);

        s2Events = SupportBean_S2.makeS2("P-s2-1", new String[]{"P-s2-30", "P-s2-31"});
        sendEvent(epService, s2Events);

        s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s2-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1", "P-s1-2", "P-s1-3"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events_1 = SupportBean_S3.makeS3("P-s1-2", new String[]{"P-s3-1", "P-s3-2"});
        sendEventsAndReset(epService, listener, s3Events_1);
        s3Events_2 = SupportBean_S3.makeS3("P-s1-3", new String[]{"P-s3-3"});
        sendEventsAndReset(epService, listener, s3Events_2);

        s6Events = SupportBean_S6.makeS6("P-s2-31", new String[]{"P-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events_1[1], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[2], s2Events[1], s3Events_2[0], null, s5Events[0], s6Events[0]}
        }, getAndResetNewEvents(listener));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionRoot_s0(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s1(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S1 + "#length(1000) as s1 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s3(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S3 + "#length(1000) as s3 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s3.p30 = s1.p10 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s4(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S4 + "#length(1000) as s4 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s4.p40 = s1.p10 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s5(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S5 + "#length(1000) as s5 " +
                " right outer join " + EVENT_S2 + "#length(1000) as s2 on s2.p20 = s5.p50 " +
                " right outer join " + EVENT_S6 + "#length(1000) as s6 on s2.p20 = s6.p60 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s6(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 -> s1
         *                  <- s3
         *                  -> s4
         *             <- s2
         *                  -> s5
         *                  <- s6
         */
        String epl = "select * from " +
                EVENT_S6 + "#length(1000) as s6 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s2.p20 = s6.p60 " +
                " left outer join " + EVENT_S5 + "#length(1000) as s5 on s2.p20 = s5.p50 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " right outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        Object[] s0Events, s1Events, s2Events, s3Events, s4Events, s5Events, s6Events;

        // Test s0 and s1=0, s2=0, s3=0, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(epService, s0Events);
        assertFalse(listener.isInvoked());

        // Test s0 and s1=0, s2=1, s3=0, s4=0, s5=0, s6=0
        //
        s2Events = SupportBean_S2.makeS2("B", new String[]{"B-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(epService, s0Events);
        assertFalse(listener.isInvoked());

        // Test s0 and s1=0, s2=1, s3=0, s4=0, s5=0, s6=1
        //
        s2Events = SupportBean_S2.makeS2("C", new String[]{"C-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s6Events = SupportBean_S6.makeS6("C", new String[]{"C-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=1, s4=0, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s2Events = SupportBean_S2.makeS2("D", new String[]{"D-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s3Events = SupportBean_S3.makeS3("D", new String[]{"D-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(listener));

        s5Events = SupportBean_S5.makeS5("D", new String[]{"D-s5-1"});
        sendEvent(epService, s5Events);
        assertFalse(listener.isInvoked());

        s6Events = SupportBean_S6.makeS6("D", new String[]{"D-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=1, s4=2, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("E", new String[]{"E-s2-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("E", new String[]{"E-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        s5Events = SupportBean_S5.makeS5("E", new String[]{"E-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("E", new String[]{"E-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=2, s3=1, s4=2, s5=1, s6=1
        //
        s1Events = SupportBean_S1.makeS1("F", new String[]{"F-s1-1", "F-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1", "F-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("F", new String[]{"F-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("F", new String[]{"F-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        s5Events = SupportBean_S5.makeS5("F", new String[]{"F-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("F", new String[]{"F-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=2, s4=2, s5=1, s6=2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("G", new String[]{"G-s3-1", "G-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("G", new String[]{"G-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        s5Events = SupportBean_S5.makeS5("G", new String[]{"G-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("G", new String[]{"G-s6-1", "G-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0], s5Events[0], s6Events[1]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=2, s3=1, s4=1, s5=2, s6=1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1", "H-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("H", new String[]{"H-s2-1", "H-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("H", new String[]{"H-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("H", new String[]{"H-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        s5Events = SupportBean_S5.makeS5("H", new String[]{"H-s5-1", "H-s5-2"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("H", new String[]{"H-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0], s5Events[1], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=1, s2=1, s3=1, s4=0, s5=1, s6=0
        //
        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-1"});
        sendEvent(epService, s0Events);

        s2Events = SupportBean_S2.makeS2("I", new String[]{"I-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("I", new String[]{"I-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events = SupportBean_S5.makeS5("I", new String[]{"I-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1", "I-s1-2"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());    // no s6

        // Test s1 and s0=1, s2=1, s3=1, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEvent(epService, s0Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("J", new String[]{"J-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events = SupportBean_S5.makeS5("J", new String[]{"J-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("J", new String[]{"J-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=1, s2=1, s3=1, s4=1, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("K", new String[]{"K-s0-1"});
        sendEvent(epService, s0Events);

        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("K", new String[]{"K-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("K", new String[]{"K-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s6Events = SupportBean_S6.makeS6("K", new String[]{"K-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=1, s1=0, s3=0, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s6Events = SupportBean_S6.makeS6("L", new String[]{"L-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("L", new String[]{"L-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=1, s1=1, s3=0, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s5Events = SupportBean_S5.makeS5("M", new String[]{"M-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("M", new String[]{"M-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=1, s1=1, s3=1, s4=0, s5=1, s6=1
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events = SupportBean_S3.makeS3("N", new String[]{"N-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s5Events = SupportBean_S5.makeS5("N", new String[]{"N-s5-1"});
        sendEventsAndReset(epService, listener, s5Events);

        s6Events = SupportBean_S6.makeS6("N", new String[]{"N-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s2Events = SupportBean_S2.makeS2("N", new String[]{"N-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("O", new String[]{"O-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(listener));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("O", new String[]{"O-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, null, null, s3Events[0], null, null, null}}, getAndResetNewEvents(listener));

        // Test s3 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[]{"P-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s6Events = SupportBean_S6.makeS6("P", new String[]{"P-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s3Events = SupportBean_S3.makeS3("P", new String[]{"P-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s3 and s0=1, s1=1, s2=1, s4=2, s5=2, s6=1
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[]{"Q-s0-1"});
        sendEvent(epService, s0Events);
        assertFalse(listener.isInvoked());

        s1Events = SupportBean_S1.makeS1("Q", new String[]{"Q-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s2Events = SupportBean_S2.makeS2("Q", new String[]{"Q-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s4Events = SupportBean_S4.makeS4("Q", new String[]{"Q-s4-1", "Q-s4-2"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        s5Events = SupportBean_S5.makeS5("Q", new String[]{"Q-s5-1", "Q-s5-2"});
        sendEvent(epService, s5Events);
        assertFalse(listener.isInvoked());

        s6Events = SupportBean_S6.makeS6("Q", new String[]{"Q-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[0], null, null, s5Events[1], s6Events[0]}}, getAndResetNewEvents(listener));

        s3Events = SupportBean_S3.makeS3("Q", new String[]{"Q-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], s5Events[1], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1], s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1], s5Events[1], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s4 and s0=1, s1=1, s2=0, s4=0, s5=0, s6=0
        //
        s0Events = SupportBean_S0.makeS0("R", new String[]{"R-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEvent(epService, s1Events);

        s4Events = SupportBean_S4.makeS4("R", new String[]{"R-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        // Test s4 and s0=2, s1=1, s2=1, s4=0, s5=0, s6=2
        //
        s0Events = SupportBean_S0.makeS0("S", new String[]{"S-s0-1", "S-s0-2"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1"});
        sendEvent(epService, s1Events);

        s2Events = SupportBean_S2.makeS2("S", new String[]{"S-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s6Events = SupportBean_S6.makeS6("S", new String[]{"S-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, null, s6Events[0]},
                {s0Events[1], null, s2Events[0], null, null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        s4Events = SupportBean_S4.makeS4("S", new String[]{"S-s4-1"});
        sendEvent(epService, s4Events);
        assertFalse(listener.isInvoked());

        // Test s4 and s0=1, s1=1, s2=1, s4=0, s5=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("T", new String[]{"T-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("T", new String[]{"T-s1-1"});
        sendEvent(epService, s1Events);

        s2Events = SupportBean_S2.makeS2("T", new String[]{"T-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("T", new String[]{"T-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s6Events = SupportBean_S6.makeS6("T", new String[]{"T-s6-1"});
        sendEvent(epService, s6Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, null, s6Events[0]}}, getAndResetNewEvents(listener));

        s4Events = SupportBean_S4.makeS4("T", new String[]{"T-s4-1"});
        sendEvent(epService, s4Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0], null, s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s5 and s0=1, s1=0, s2=1, s3=0, s4=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1"});
        sendEvent(epService, s0Events);

        s2Events = SupportBean_S2.makeS2("U", new String[]{"U-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s6Events = SupportBean_S6.makeS6("U", new String[]{"U-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s5Events = SupportBean_S5.makeS5("U", new String[]{"U-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        // Test s6 and s0=1, s1=2, s2=1, s3=0, s4=0, s6=2
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1"});
        sendEvent(epService, s1Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s6Events = SupportBean_S6.makeS6("V", new String[]{"V-s6-1", "V-s6-2"});
        sendEventsAndReset(epService, listener, s6Events);

        s5Events = SupportBean_S5.makeS5("V", new String[]{"V-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[0]},
                {s0Events[0], null, s2Events[0], null, null, s5Events[0], s6Events[1]}}, getAndResetNewEvents(listener));

        // Test s5 and s0=1, s1=2, s2=1, s3=1, s4=0, s6=1
        //
        s0Events = SupportBean_S0.makeS0("W", new String[]{"W-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("W", new String[]{"W-s1-1", "W-s1-2"});
        sendEvent(epService, s1Events);

        s2Events = SupportBean_S2.makeS2("W", new String[]{"W-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("W", new String[]{"W-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s6Events = SupportBean_S6.makeS6("W", new String[]{"W-s6-1"});
        sendEventsAndReset(epService, listener, s6Events);

        s5Events = SupportBean_S5.makeS5("W", new String[]{"W-s5-1"});
        sendEvent(epService, s5Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null, s5Events[0], s6Events[0]}}, getAndResetNewEvents(listener));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void sendEventsAndReset(EPServiceProvider epService, SupportUpdateListener listener, Object[] events) {
        sendEvent(epService, events);
        listener.reset();
    }

    private void sendEvent(EPServiceProvider epService, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }

    private Object[][] getAndResetNewEvents(SupportUpdateListener listener) {
        EventBean[] newEvents = listener.getLastNewData();
        listener.reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2", "s3", "s4", "s5", "s6"});
    }
}
