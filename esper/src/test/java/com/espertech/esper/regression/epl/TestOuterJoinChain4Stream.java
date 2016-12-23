/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.bean.SupportBean_S3;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.ArrayHandlingUtil;
import junit.framework.TestCase;

public class TestOuterJoinChain4Stream extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();
    private final static String EVENT_S3 = SupportBean_S3.class.getName();

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testLeftOuterJoin_root_s0()
    {
        /**
         * Query:
         *          s0
         *             -> s1
         *                  -> s2
         *                      -> s3
         */
        String joinStatement = "select * from " +
                                  EVENT_S0 + "#length(1000) as s0 " +
            " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
            " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
            " left outer join " + EVENT_S3 + "#length(1000) as s3 on s2.p20 = s3.p30 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testLeftOuterJoin_root_s1()
    {
        /**
         * Query:
         *          s0
         *             -> s1
         *                  -> s2
         *                      -> s3
         */
        String joinStatement = "select * from " +
                                  EVENT_S1 + "#length(1000) as s1 " +
            " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
            " left outer join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
            " left outer join " + EVENT_S3 + "#length(1000) as s3 on s2.p20 = s3.p30 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testLeftOuterJoin_root_s2()
    {
        /**
         * Query:
         *          s0
         *             -> s1
         *                  -> s2
         *                      -> s3
         */
        String joinStatement = "select * from " +
                                  EVENT_S2 + "#length(1000) as s2 " +
            " right outer join " + EVENT_S1 + "#length(1000) as s1 on s2.p20 = s1.p10 " +
            " right outer join " + EVENT_S0 + "#length(1000) as s0 on s1.p10 = s0.p00 " +
            " left outer join " + EVENT_S3 + "#length(1000) as s3 on s2.p20 = s3.p30 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testLeftOuterJoin_root_s3()
    {
        /**
         * Query:
         *          s0
         *             -> s1
         *                  -> s2
         *                      -> s3
         */
        String joinStatement = "select * from " +
                                  EVENT_S3 + "#length(1000) as s3 " +
            " right outer join " + EVENT_S2 + "#length(1000) as s2 on s3.p30 = s2.p20 " +
            " right outer join " + EVENT_S1 + "#length(1000) as s1 on s2.p20 = s1.p10 " +
            " right outer join " + EVENT_S0 + "#length(1000) as s0 on s1.p10 = s0.p00 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    private void runAsserts()
    {
        Object[] s0Events, s1Events, s2Events, s3Events;

        // Test s0 and s1=1, s2=1, s3=1
        //
        s1Events = SupportBean_S1.makeS1("A", new String[] {"A-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("A", new String[] {"A-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("A", new String[] {"A-s3-1"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("A", new String[] {"A-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{s0Events[0], s1Events[0], s2Events[0], s3Events[0]}}, getAndResetNewEvents());

        // Test s0 and s1=1, s2=0, s3=0
        //
        s1Events = SupportBean_S1.makeS1("B", new String[] {"B-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("B", new String[] {"B-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{s0Events[0], s1Events[0], null, null}}, getAndResetNewEvents());

        // Test s0 and s1=1, s2=1, s3=0
        //
        s1Events = SupportBean_S1.makeS1("C", new String[] {"C-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("C", new String[] {"C-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("C", new String[] {"C-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(
                new Object[][]{{s0Events[0], s1Events[0], s2Events[0], null}}, getAndResetNewEvents());

        // Test s0 and s1=2, s2=0, s3=0
        //
        s1Events = SupportBean_S1.makeS1("D", new String[] {"D-s1-1", "D-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("D", new String[] {"D-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("D", new String[] {"D-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null},
                {s0Events[0], s1Events[1], s2Events[0], null}}, getAndResetNewEvents());

        // Test s0 and s1=2, s2=2, s3=0
        //
        s1Events = SupportBean_S1.makeS1("E", new String[] {"E-s1-1", "E-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("E", new String[] {"E-s2-1", "E-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("E", new String[] {"E-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null},
                {s0Events[0], s1Events[1], s2Events[0], null},
                {s0Events[0], s1Events[0], s2Events[1], null},
                {s0Events[0], s1Events[1], s2Events[1], null}}, getAndResetNewEvents());

        // Test s0 and s1=2, s2=2, s3=1
        //
        s1Events = SupportBean_S1.makeS1("F", new String[] {"F-s1-1", "F-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("F", new String[] {"F-s2-1", "F-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("F", new String[] {"F-s3-1"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("F", new String[] {"F-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0]}}, getAndResetNewEvents());

        // Test s0 and s1=2, s2=2, s3=2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[] {"G-s1-1", "G-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("G", new String[] {"G-s2-1", "G-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("G", new String[] {"G-s3-1", "G-s3-2"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("G", new String[] {"G-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[1]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[1]}}, getAndResetNewEvents());

        // Test s0 and s1=1, s2=1, s3=3
        //
        s1Events = SupportBean_S1.makeS1("H", new String[] {"H-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("H", new String[] {"H-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("H", new String[] {"H-s3-1", "H-s3-2", "H-s3-3"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("H", new String[] {"H-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[2]}}, getAndResetNewEvents());

        // Test s3 and s0=0, s1=0, s2=0
        //
        s3Events = SupportBean_S3.makeS3("I", new String[] {"I-s3-1"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        // Test s3 and s0=0, s1=0, s2=1
        //
        s2Events = SupportBean_S2.makeS2("J", new String[] {"J-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("J", new String[] {"J-s3-1"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        // Test s3 and s0=0, s1=1, s2=1
        //
        s2Events = SupportBean_S2.makeS2("K", new String[] {"K-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s1Events = SupportBean_S1.makeS1("K", new String[] {"K-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s3Events = SupportBean_S3.makeS3("K", new String[] {"K-s3-1"});
        sendEvent(s3Events);
        assertFalse(updateListener.isInvoked());

        // Test s3 and s0=1, s1=1, s2=1
        //
        s0Events = SupportBean_S0.makeS0("M", new String[] {"M-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[] {"M-s1-1"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("M", new String[] {"M-s2-1"});
        sendEventsAndReset(s2Events);

        s3Events = SupportBean_S3.makeS3("M", new String[] {"M-s3-1"});
        sendEvent(s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]}}, getAndResetNewEvents());

        // Test s3 and s0=1, s1=2, s2=1
        //
        s0Events = SupportBean_S0.makeS0("N", new String[] {"N-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("N", new String[] {"N-s1-1", "N-s1-2"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("N", new String[] {"N-s2-1"});
        sendEventsAndReset(s2Events);

        s3Events = SupportBean_S3.makeS3("N", new String[] {"N-s3-1"});
        sendEvent(s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]}}, getAndResetNewEvents());

        // Test s3 and s0=1, s1=2, s2=3
        //
        s0Events = SupportBean_S0.makeS0("O", new String[] {"O-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[] {"O-s1-1", "O-s1-2"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[] {"O-s2-1", "O-s2-2", "O-s2-3"});
        sendEventsAndReset(s2Events);

        s3Events = SupportBean_S3.makeS3("O", new String[] {"O-s3-1"});
        sendEvent(s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[2], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[2], s3Events[0]}}, getAndResetNewEvents());

        // Test s3 and s0=2, s1=2, s2=3
        //
        s0Events = SupportBean_S0.makeS0("P", new String[] {"P-s0-1", "P-s0-2"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[] {"P-s1-1", "P-s1-2"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[] {"P-s2-1", "P-s2-2", "P-s2-3"});
        sendEventsAndReset(s2Events);

        s3Events = SupportBean_S3.makeS3("P", new String[] {"P-s3-1"});
        sendEvent(s3Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[1], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[2], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[2], s3Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[1], s1Events[0], s2Events[1], s3Events[0]},
                {s0Events[1], s1Events[1], s2Events[1], s3Events[0]},
                {s0Events[1], s1Events[0], s2Events[2], s3Events[0]},
                {s0Events[1], s1Events[1], s2Events[2], s3Events[0]}}, getAndResetNewEvents());

        // Test s1 and s0=0, s2=1, s3=0
        //
        s2Events = SupportBean_S2.makeS2("Q", new String[] {"Q-s2-1"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("Q", new String[] {"Q-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        // Test s1 and s0=2, s2=1, s3=0
        //
        s0Events = SupportBean_S0.makeS0("R", new String[] {"R-s0-1", "R-s0-2"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("R", new String[] {"R-s2-1"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("R", new String[] {"R-s1-1"});
        sendEvent(s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null},
                {s0Events[1], s1Events[0], s2Events[0], null}}, getAndResetNewEvents());

        // Test s1 and s0=2, s2=2, s3=2
        //
        s0Events = SupportBean_S0.makeS0("S", new String[] {"S-s0-1", "S-s0-2"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("S", new String[] {"S-s2-1"});
        sendEventsAndReset(s2Events);

        s3Events = SupportBean_S3.makeS3("S", new String[] {"S-s3-1", "S-s3-1"});
        sendEventsAndReset(s3Events);

        s1Events = SupportBean_S1.makeS1("S", new String[] {"S-s1-1"});
        sendEvent(s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1]}}, getAndResetNewEvents());

        // Test s2 and s0=0, s1=0, s3=1
        //
        s3Events = SupportBean_S3.makeS3("T", new String[] {"T-s3-1"});
        sendEventsAndReset(s3Events);

        s2Events = SupportBean_S2.makeS2("T", new String[] {"T-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        // Test s2 and s0=0, s1=1, s3=1
        //
        s3Events = SupportBean_S3.makeS3("U", new String[] {"U-s3-1"});
        sendEventsAndReset(s3Events);

        s1Events = SupportBean_S1.makeS1("U", new String[] {"U-s1-1"});
        sendEvent(s1Events);

        s2Events = SupportBean_S2.makeS2("U", new String[] {"U-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        // Test s2 and s0=1, s1=1, s3=1
        //
        s0Events = SupportBean_S0.makeS0("V", new String[] {"V-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("V", new String[] {"V-s1-1"});
        sendEvent(s1Events);

        s3Events = SupportBean_S3.makeS3("V", new String[] {"V-s3-1"});
        sendEventsAndReset(s3Events);

        s2Events = SupportBean_S2.makeS2("V", new String[] {"V-s2-1"});
        sendEvent(s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]}}, getAndResetNewEvents());

        // Test s2 and s0=2, s1=2, s3=0
        //
        s0Events = SupportBean_S0.makeS0("W", new String[] {"W-s0-1", "W-s0-2"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("W", new String[] {"W-s1-1", "W-s1-2"});
        sendEvent(s1Events);

        s2Events = SupportBean_S2.makeS2("W", new String[] {"W-s2-1"});
        sendEvent(s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], null},
                {s0Events[0], s1Events[1], s2Events[0], null},
                {s0Events[1], s1Events[0], s2Events[0], null},
                {s0Events[1], s1Events[1], s2Events[0], null}}, getAndResetNewEvents());

        // Test s2 and s0=2, s1=2, s3=2
        //
        s0Events = SupportBean_S0.makeS0("X", new String[] {"X-s0-1", "X-s0-2"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("X", new String[] {"X-s1-1", "X-s1-2"});
        sendEvent(s1Events);

        s3Events = SupportBean_S3.makeS3("X", new String[] {"X-s3-1", "X-s3-2"});
        sendEventsAndReset(s3Events);

        s2Events = SupportBean_S2.makeS2("X", new String[] {"X-s2-1"});
        sendEvent(s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[0]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[0]},
                {s0Events[0], s1Events[0], s2Events[0], s3Events[1]},
                {s0Events[0], s1Events[1], s2Events[0], s3Events[1]},
                {s0Events[1], s1Events[0], s2Events[0], s3Events[1]},
                {s0Events[1], s1Events[1], s2Events[0], s3Events[1]}}, getAndResetNewEvents());
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventsAndReset(Object[] events)
    {
        sendEvent(events);
        updateListener.reset();
    }

    private void sendEvent(Object[] events)
    {
        for (int i = 0; i < events.length; i++)
        {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }

    private Object[][] getAndResetNewEvents()
    {
        EventBean[] newEvents = updateListener.getLastNewData();
        updateListener.reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[] {"s0", "s1", "s2","s3"});
    }
}
