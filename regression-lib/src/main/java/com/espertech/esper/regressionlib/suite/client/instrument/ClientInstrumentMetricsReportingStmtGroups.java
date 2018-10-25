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
import com.espertech.esper.runtime.client.scopetest.SupportSubscriber;

import static org.junit.Assert.assertFalse;

public class ClientInstrumentMetricsReportingStmtGroups implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        sendTimer(env, 0);

        env.compileDeploy("@name('GroupOne') select * from SupportBean(intPrimitive = 1)#keepall");
        env.compileDeploy("@name('GroupTwo') select * from SupportBean(intPrimitive = 2)#keepall");
        env.statement("GroupTwo").setSubscriber(new SupportSubscriber());
        env.compileDeploy("@name('Default') select * from SupportBean(intPrimitive = 3)#keepall");   // no listener

        env.compileDeploy("@name('StmtMetrics') select * from " + StatementMetric.class.getName()).addListener("StmtMetrics");

        sendTimer(env, 6000);
        sendTimer(env, 7000);
        assertFalse(env.listener("StmtMetrics").isInvoked());

        sendTimer(env, 8000);
        String[] fields = "statementName,numOutputIStream,numInput".split(",");
        EPAssertionUtil.assertProps(env.listener("StmtMetrics").assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});

        sendTimer(env, 12000);
        sendTimer(env, 14000);
        sendTimer(env, 15999);
        assertFalse(env.listener("StmtMetrics").isInvoked());

        sendTimer(env, 16000);
        EPAssertionUtil.assertProps(env.listener("StmtMetrics").assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});

        // should report as groupTwo
        env.sendEventBean(new SupportBean("E1", 2));
        sendTimer(env, 17999);
        assertFalse(env.listener("StmtMetrics").isInvoked());

        sendTimer(env, 18000);
        EPAssertionUtil.assertProps(env.listener("StmtMetrics").assertOneGetNewAndReset(), fields, new Object[]{"GroupTwo", 1L, 1L});

        // should report as groupTwo
        env.sendEventBean(new SupportBean("E1", 3));
        sendTimer(env, 20999);
        assertFalse(env.listener("StmtMetrics").isInvoked());

        sendTimer(env, 21000);
        EPAssertionUtil.assertProps(env.listener("StmtMetrics").assertOneGetNewAndReset(), fields, new Object[]{"Default", 0L, 1L});

        // turn off group 1
        env.runtime().getMetricsService().setMetricsReportingInterval("GroupOneStatements", -1);
        sendTimer(env, 24000);
        assertFalse(env.listener("StmtMetrics").isInvoked());

        // turn on group 1
        env.runtime().getMetricsService().setMetricsReportingInterval("GroupOneStatements", 1000);
        sendTimer(env, 25000);
        EPAssertionUtil.assertProps(env.listener("StmtMetrics").assertOneGetNewAndReset(), fields, new Object[]{"GroupOne", 0L, 0L});

        env.undeployAll();
    }

    private void sendTimer(RegressionEnvironment env, long currentTime) {
        env.advanceTime(currentTime);
    }
}
