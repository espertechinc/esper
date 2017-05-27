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
import com.espertech.esper.client.ConfigurationMetricsReporting;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportSubscriber;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.client.ExecClientMetricsReportingNW.applyMetricsConfig;
import static org.junit.Assert.assertFalse;

public class ExecClientMetricsReportingStmtGroups implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, -1, 7000, true);

        ConfigurationMetricsReporting.StmtGroupMetrics groupOne = new ConfigurationMetricsReporting.StmtGroupMetrics();
        groupOne.setInterval(8000);
        groupOne.addIncludeLike("%GroupOne%");
        groupOne.setReportInactive(true);
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("GroupOneStatements", groupOne);

        ConfigurationMetricsReporting.StmtGroupMetrics groupTwo = new ConfigurationMetricsReporting.StmtGroupMetrics();
        groupTwo.setInterval(6000);
        groupTwo.setDefaultInclude(true);
        groupTwo.addExcludeLike("%Default%");
        groupTwo.addExcludeLike("%Metrics%");
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("GroupTwoNonDefaultStatements", groupTwo);

        ConfigurationMetricsReporting.StmtGroupMetrics groupThree = new ConfigurationMetricsReporting.StmtGroupMetrics();
        groupThree.setInterval(-1);
        groupThree.addIncludeLike("%Metrics%");
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("MetricsStatements", groupThree);
    }

    public void run(EPServiceProvider epService) throws Exception {

        sendTimer(epService, 0);

        epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive = 1)#keepall", "GroupOne");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive = 2)#keepall", "GroupTwo");
        stmt.setSubscriber(new SupportSubscriber());
        epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive = 3)#keepall", "Default");   // no listener

        stmt = epService.getEPAdministrator().createEPL("select * from " + StatementMetric.class.getName(), "StmtMetrics");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(epService, 6000);
        sendTimer(epService, 7000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 8000);
        String[] fields = "statementName,numOutputIStream,numInput".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});

        sendTimer(epService, 12000);
        sendTimer(epService, 14000);
        sendTimer(epService, 15999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 16000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});

        // should report as groupTwo
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        sendTimer(epService, 17999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 18000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"GroupTwo", 1L, 1L});

        // should report as groupTwo
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        sendTimer(epService, 20999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 21000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"Default", 0L, 1L});

        // turn off group 1
        epService.getEPAdministrator().getConfiguration().setMetricsReportingInterval("GroupOneStatements", -1);
        sendTimer(epService, 24000);
        assertFalse(listener.isInvoked());

        // turn on group 1
        epService.getEPAdministrator().getConfiguration().setMetricsReportingInterval("GroupOneStatements", 1000);
        sendTimer(epService, 25000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});
    }

    private void sendTimer(EPServiceProvider epService, long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }
}
