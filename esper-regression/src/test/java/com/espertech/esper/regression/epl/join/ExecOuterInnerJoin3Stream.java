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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import static org.junit.Assert.assertFalse;

public class ExecOuterInnerJoin3Stream implements RegressionExecution {
    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFullJoinVariantThree(epService);
        runAssertionFullJoinVariantTwo(epService);
        runAssertionFullJoinVariantOne(epService);
        runAssertionLeftJoinVariantThree(epService);
        runAssertionLeftJoinVariantTwo(epService);
        runAssertionRightJoinVariantOne(epService);
    }

    private void runAssertionFullJoinVariantThree(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S1 + "#keepall as s1 inner join " +
                EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20 " +
                "full outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10";

        tryAssertionFull(epService, joinStatement);
    }

    private void runAssertionFullJoinVariantTwo(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                "inner join " + EVENT_S1 + "#keepall as s1 on s1.p10 = s2.p20" +
                " full outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10";

        tryAssertionFull(epService, joinStatement);
    }

    private void runAssertionFullJoinVariantOne(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                "full outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10" +
                " inner join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20";

        tryAssertionFull(epService, joinStatement);
    }

    private void runAssertionLeftJoinVariantThree(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S1 + "#keepall as s1 left outer join " +
                EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                "inner join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20";

        tryAssertionFull(epService, joinStatement);
    }

    private void runAssertionLeftJoinVariantTwo(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                "inner join " + EVENT_S1 + "#keepall as s1 on s1.p10 = s2.p20" +
                " left outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10";

        tryAssertionFull(epService, joinStatement);
    }

    private void runAssertionRightJoinVariantOne(EPServiceProvider epService) {
        String joinStatement = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                "right outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10" +
                " inner join " + EVENT_S2 + "#length(1000) as s2 on s1.p10 = s2.p20";

        tryAssertionFull(epService, joinStatement);
    }

    private void tryAssertionFull(EPServiceProvider epService, String expression) {
        String[] fields = "s0.id, s0.p00, s1.id, s1.p10, s2.id, s2.p20".split(",");

        EPStatement joinView = SupportModelHelper.compileCreate(epService, expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        joinView.addListener(listener);

        // s1, s2, s0
        epService.getEPRuntime().sendEvent(new SupportBean_S1(100, "A_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(200, "A_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 100, "A_1", 200, "A_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{0, "A_1", 100, "A_1", 200, "A_1"});

        // s1, s0, s2
        epService.getEPRuntime().sendEvent(new SupportBean_S1(103, "D_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(203, "D_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 103, "D_1", 203, "D_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "D_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "D_1", 103, "D_1", 203, "D_1"});

        // s2, s1, s0
        epService.getEPRuntime().sendEvent(new SupportBean_S2(201, "B_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(101, "B_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, 101, "B_1", 201, "B_1"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "B_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "B_1", 101, "B_1", 201, "B_1"});

        // s2, s0, s1
        epService.getEPRuntime().sendEvent(new SupportBean_S2(202, "C_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "C_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(102, "C_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "C_1", 102, "C_1", 202, "C_1"});

        // s0, s1, s2
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "E_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(104, "E_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(204, "E_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, "E_1", 104, "E_1", 204, "E_1"});

        // s0, s2, s1
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "F_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(205, "F_1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(105, "F_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, "F_1", 105, "F_1", 205, "F_1"});
    }
}
