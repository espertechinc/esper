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
import com.espertech.esper.client.metric.EngineMetric;
import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.client.ExecClientMetricsReportingNW.applyMetricsConfig;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecClientMetricsReportingDisableRuntime implements RegressionExecution {
    private final static long CPUGOALONENANO = 80 * 1000 * 1000;

    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, 10000, 10000, true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement[] statements = new EPStatement[5];
        sendTimer(epService, 1000);

        statements[0] = epService.getEPAdministrator().createEPL("select * from " + StatementMetric.class.getName(), "stmtmetric");
        SupportUpdateListener listenerStmtMetric = new SupportUpdateListener();
        statements[0].addListener(listenerStmtMetric);

        statements[1] = epService.getEPAdministrator().createEPL("select * from " + EngineMetric.class.getName(), "enginemetric");
        SupportUpdateListener listenerEngineMetric = new SupportUpdateListener();
        statements[1].addListener(listenerEngineMetric);

        statements[2] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=1)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)");
        sendEvent(epService, "E1", 1, CPUGOALONENANO);

        sendTimer(epService, 11000);
        assertTrue(listenerStmtMetric.getAndClearIsInvoked());
        assertTrue(listenerEngineMetric.getAndClearIsInvoked());

        epService.getEPAdministrator().getConfiguration().setMetricsReportingDisabled();
        sendEvent(epService, "E2", 2, CPUGOALONENANO);
        sendTimer(epService, 21000);
        assertFalse(listenerStmtMetric.getAndClearIsInvoked());
        assertFalse(listenerEngineMetric.getAndClearIsInvoked());

        sendTimer(epService, 31000);
        sendEvent(epService, "E3", 3, CPUGOALONENANO);
        assertFalse(listenerStmtMetric.getAndClearIsInvoked());
        assertFalse(listenerEngineMetric.getAndClearIsInvoked());

        epService.getEPAdministrator().getConfiguration().setMetricsReportingEnabled();
        sendEvent(epService, "E4", 4, CPUGOALONENANO);
        sendTimer(epService, 41000);
        assertTrue(listenerStmtMetric.getAndClearIsInvoked());
        assertTrue(listenerEngineMetric.getAndClearIsInvoked());

        statements[2].destroy();
        sendTimer(epService, 51000);
        assertTrue(listenerStmtMetric.isInvoked()); // metrics statements reported themselves
        assertTrue(listenerEngineMetric.isInvoked());
    }

    private void sendTimer(EPServiceProvider epService, long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }

    private void sendEvent(EPServiceProvider epService, String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
