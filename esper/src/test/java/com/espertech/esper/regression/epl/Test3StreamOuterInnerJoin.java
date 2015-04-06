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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.bean.SupportBean_S2;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class Test3StreamOuterInnerJoin extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();

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

    public void testFullJoinVariantThree()
    {
        String joinStatement = "select * from " +
                EVENT_S1 + ".win:keepall() as s1 inner join " +
                EVENT_S2 + ".win:length(1000) as s2 on s1.p10 = s2.p20 " +
                " full outer join " + EVENT_S0 + ".win:length(1000) as s0 on s0.p00 = s1.p10";

        runAssertionFull(joinStatement);
    }

    public void testFullJoinVariantTwo()
    {
        String joinStatement = "select * from " +
                EVENT_S2 + ".win:length(1000) as s2 " +
               " inner join " + EVENT_S1 + ".win:keepall() s1 on s1.p10 = s2.p20" +
               " full outer join " + EVENT_S0 + ".win:length(1000) as s0 on s0.p00 = s1.p10";

        runAssertionFull(joinStatement);
    }

    public void testFullJoinVariantOne()
    {
        String joinStatement = "select * from " +
                EVENT_S0 + ".win:length(1000) as s0 " +
            " full outer join " + EVENT_S1 + ".win:length(1000) as s1 on s0.p00 = s1.p10" +
            " inner join " + EVENT_S2 + ".win:length(1000) as s2 on s1.p10 = s2.p20";

        runAssertionFull(joinStatement);
    }

    public void testLeftJoinVariantThree()
    {
        String joinStatement = "select * from " +
                EVENT_S1 + ".win:keepall() as s1 left outer join " +
                EVENT_S0 + ".win:length(1000) as s0 on s0.p00 = s1.p10 " +
                "inner join " + EVENT_S2 + ".win:length(1000) as s2 on s1.p10 = s2.p20";

        runAssertionFull(joinStatement);
    }

    public void testLeftJoinVariantTwo()
    {
        String joinStatement = "select * from " +
                EVENT_S2 + ".win:length(1000) as s2 " +
               " inner join " + EVENT_S1 + ".win:keepall() s1 on s1.p10 = s2.p20" +
               " left outer join " + EVENT_S0 + ".win:length(1000) as s0 on s0.p00 = s1.p10";

        runAssertionFull(joinStatement);
    }

    public void testRightJoinVariantOne()
    {
        String joinStatement = "select * from " +
                EVENT_S0 + ".win:length(1000) as s0 " +
            " right outer join " + EVENT_S1 + ".win:length(1000) as s1 on s0.p00 = s1.p10" +
            " inner join " + EVENT_S2 + ".win:length(1000) as s2 on s1.p10 = s2.p20";

        runAssertionFull(joinStatement);
    }

    public void runAssertionFull(String expression)
    {
        String fields[] = "s0.id, s0.p00, s1.id, s1.p10, s2.id, s2.p20".split(",");

        EPStatement joinView = epService.getEPAdministrator().createEPL(expression);
        joinView.addListener(updateListener);

        // s1, s2, s0
        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "A_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(200, "A_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 100, "A_1", 200, "A_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{0, "A_1", 100, "A_1", 200, "A_1"});

        // s1, s0, s2
        epService.getEPRuntime().sendEvent(new SupportBean_S1(103, "D_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(203, "D_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 103, "D_1", 203, "D_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "D_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{3, "D_1", 103, "D_1", 203, "D_1"});

        // s2, s1, s0
        epService.getEPRuntime().sendEvent(new SupportBean_S2(201, "B_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "B_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 101, "B_1", 201, "B_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "B_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{1, "B_1", 101, "B_1", 201, "B_1"});

        // s2, s0, s1
        epService.getEPRuntime().sendEvent(new SupportBean_S2(202, "C_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "C_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "C_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{2, "C_1", 102, "C_1", 202, "C_1"});

        // s0, s1, s2
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "E_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(104, "E_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(204, "E_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{4, "E_1", 104, "E_1", 204, "E_1"});

        // s0, s2, s1
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "F_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(205, "F_1"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(105, "F_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{5, "F_1", 105, "F_1", 205, "F_1"});
    }
}
