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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeMetricsReporting;
import com.espertech.esper.regressionlib.suite.client.instrument.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import com.espertech.esper.regressionlib.support.client.MyMetricFunctions;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteClientInstrument extends TestCase {

    public void testClientInstrumentInstrumentation() {
        RegressionSession session = RegressionRunner.session();
        for (Class clazz : new Class[]{SupportBean.class}) {
            session.getConfiguration().getCommon().addEventType(clazz);
        }
        RegressionRunner.run(session, new ClientInstrumentInstrumentation());
        session.destroy();
    }

    public void testClientInstrumentAudit() {
        RegressionSession session = RegressionRunner.session();
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_ST0.class, SupportBean_ST1.class}) {
            session.getConfiguration().getCommon().addEventType(clazz);
        }
        session.getConfiguration().getRuntime().getLogging().setAuditPattern("[%u] [%d] [%s] [%i] [%c] %m");
        RegressionRunner.run(session, ClientInstrumentAudit.executions());
        session.destroy();
    }

    public void testClientInstrumentMetricsReportingStmtMetrics() {
        RegressionSession session = RegressionRunner.session();

        applyMetricsConfig(session.getConfiguration(), -1, -1);

        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics configOne = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        configOne.setInterval(10000);
        configOne.addIncludeLike("%cpuStmt%");
        configOne.addIncludeLike("%wallStmt%");
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("nonmetrics", configOne);

        // exclude metrics themselves from reporting
        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics configTwo = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        configTwo.setInterval(-1);
        configOne.addExcludeLike("%metrics%");
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("metrics", configTwo);

        RegressionRunner.run(session, new ClientInstrumentMetricsReportingStmtMetrics());

        session.destroy();
    }

    public void testClientInstrumentMetricsReportingStmtGroups() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getByteCode().setAllowSubscriber(true);

        applyMetricsConfig(session.getConfiguration(), -1, 7000);

        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics groupOne = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        groupOne.setInterval(8000);
        groupOne.addIncludeLike("%GroupOne%");
        groupOne.setReportInactive(true);
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("GroupOneStatements", groupOne);

        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics groupTwo = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        groupTwo.setInterval(6000);
        groupTwo.setDefaultInclude(true);
        groupTwo.addExcludeLike("%Default%");
        groupTwo.addExcludeLike("%Metrics%");
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("GroupTwoNonDefaultStatements", groupTwo);

        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics groupThree = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        groupThree.setInterval(-1);
        groupThree.addIncludeLike("%Metrics%");
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("MetricsStatements", groupThree);

        RegressionRunner.run(session, new ClientInstrumentMetricsReportingStmtGroups());

        session.destroy();
    }

    public void testClientInstrumentMetricsReportingNW() {
        RegressionSession session = RegressionRunner.session();
        applyMetricsConfig(session.getConfiguration(), -1, 1000);
        RegressionRunner.run(session, new ClientInstrumentMetricsReportingNW());
        session.destroy();
    }

    public void testClientInstrumentMetricsReportingRuntimeMetrics() {
        RegressionSession session = RegressionRunner.session();
        applyMetricsConfig(session.getConfiguration(), 10000, -1);
        RegressionRunner.run(session, new ClientInstrumentMetricsReportingRuntimeMetrics());
        session.destroy();
    }

    public void testClientInstrumentMetricsReportingDisableStatement() {
        RegressionSession session = RegressionRunner.session();
        applyMetricsConfig(session.getConfiguration(), -1, 10000);
        ConfigurationRuntimeMetricsReporting.StmtGroupMetrics configOne = new ConfigurationRuntimeMetricsReporting.StmtGroupMetrics();
        configOne.setInterval(-1);
        configOne.addIncludeLike("%@METRIC%");
        session.getConfiguration().getRuntime().getMetricsReporting().addStmtGroup("metrics", configOne);
        RegressionRunner.run(session, new ClientInstrumentMetricsReportingDisableStatement());
        session.destroy();
    }

    public void testClientInstrumentMetricsReportingDisableRuntime() {
        RegressionSession session = RegressionRunner.session();
        applyMetricsConfig(session.getConfiguration(), 10000, 10000);
        RegressionRunner.run(session, new ClientInstrumentMetricsReportingDisableRuntime());
        session.destroy();
    }

    private static void applyMetricsConfig(Configuration configuration, long runtimeMetricInterval, long stmtMetricInterval) {
        configuration.getRuntime().getMetricsReporting().setEnableMetricsReporting(true);
        configuration.getRuntime().getMetricsReporting().setThreading(false);
        configuration.getRuntime().getMetricsReporting().setRuntimeInterval(runtimeMetricInterval);
        configuration.getRuntime().getMetricsReporting().setStatementInterval(stmtMetricInterval);
        configuration.getCommon().addImport(MyMetricFunctions.class.getName());
        configuration.getCommon().addEventType(SupportBean.class);
    }
}
