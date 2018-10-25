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
package com.espertech.esper.regressionlib.suite.client.instrument;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.metric.RuntimeMetric;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.MyMetricFunctions;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.*;

public class ClientInstrumentMetricsReportingRuntimeMetrics implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String[] fields = "runtimeURI,timestamp,inputCount,inputCountDelta,scheduleDepth".split(",");
        sendTimer(env, 1000);

        String text = "@name('s0') select * from " + RuntimeMetric.class.getName();
        env.compileDeploy(text).addListener("s0");

        env.sendEventBean(new SupportBean());

        sendTimer(env, 10999);
        assertFalse(env.listener("s0").isInvoked());

        env.compileDeploy("select * from pattern[timer:interval(5 sec)]");

        sendTimer(env, 11000);
        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fields, new Object[]{"default", 11000L, 1L, 1L, 1L});

        env.sendEventBean(new SupportBean());
        env.sendEventBean(new SupportBean());

        sendTimer(env, 20000);
        sendTimer(env, 21000);
        theEvent = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fields, new Object[]{"default", 21000L, 4L, 3L, 0L});

        // Try MBean
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        if (!mbean.isThreadCpuTimeEnabled()) {
            fail("ThreadMXBean CPU time reporting is not enabled");
        }

        long msecMultiplier = 1000 * 1000;
        long msecGoal = 10;
        long cpuGoal = msecGoal * msecMultiplier;

        long beforeCPU = mbean.getCurrentThreadCpuTime();
        MyMetricFunctions.takeCPUTime(cpuGoal);
        long afterCPU = mbean.getCurrentThreadCpuTime();
        assertTrue((afterCPU - beforeCPU) > cpuGoal);

        env.undeployAll();
    }

    private void sendTimer(RegressionEnvironment env, long currentTime) {
        env.advanceTime(currentTime);
    }
}
