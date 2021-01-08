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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.client.scopetest.EPAssertionUtil.assertPropsPerRow;
import static com.espertech.esper.common.client.util.DateTime.parseDefaultMSec;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ClientStageAdvanceTime {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageAdvanceWindowTime());
        execs.add(new ClientStageAdvanceWindowTimeBatch());
        execs.add(new ClientStageAdvanceRelativeTime());
        execs.add(new ClientStageCurrentTime());
        return execs;
    }

    private static class ClientStageCurrentTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            advanceTime(env, null, "2002-05-30T09:00:00.000");
            env.stageService().getStage("ST");
            assertEquals(parseDefaultMSec("2002-05-30T09:00:00.000"), env.stageService().getStage("ST").getEventService().getCurrentTime());

            advanceTime(env, "ST", "2002-05-30T09:00:05.000");
            assertEquals(parseDefaultMSec("2002-05-30T09:00:05.000"), env.stageService().getStage("ST").getEventService().getCurrentTime());

            env.milestone(0);

            assertEquals(parseDefaultMSec("2002-05-30T09:00:05.000"), env.stageService().getStage("ST").getEventService().getCurrentTime());

            env.undeployAll();
        }
    }

    private static class ClientStageAdvanceRelativeTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            advanceTime(env, null, "2002-05-30T09:00:00.000");

            String epl = "@name('s0') select * from pattern[timer:interval(10 seconds)];\n";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            env.stageService().getStage("ST");
            advanceTime(env, "ST", "2002-05-30T09:00:05.000");

            stageIt(env, "ST", deploymentId);

            advanceTime(env, "ST", "2002-05-30T09:00:10.000");
            env.listenerStage("ST", "s0").assertOneGetNewAndReset();

            unstageIt(env, "ST", deploymentId);

            env.undeployAll();
        }
    }

    private static class ClientStageAdvanceWindowTimeBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            advanceTime(env, null, "2002-05-30T09:00:00.000");

            String epl = "@name('s0') select * from SupportBean#time_batch(10)";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");
            String[] fields = new String[]{"theString"};

            env.sendEventBean(new SupportBean("E1", 1));

            env.milestone(0);

            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            env.milestone(1);

            advanceTime(env, "ST", "2002-05-30T09:00:09.999");
            assertFalse(env.listenerStage("ST", "s0").getAndClearIsInvoked());
            advanceTime(env, "ST", "2002-05-30T09:00:10.000");
            assertPropsPerRow(env.listenerStage("ST", "s0").getAndResetLastNewData(), fields, new Object[][]{{"E1"}});
            env.sendEventBeanStage("ST", new SupportBean("E2", 1));

            env.milestone(2);

            advanceTime(env, "2002-05-30T09:00:19.999");
            advanceTime(env, "ST", "2002-05-30T09:00:19.999");
            assertFalse(env.listenerStage("ST", "s0").getAndClearIsInvoked());

            unstageIt(env, "ST", deploymentId);

            env.milestone(3);

            advanceTime(env, "ST", "2002-05-30T09:00:20.000");
            env.assertListenerNotInvoked("s0");
            advanceTime(env, "2002-05-30T09:00:20.000");
            assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2"}});

            env.undeployAll();
        }
    }

    private static class ClientStageAdvanceWindowTime implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String epl = "@name('s0') select irstream * from SupportBean#time(10)";
            env.compileDeploy(epl).addListener("s0");
            String deploymentId = env.deploymentId("s0");

            env.sendEventBean(new SupportBean("E1", 1));

            env.advanceTime(2000);
            env.sendEventBean(new SupportBean("E2", 2));

            env.advanceTime(4000);
            env.sendEventBean(new SupportBean("E3", 3));
            env.listener("s0").reset();

            env.milestone(0);

            env.stageService().getStage("P1");
            stageIt(env, "P1", deploymentId);

            env.milestone(1);

            env.advanceTimeStage("P1", 9999);
            assertFalse(env.listenerStage("P1", "s0").getAndClearIsInvoked());
            env.advanceTimeStage("P1", 10000);
            assertEquals("E1", env.listenerStage("P1", "s0").assertOneGetOldAndReset().get("theString"));

            env.milestone(2);

            env.advanceTimeStage("P1", 11999);
            assertFalse(env.listenerStage("P1", "s0").getAndClearIsInvoked());
            env.advanceTimeStage("P1", 12000);
            assertEquals("E2", env.listenerStage("P1", "s0").assertOneGetOldAndReset().get("theString"));

            env.advanceTime(12000);
            assertFalse(env.listenerStage("P1", "s0").getAndClearIsInvoked());

            unstageIt(env, "P1", deploymentId);

            env.milestone(3);

            env.advanceTime(13999);
            env.advanceTimeStage("P1", 14000);
            SupportListener listener = env.listener("s0");
            assertFalse(listener.getAndClearIsInvoked());
            env.advanceTime(14000);
            assertEquals("E3", env.listener("s0").assertOneGetOldAndReset().get("theString"));

            env.undeployAll();
        }
    }

    private static void advanceTime(RegressionEnvironment env, String stageUri, String time) {
        env.advanceTimeStage(stageUri, parseDefaultMSec(time));
    }

    private static void advanceTime(RegressionEnvironment env, String time) {
        advanceTime(env, null, time);
    }
}
