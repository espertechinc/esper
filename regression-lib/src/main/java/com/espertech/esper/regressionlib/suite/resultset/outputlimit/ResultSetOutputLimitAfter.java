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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportOutputLimitOpt;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSetOutputLimitAfter {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAfterWithOutputLast());
        execs.add(new ResultSetEveryPolicy());
        execs.add(new ResultSetMonthScoped());
        execs.add(new ResultSetDirectNumberOfEvents());
        execs.add(new ResultSetDirectTimePeriod());
        execs.add(new ResultSetSnapshotVariable());
        execs.add(new ResultSetOutputWhenThen());
        return execs;
    }

    private static class ResultSetAfterWithOutputLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (SupportOutputLimitOpt outputLimitOpt : SupportOutputLimitOpt.values()) {
                runAssertionAfterWithOutputLast(env, outputLimitOpt);
            }
        }
    }

    private static class ResultSetEveryPolicy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            sendTimer(env, 0);
            String stmtText = "select theString from SupportBean#keepall output after 0 days 0 hours 0 minutes 20 seconds 0 milliseconds every 0 days 0 hours 0 minutes 5 seconds 0 milliseconds";
            env.compileDeploy("@name('s0') " + stmtText).addListener("s0");

            tryAssertionEveryPolicy(env, milestone);

            env.undeployAll();

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("theString"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("keepall")));
            model.setOutputLimitClause(OutputLimitClause.create(Expressions.timePeriod(0, 0, 0, 5, 0)).afterTimePeriodExpression(Expressions.timePeriod(0, 0, 0, 20, 0)));
            Assert.assertEquals(stmtText, model.toEPL());
        }
    }

    private static class ResultSetMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendCurrentTime(env, "2002-02-01T09:00:00.000");

            String epl = "@name('s0') select * from SupportBean output after 1 month";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            env.sendEventBean(new SupportBean("E2", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            env.sendEventBean(new SupportBean("E3", 3));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E3"});

            env.undeployAll();
        }
    }

    private static class ResultSetDirectNumberOfEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString".split(",");
            String stmtText = "@name('s0') select theString from SupportBean#keepall output after 3 events";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "E1");

            env.milestone(0);

            sendEvent(env, "E2");
            sendEvent(env, "E3");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendEvent(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            sendEvent(env, "E5");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            env.undeployAll();

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("theString"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBean").addView("keepall")));
            model.setOutputLimitClause(OutputLimitClause.createAfter(3));
            Assert.assertEquals("select theString from SupportBean#keepall output after 3 events ", model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));

            env.compileDeploy(model).addListener("s0");

            sendEvent(env, "E1");
            sendEvent(env, "E2");
            sendEvent(env, "E3");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            sendEvent(env, "E5");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            model = env.eplToModel("select theString from SupportBean#keepall output after 3 events");
            Assert.assertEquals("select theString from SupportBean#keepall output after 3 events ", model.toEPL());

            env.undeployAll();
        }
    }

    private static class ResultSetDirectTimePeriod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String[] fields = "theString".split(",");
            String stmtText = "@name('s0') select theString from SupportBean#keepall output after 20 seconds";
            env.compileDeploy(stmtText).addListener("s0");

            env.milestone(0);

            sendTimer(env, 1);
            sendEvent(env, "E1");

            env.milestone(1);

            sendTimer(env, 6000);
            sendEvent(env, "E2");

            env.milestone(2);

            sendTimer(env, 19999);
            sendEvent(env, "E3");
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendTimer(env, 20000);
            sendEvent(env, "E4");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E4"});

            sendTimer(env, 21000);
            sendEvent(env, "E5");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E5"});

            env.undeployAll();
        }
    }

    private static class ResultSetSnapshotVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int myvar_local = 1", path);

            sendTimer(env, 0);
            String stmtText = "@name('s0') select theString from SupportBean#keepall output after 20 seconds snapshot when myvar_local=1";
            env.compileDeploy(stmtText, path).addListener("s0");

            tryAssertionSnapshotVar(env);

            env.undeployModuleContaining("s0");

            env.eplToModelCompileDeploy(stmtText, path).undeployAll();
        }
    }

    private static class ResultSetOutputWhenThen implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create variable boolean myvar0 = false;\n" +
                "create variable boolean myvar1 = false;\n" +
                "create variable boolean myvar2 = false;\n" +
                "@Name('s0')\n" +
                "select a.* from SupportBean#time(10) a output after 3 events when myvar0=true then set myvar1=true, myvar2=true";
            env.compileDeploy(epl).addListener("s0");
            String depId = env.deploymentId("s0");

            sendEvent(env, "E1");
            sendEvent(env, "E2");
            sendEvent(env, "E3");
            assertFalse(env.listener("s0").isInvoked());

            env.runtime().getVariableService().setVariableValue(depId, "myvar0", true);
            sendEvent(env, "E4");
            assertTrue(env.listener("s0").isInvoked());

            Assert.assertEquals(true, env.runtime().getVariableService().getVariableValue(depId, "myvar1"));
            Assert.assertEquals(true, env.runtime().getVariableService().getVariableValue(depId, "myvar2"));

            env.undeployAll();
        }
    }

    private static void tryAssertionSnapshotVar(RegressionEnvironment env) {
        sendTimer(env, 6000);
        sendEvent(env, "E1");
        sendEvent(env, "E2");

        env.milestone(0);

        sendTimer(env, 19999);
        sendEvent(env, "E3");
        assertFalse(env.listener("s0").isInvoked());

        sendTimer(env, 20000);
        sendEvent(env, "E4");
        String[] fields = "theString".split(",");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});
        env.listener("s0").reset();

        env.milestone(1);

        sendTimer(env, 21000);
        sendEvent(env, "E5");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}, {"E5"}});
        env.listener("s0").reset();
    }

    private static void tryAssertionEveryPolicy(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "theString".split(",");
        sendTimer(env, 1);
        sendEvent(env, "E1");

        env.milestoneInc(milestone);

        sendTimer(env, 6000);
        sendEvent(env, "E2");

        env.milestoneInc(milestone);

        sendTimer(env, 16000);
        sendEvent(env, "E3");
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        sendTimer(env, 20000);
        sendEvent(env, "E4");
        assertFalse(env.listener("s0").isInvoked());

        sendTimer(env, 24999);
        sendEvent(env, "E5");

        env.milestoneInc(milestone);

        sendTimer(env, 25000);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E4"}, {"E5"}});
        env.listener("s0").reset();

        env.milestoneInc(milestone);

        sendTimer(env, 27000);
        sendEvent(env, "E6");

        sendTimer(env, 29999);
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        sendTimer(env, 30000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E6"});
    }

    private static void runAssertionAfterWithOutputLast(RegressionEnvironment env, SupportOutputLimitOpt opt) {
        String epl = opt.getHint() + "@name('s0') select sum(intPrimitive) as thesum " +
            "from SupportBean#keepall " +
            "output after 4 events last every 2 events";
        env.compileDeploy(epl).addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));
        env.sendEventBean(new SupportBean("E2", 20));

        env.milestone(0);

        env.sendEventBean(new SupportBean("E3", 30));
        env.sendEventBean(new SupportBean("E4", 40));

        env.milestone(1);

        env.sendEventBean(new SupportBean("E5", 50));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean("E6", 60));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "thesum".split(","), new Object[]{210});

        env.undeployAll();
    }

    private static void sendTimer(RegressionEnvironment env, long time) {
        env.advanceTime(time);
    }

    private static void sendEvent(RegressionEnvironment env, String theString) {
        env.sendEventBean(new SupportBean(theString, 0));
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }
}
