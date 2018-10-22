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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class RowRecogIntervalOrTerminated implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        AtomicInteger milestone = new AtomicInteger();
        runAssertionDocSample(env, milestone);

        runAssertion_A_Bstar(env, milestone, false);

        runAssertion_A_Bstar(env, milestone, true);

        runAssertion_Astar(env, milestone);

        runAssertion_A_Bplus(env, milestone);

        runAssertion_A_Bstar_or_Cstar(env, milestone);

        runAssertion_A_B_Cstar(env, milestone);

        runAssertion_A_B(env, milestone);

        runAssertion_A_Bstar_or_C(env, milestone);

        runAssertion_A_parenthesisBstar(env, milestone);
    }

    private void runAssertion_A_Bstar_or_C(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a,b0,b1,b2,c".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2, C.theString as c " +
            " pattern (A (B* | C))" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like 'A%'," +
            " B as B.theString like 'B%'," +
            " C as C.theString like 'C%'" +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("C1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", null, null, null, "C1"});

        env.sendEventBean(new SupportRecogBean("A2"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", null, null, null, null});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("B2"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B1", "B2", null, null});

        env.sendEventBean(new SupportRecogBean("A3"));
        sendTimer(env, 10000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", null, null, null, null});

        sendTimer(env, Integer.MAX_VALUE);
        assertFalse(env.listener("s0").isInvoked());

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_B(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        // the interval is not effective
        String[] fields = "a,b".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B.theString as b" +
            " pattern (A B)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like 'A%'," +
            " B as B.theString like 'B%'" +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1"});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("A3"));
        env.sendEventBean(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", "B2"});

        // destroy
        env.undeployAll();
    }

    private void runAssertionDocSample(RegressionEnvironment env, AtomicInteger milestone) {
        sendTimer(env, 0);

        String[] fields = "a_id,count_b,first_b,last_b".split(",");
        String text = "@name('s0') select * from TemperatureSensorEvent\n" +
            "match_recognize (\n" +
            "  partition by device\n" +
            "  measures A.id as a_id, count(B.id) as count_b, first(B.id) as first_b, last(B.id) as last_b\n" +
            "  pattern (A B*)\n" +
            "  interval 5 seconds or terminated\n" +
            "  define\n" +
            "    A as A.temp > 100,\n" +
            "    B as B.temp > 100)";

        env.compileDeploy(text).addListener("s0");

        sendTemperatureEvent(env, "E1", 1, 98);

        env.milestoneInc(milestone);

        sendTemperatureEvent(env, "E2", 1, 101);
        sendTemperatureEvent(env, "E3", 1, 102);
        sendTemperatureEvent(env, "E4", 1, 101);   // falls below
        assertFalse(env.listener("s0").isInvoked());

        sendTemperatureEvent(env, "E5", 1, 100);   // falls below
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 2L, "E3", "E4"});

        env.milestoneInc(milestone);

        sendTimer(env, Integer.MAX_VALUE);
        assertFalse(env.listener("s0").isInvoked());

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_B_Cstar(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a,b,c0,c1,c2".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B.theString as b, " +
            "C[0].theString as c0, C[1].theString as c1, C[2].theString as c2 " +
            " pattern (A B C*)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like 'A%'," +
            " B as B.theString like 'B%'," +
            " C as C.theString like 'C%'" +
            ")";
        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("B1"));
        env.sendEventBean(new SupportRecogBean("C1"));
        env.sendEventBean(new SupportRecogBean("C2"));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", "C1", "C2", null});

        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("X1"));
        env.sendEventBean(new SupportRecogBean("B3"));
        env.sendEventBean(new SupportRecogBean("X2"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportRecogBean("A3"));
        env.sendEventBean(new SupportRecogBean("B4"));
        env.sendEventBean(new SupportRecogBean("X3"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", "B4", null, null, null});

        env.milestoneInc(milestone);

        sendTimer(env, 20000);
        env.sendEventBean(new SupportRecogBean("A4"));
        env.sendEventBean(new SupportRecogBean("B5"));
        env.sendEventBean(new SupportRecogBean("C3"));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        sendTimer(env, 30000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A4", "B5", "C3", null, null});

        sendTimer(env, Integer.MAX_VALUE);
        assertFalse(env.listener("s0").isInvoked());

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_Bstar_or_Cstar(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a,b0,b1,c0,c1".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, " +
            "B[0].theString as b0, B[1].theString as b1, " +
            "C[0].theString as c0, C[1].theString as c1 " +
            " pattern (A (B* | C*))" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like 'A%'," +
            " B as B.theString like 'B%'," +
            " C as C.theString like 'C%'" +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", null, null, null, null});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("C1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", null, null, null, null});

        env.sendEventBean(new SupportRecogBean("B1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
            new Object[][]{{"A2", null, null, "C1", null}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("C2"));
        assertFalse(env.listener("s0").isInvoked());

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_Bplus(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
            " pattern (A B+)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like 'A%'," +
            " B as B.theString like 'B%'" +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("X1"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("B2"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("X2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});

        env.sendEventBean(new SupportRecogBean("A3"));
        env.sendEventBean(new SupportRecogBean("A4"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportRecogBean("B3"));
        env.sendEventBean(new SupportRecogBean("B4"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("X3", -1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A4", "B3", "B4", null});

        // destroy
        env.undeployAll();
    }

    private void runAssertion_Astar(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a0,a1,a2,a3,a4".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, A[3].theString as a3, A[4].theString as a4" +
            " pattern (A*)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as theString like 'A%'" +
            ")";

        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("A2"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("B1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "A2", null, null, null});

        env.milestoneInc(milestone);

        sendTimer(env, 2000);
        env.sendEventBean(new SupportRecogBean("A3"));
        env.sendEventBean(new SupportRecogBean("A4"));
        env.sendEventBean(new SupportRecogBean("A5"));
        assertFalse(env.listener("s0").isInvoked());
        sendTimer(env, 12000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", "A4", "A5", null, null});

        env.sendEventBean(new SupportRecogBean("A6"));
        env.sendEventBean(new SupportRecogBean("B2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A3", "A4", "A5", "A6", null});
        env.sendEventBean(new SupportRecogBean("B3"));
        assertFalse(env.listener("s0").isInvoked());

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_Bstar(RegressionEnvironment env, AtomicInteger milestone, boolean allMatches) {

        sendTimer(env, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
            (allMatches ? " all matches" : "") +
            " pattern (A B*)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like \"A%\"," +
            " B as B.theString like \"B%\"" +
            ")";

        env.compileDeploy(text).addListener("s0");

        // test output by terminated because of misfit event
        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("B1"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("X1"));
        if (!allMatches) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", null, null});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A1", "B1", null, null}, {"A1", null, null, null}});
        }

        env.milestoneInc(milestone);

        sendTimer(env, 20000);
        assertFalse(env.listener("s0").isInvoked());

        // test output by timer expiry
        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("B2"));
        assertFalse(env.listener("s0").isInvoked());
        sendTimer(env, 29999);

        sendTimer(env, 30000);
        if (!allMatches) {
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});
        } else {
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A2", "B2", null, null}, {"A2", null, null, null}});
        }

        // destroy
        env.undeployAll();
    }

    private void runAssertion_A_parenthesisBstar(RegressionEnvironment env, AtomicInteger milestone) {

        sendTimer(env, 0);

        String[] fields = "a,b0,b1,b2".split(",");
        String text = "@name('s0') select * from SupportRecogBean#keepall " +
            "match_recognize (" +
            " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, B[2].theString as b2" +
            " pattern (A (B)*)" +
            " interval 10 seconds or terminated" +
            " define" +
            " A as A.theString like \"A%\"," +
            " B as B.theString like \"B%\"" +
            ")";

        env.compileDeploy(text).addListener("s0");

        // test output by terminated because of misfit event
        env.sendEventBean(new SupportRecogBean("A1"));
        env.sendEventBean(new SupportRecogBean("B1"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportRecogBean("X1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A1", "B1", null, null});

        env.milestoneInc(milestone);

        sendTimer(env, 20000);
        assertFalse(env.listener("s0").isInvoked());

        // test output by timer expiry
        env.sendEventBean(new SupportRecogBean("A2"));
        env.sendEventBean(new SupportRecogBean("B2"));
        assertFalse(env.listener("s0").isInvoked());
        sendTimer(env, 29999);

        sendTimer(env, 30000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2", null, null});

        // destroy
        env.undeployAll();
    }

    private void sendTemperatureEvent(RegressionEnvironment env, String id, int device, double temp) {
        env.sendEventObjectArray(new Object[]{id, device, temp}, "TemperatureSensorEvent");
    }

    private void sendTimer(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }
}