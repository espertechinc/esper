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

import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;

public class ClientInstrumentMetricsReportingDisableStatement implements RegressionExecution {
    private final static long CPUGOALONENANO = 80 * 1000 * 1000;

    public void run(RegressionEnvironment env) {
        String[] fields = new String[]{"statementName"};
        EPStatement[] statements = new EPStatement[5];

        sendTimer(env, 1000);

        statements[0] = env.compileDeploy("@name('MyStatement@METRIC') select * from " + StatementMetric.class.getName()).statement("MyStatement@METRIC");
        statements[0].addListener(env.listenerNew());

        statements[1] = env.compileDeploy("@name('stmtone') select * from SupportBean(intPrimitive=1)#keepall where 2=2").statement("stmtone");
        sendEvent(env, "E1", 1, CPUGOALONENANO);
        statements[2] = env.compileDeploy("@name('stmttwo') select * from SupportBean(intPrimitive>0)#lastevent where 1=1").statement("stmttwo");
        sendEvent(env, "E2", 1, CPUGOALONENANO);

        sendTimer(env, 11000);
        EPAssertionUtil.assertPropsPerRow(env.listener("MyStatement@METRIC").getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}, {"stmttwo"}});
        env.listener("MyStatement@METRIC").reset();

        sendEvent(env, "E1", 1, CPUGOALONENANO);
        sendTimer(env, 21000);
        EPAssertionUtil.assertPropsPerRow(env.listener("MyStatement@METRIC").getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}, {"stmttwo"}});
        env.listener("MyStatement@METRIC").reset();

        env.runtime().getMetricsService().setMetricsReportingStmtDisabled(env.deploymentId("stmtone"), "stmtone");

        sendEvent(env, "E1", 1, CPUGOALONENANO);
        sendTimer(env, 31000);
        EPAssertionUtil.assertPropsPerRow(env.listener("MyStatement@METRIC").getNewDataListFlattened(), fields, new Object[][]{{"stmttwo"}});
        env.listener("MyStatement@METRIC").reset();

        env.runtime().getMetricsService().setMetricsReportingStmtEnabled(env.deploymentId("stmtone"), "stmtone");
        env.runtime().getMetricsService().setMetricsReportingStmtDisabled(env.deploymentId("stmttwo"), "stmttwo");

        sendEvent(env, "E1", 1, CPUGOALONENANO);
        sendTimer(env, 41000);
        EPAssertionUtil.assertPropsPerRow(env.listener("MyStatement@METRIC").getNewDataListFlattened(), fields, new Object[][]{{"stmtone"}});

        env.undeployAll();
    }

    private void sendTimer(RegressionEnvironment env, long currentTime) {
        env.advanceTime(currentTime);
    }

    private void sendEvent(RegressionEnvironment env, String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }
}
