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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.client.ExecClientMetricsReportingNW.applyMetricsConfig;

public class ExecClientMetricsReportingDisableStatement implements RegressionExecution {
    private final static long CPUGOALONENANO = 80 * 1000 * 1000;

    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, -1, 10000, true);

        ConfigurationMetricsReporting.StmtGroupMetrics configOne = new ConfigurationMetricsReporting.StmtGroupMetrics();
        configOne.setInterval(-1);
        configOne.addIncludeLike("%@METRIC%");
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("metrics", configOne);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String[] fields = new String[]{"statementName"};
        EPStatement[] statements = new EPStatement[5];

        sendTimer(epService, 1000);

        statements[0] = epService.getEPAdministrator().createEPL("select * from " + StatementMetric.class.getName(), "MyStatement@METRIC");
        SupportUpdateListener listenerStmtMetric = new SupportUpdateListener();
        statements[0].addListener(listenerStmtMetric);

        statements[1] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=1)#keepall where 2=2", "stmtone");
        sendEvent(epService, "E1", 1, CPUGOALONENANO);
        statements[2] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive>0)#lastevent where 1=1", "stmttwo");
        sendEvent(epService, "E2", 1, CPUGOALONENANO);

        sendTimer(epService, 11000);
        EPAssertionUtil.assertPropsPerRow(listenerStmtMetric.getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}, {"stmttwo"}});
        listenerStmtMetric.reset();

        sendEvent(epService, "E1", 1, CPUGOALONENANO);
        sendTimer(epService, 21000);
        EPAssertionUtil.assertPropsPerRow(listenerStmtMetric.getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}, {"stmttwo"}});
        listenerStmtMetric.reset();

        epService.getEPAdministrator().getConfiguration().setMetricsReportingStmtDisabled("stmtone");

        sendEvent(epService, "E1", 1, CPUGOALONENANO);
        sendTimer(epService, 31000);
        EPAssertionUtil.assertPropsPerRow(listenerStmtMetric.getNewDataListFlattened(), fields, new Object[][]{{"stmttwo"}});
        listenerStmtMetric.reset();

        epService.getEPAdministrator().getConfiguration().setMetricsReportingStmtEnabled("stmtone");
        epService.getEPAdministrator().getConfiguration().setMetricsReportingStmtDisabled("stmttwo");

        sendEvent(epService, "E1", 1, CPUGOALONENANO);
        sendTimer(epService, 41000);
        EPAssertionUtil.assertPropsPerRow(listenerStmtMetric.getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}});
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
