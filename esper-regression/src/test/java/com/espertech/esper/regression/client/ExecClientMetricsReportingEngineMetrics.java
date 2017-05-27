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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.metric.EngineMetric;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.MyMetricFunctions;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static com.espertech.esper.regression.client.ExecClientMetricsReportingNW.applyMetricsConfig;
import static org.junit.Assert.*;

public class ExecClientMetricsReportingEngineMetrics implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, 10000, -1, true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] engineFields = "engineURI,timestamp,inputCount,inputCountDelta,scheduleDepth".split(",");
        sendTimer(epService, 1000);

        String text = "select * from " + EngineMetric.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());

        sendTimer(epService, 10999);
        assertFalse(listener.isInvoked());

        epService.getEPAdministrator().createEPL("select * from pattern[timer:interval(5 sec)]");

        sendTimer(epService, 11000);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, engineFields, new Object[]{"default", 11000L, 1L, 1L, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean());
        epService.getEPRuntime().sendEvent(new SupportBean());

        sendTimer(epService, 20000);
        sendTimer(epService, 21000);
        theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, engineFields, new Object[]{"default", 21000L, 4L, 3L, 0L});

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
    }

    private void sendTimer(EPServiceProvider epService, long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }
}
