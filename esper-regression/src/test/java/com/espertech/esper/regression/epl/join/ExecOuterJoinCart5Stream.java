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

public class ExecOuterJoinCart5Stream implements RegressionExecution {
    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();
    private final static String EVENT_S3 = SupportBean_S3.class.getName();
    private final static String EVENT_S4 = SupportBean_S4.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionRoot_s0(epService);
        runAssertionRoot_s1(epService);
        runAssertionRoot_s1_order_2(epService);
        runAssertionRoot_s2(epService);
        runAssertionRoot_s2_order_2(epService);
        runAssertionRoot_s3(epService);
        runAssertionRoot_s3_order2(epService);
        runAssertionRoot_s4(epService);
        runAssertionRoot_s4_order2(epService);
    }

    private void runAssertionRoot_s0(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s1(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S1 + "#length(1000) as s1 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s1_order_2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S1 + "#length(1000) as s1 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s2_order_2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s3(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S3 + "#length(1000) as s3 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s3_order2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S3 + "#length(1000) as s3 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S4 + "#length(1000) as s4 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s4(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S4 + "#length(1000) as s4 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRoot_s4_order2(EPServiceProvider epService) {
        /**
         * Query:
         *          s0 <- s1
         *                  -> s2
         *                  -> s3
         *                  -> s4
         */
        String epl = "select * from " +
                EVENT_S4 + "#length(1000) as s4 " +
                " right outer join " + EVENT_S1 + "#length(1000) as s1 on s1.p10 = s4.p40 " +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                " left outer join " + EVENT_S3 + "#length(1000) as s3 on s1.p10 = s3.p30 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        Object[] s0Events;
        Object[] s1Events;
        Object[] s2Events;
        Object[] s3Events;
        Object[] s4Events;

        // Test s0 and s1=0, s2=0, s3=0, s4=0
        //
        s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(epService, s0Events);
        assertFalse(listener.isInvoked());

        // Test s0 and s1=1, s2=0, s3=0, s4=0
        //
        s1Events = SupportBean_S1.makeS1("B", new String[]{"B-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{null, s1Events[0], null, null, null}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{s0Events[0], s1Events[0], null, null, null}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=0, s4=0
        //
        s1Events = SupportBean_S1.makeS1("C", new String[]{"C-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("C", new String[]{"C-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{null, s1Events[0], s2Events[0], null, null}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{s0Events[0], s1Events[0], s2Events[0], null, null}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=1, s4=0
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("D", new String[]{"D-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("D", new String[]{"D-s2-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{null, s1Events[0], s2Events[0], s3Events[0], null}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], null}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=1, s4=1
        //
        s1Events = SupportBean_S1.makeS1("E", new String[]{"E-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("E", new String[]{"E-s2-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("E", new String[]{"E-s2-1"});
        sendEvent(epService, s4Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{null, s1Events[0], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=1, s3=1, s4=1
        //
        s1Events = SupportBean_S1.makeS1("F", new String[]{"F-s1-1", "F-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("F", new String[]{"F-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("F", new String[]{"F-s2-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=2, s3=1, s4=1
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1", "G-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1", "G-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("G", new String[]{"G-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("G", new String[]{"G-s2-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=2, s3=2, s4=1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1", "H-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("H", new String[]{"H-s2-1", "H-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("H", new String[]{"H-s3-1", "H-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("H", new String[]{"H-s2-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[1], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=2, s2=2, s3=2, s4=2
        //
        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1", "I-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("I", new String[]{"I-s2-1", "I-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("I", new String[]{"I-s3-1", "I-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("I", new String[]{"I-s4-1", "I-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[1], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[1], s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s0 and s1=1, s2=1, s3=2, s4=3
        //
        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("J", new String[]{"J-s3-1", "J-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("J", new String[]{"J-s4-1", "J-s4-2", "J-s4-3"});
        sendEventsAndReset(epService, listener, s4Events);

        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[2]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[2]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=1, s3=1, s4=1
        //
        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s3Events = SupportBean_S3.makeS3("K", new String[]{"K-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("K", new String[]{"K-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=1, s3=0, s4=1
        //
        s2Events = SupportBean_S2.makeS2("L", new String[]{"L-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s4Events = SupportBean_S4.makeS4("L", new String[]{"L-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("L", new String[]{"L-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], s2Events[0], null, s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=2, s2=1, s3=0, s4=1
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1", "M-s0-2"});
        sendEvent(epService, s0Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s4Events = SupportBean_S4.makeS4("M", new String[]{"M-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null, s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], null, s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=1, s2=0, s3=0, s4=0
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("N", new String[]{"N-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], null, null, null}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=0, s3=1, s4=0
        //
        s3Events = SupportBean_S3.makeS3("O", new String[]{"O-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], null, s3Events[0], null}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=0, s3=0, s4=1
        //
        s4Events = SupportBean_S4.makeS4("P", new String[]{"P-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], null, null, s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=0, s3=0, s4=2
        //
        s4Events = SupportBean_S4.makeS4("Q", new String[]{"Q-s4-1", "Q-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("Q", new String[]{"Q-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], null, null, s4Events[0]},
                {null, s1Events[0], null, null, s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=0, s3=2, s4=2
        //
        s3Events = SupportBean_S3.makeS3("R", new String[]{"R-s3-1", "R-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("R", new String[]{"R-s4-1", "R-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], null, s3Events[0], s4Events[0]},
                {null, s1Events[0], null, s3Events[1], s4Events[0]},
                {null, s1Events[0], null, s3Events[0], s4Events[1]},
                {null, s1Events[0], null, s3Events[1], s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s1 and s0=0, s2=2, s3=0, s4=2
        //
        s4Events = SupportBean_S4.makeS4("S", new String[]{"S-s4-1", "S-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s2Events = SupportBean_S2.makeS2("S", new String[]{"S-s2-1", "S-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], s2Events[0], null, s4Events[0]},
                {null, s1Events[0], s2Events[0], null, s4Events[1]},
                {null, s1Events[0], s2Events[1], null, s4Events[0]},
                {null, s1Events[0], s2Events[1], null, s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=1, s1=2, s3=0, s4=2
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("U", new String[]{"U-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s4Events = SupportBean_S4.makeS4("U", new String[]{"U-s4-1", "U-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s2Events = SupportBean_S2.makeS2("U", new String[]{"U-s1-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null, s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], null, s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=3, s1=1, s3=2, s4=1
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1", "V-s0-2", "V-s0-3"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events = SupportBean_S3.makeS3("V", new String[]{"V-s3-1", "V-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("V", new String[]{"V-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s1-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[2], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[2], s1Events[0], s2Events[0], s3Events[1], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 and s0=2, s1=2, s3=2, s4=1
        //
        s0Events = SupportBean_S0.makeS0("W", new String[]{"W-s0-1", "W-s0-2"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("W", new String[]{"W-s1-1", "W-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s3Events = SupportBean_S3.makeS3("W", new String[]{"W-s3-1", "W-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("W", new String[]{"W-s4-1", "W-s4-2"});
        sendEventsAndReset(epService, listener, s4Events);

        s2Events = SupportBean_S2.makeS2("W", new String[]{"W-s1-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0], s4Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[1]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[1], s4Events[1]}}, getAndResetNewEvents(listener));

        // Test s4 and s0=2, s1=2, s2=2, s3=2
        //
        s0Events = SupportBean_S0.makeS0("X", new String[]{"X-s0-1", "X-s0-2"});
        sendEvent(epService, s0Events);

        s1Events = SupportBean_S1.makeS1("X", new String[]{"X-s1-1", "X-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("X", new String[]{"X-s2-1", "X-s2-2"});
        sendEvent(epService, s2Events);

        s3Events = SupportBean_S3.makeS3("X", new String[]{"X-s3-1", "X-s3-2"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("X", new String[]{"X-s4-1"});
        sendEvent(epService, s4Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[1], s3Events[0], s4Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[0], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[1], s4Events[0]},
                {s0Events[1], s1Events[1], s2Events[1], s3Events[1], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s4 and s0=0, s1=1, s2=1, s3=1
        //
        s1Events = SupportBean_S1.makeS1("Y", new String[]{"Y-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("Y", new String[]{"Y-s2-1"});
        sendEvent(epService, s2Events);

        s3Events = SupportBean_S3.makeS3("Y", new String[]{"Y-s3-1"});
        sendEventsAndReset(epService, listener, s3Events);

        s4Events = SupportBean_S4.makeS4("Y", new String[]{"Y-s4-1"});
        sendEvent(epService, s4Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

        // Test s3 and s0=0, s1=2, s2=1, s4=1
        //
        s1Events = SupportBean_S1.makeS1("Z", new String[]{"Z-s1-1", "Z-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("Z", new String[]{"Z-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s4Events = SupportBean_S4.makeS4("Z", new String[]{"Z-s4-1"});
        sendEventsAndReset(epService, listener, s4Events);

        s3Events = SupportBean_S3.makeS3("Z", new String[]{"Z-s3-1"});
        sendEvent(epService, s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {null, s1Events[0], s2Events[0], s3Events[0], s4Events[0]},
                {null, s1Events[1], s2Events[0], s3Events[0], s4Events[0]}}, getAndResetNewEvents(listener));

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
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2", "s3", "s4"});
    }
}
