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
import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.MyMetricFunctions;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.ArrayHandlingUtil;

public class ExecClientMetricsReportingNW implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, -1, 1000, false);
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeDebugSymbols(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeComments(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("@Name('0') create schema StatementMetric as " + StatementMetric.class.getName());
        epService.getEPAdministrator().createEPL("@Name('A') create window MyWindow#lastevent as select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('B1') insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('B2') insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('C') select sum(intPrimitive) from MyWindow");
        epService.getEPAdministrator().createEPL("@Name('D') select sum(w1.intPrimitive) from MyWindow w1, MyWindow w2");

        String appModuleTwo = "@Name('W') create window SupportBeanWindow#keepall as SupportBean;" +
                "" +
                "@Name('M') on SupportBean oe\n" +
                "  merge SupportBeanWindow pw\n" +
                "  where pw.theString = oe.theString\n" +
                "  when not matched \n" +
                "    then insert select *\n" +
                "  when matched and oe.intPrimitive=1\n" +
                "    then delete\n" +
                "  when matched\n" +
                "    then update set pw.intPrimitive = oe.intPrimitive";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(appModuleTwo, null, null, null);

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('X') select * from StatementMetric");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "statementName,numInput".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        EventBean[] received = ArrayHandlingUtil.reorder("statementName", listener.getNewDataListFlattened());
        for (EventBean theEvent : received) {
            System.out.println(theEvent.get("statementName") + " = " + theEvent.get("numInput"));
        }
        EPAssertionUtil.assertPropsPerRow(received, fields, new Object[][]{{"A", 2L}, {"B1", 1L}, {"B2", 1L}, {"C", 2L}, {"D", 2L}, {"M", 1L}, {"W", 1L}});

        /* Comment-in for printout.
        for (int i = 0; i < received.length; i++) {
            EventBean event = received[i];
            System.out.println(event.get("statementName") + " " + event.get("wallTime") + " " + event.get("numInput"));
        }
        */
    }

    protected static void applyMetricsConfig(Configuration configuration, long engineMetricInterval, long stmtMetricInterval, boolean shareViews) {
        configuration.getEngineDefaults().getViewResources().setShareViews(shareViews);
        configuration.getEngineDefaults().getMetricsReporting().setEnableMetricsReporting(true);
        configuration.getEngineDefaults().getMetricsReporting().setThreading(false);
        configuration.getEngineDefaults().getMetricsReporting().setEngineInterval(engineMetricInterval);
        configuration.getEngineDefaults().getMetricsReporting().setStatementInterval(stmtMetricInterval);
        configuration.addImport(MyMetricFunctions.class.getName());
        configuration.addEventType("SupportBean", SupportBean.class);
    }
}
