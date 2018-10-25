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

import com.espertech.esper.common.client.metric.RuntimeMetric;
import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientInstrumentMetricsReportingDisableRuntime implements RegressionExecution {
    private final static long CPUGOALONENANO = 80 * 1000 * 1000;

    public void run(RegressionEnvironment env) {
        EPStatement[] statements = new EPStatement[5];
        sendTimer(env, 1000);

        statements[0] = env.compileDeploy("@name('stmtmetric') select * from " + StatementMetric.class.getName()).statement("stmtmetric");
        statements[0].addListener(env.listenerNew());

        statements[1] = env.compileDeploy("@name('runtimemetric') select * from " + RuntimeMetric.class.getName()).statement("runtimemetric");
        statements[1].addListener(env.listenerNew());

        statements[2] = env.compileDeploy("@name('stmt-1') select * from SupportBean(intPrimitive=1)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)").statement("stmt-1");
        sendEvent(env, "E1", 1, CPUGOALONENANO);

        sendTimer(env, 11000);
        assertTrue(env.listener("stmtmetric").getAndClearIsInvoked());
        assertTrue(env.listener("runtimemetric").getAndClearIsInvoked());

        env.runtime().getMetricsService().setMetricsReportingDisabled();
        sendEvent(env, "E2", 2, CPUGOALONENANO);
        sendTimer(env, 21000);
        assertFalse(env.listener("stmtmetric").getAndClearIsInvoked());
        assertFalse(env.listener("runtimemetric").getAndClearIsInvoked());

        sendTimer(env, 31000);
        sendEvent(env, "E3", 3, CPUGOALONENANO);
        assertFalse(env.listener("stmtmetric").getAndClearIsInvoked());
        assertFalse(env.listener("runtimemetric").getAndClearIsInvoked());

        env.runtime().getMetricsService().setMetricsReportingEnabled();
        sendEvent(env, "E4", 4, CPUGOALONENANO);
        sendTimer(env, 41000);
        assertTrue(env.listener("stmtmetric").getAndClearIsInvoked());
        assertTrue(env.listener("runtimemetric").getAndClearIsInvoked());

        env.undeployModuleContaining(statements[2].getName());
        sendTimer(env, 51000);
        assertTrue(env.listener("stmtmetric").isInvoked()); // metrics statements reported themselves
        assertTrue(env.listener("runtimemetric").isInvoked());

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
