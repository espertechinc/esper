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
import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import static org.junit.Assert.*;

public class ClientInstrumentMetricsReportingStmtMetrics implements RegressionExecution {
    private final static long CPU_GOAL_ONE_NANO = 80 * 1000 * 1000;
    private final static long CPU_GOAL_TWO_NANO = 50 * 1000 * 1000;
    private final static long WALL_GOAL_ONE_MSEC = 200;
    private final static long WALL_GOAL_TWO_MSEC = 400;

    public void run(RegressionEnvironment env) {

        sendTimer(env, 1000);

        EPStatement[] statements = new EPStatement[5];
        statements[0] = env.compileDeploy("@name('stmt_metrics') select * from " + StatementMetric.class.getName()).statement("stmt_metrics");
        statements[0].addListener(env.listenerNew());

        statements[1] = env.compileDeploy("@name('cpuStmtOne') select * from SupportBean(intPrimitive=1)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)").statement("cpuStmtOne");
        statements[1].addListener(env.listenerNew());
        statements[2] = env.compileDeploy("@name('cpuStmtTwo') select * from SupportBean(intPrimitive=2)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)").statement("cpuStmtTwo");
        statements[2].addListener(env.listenerNew());
        statements[3] = env.compileDeploy("@name('wallStmtThree') select * from SupportBean(intPrimitive=3)#keepall where MyMetricFunctions.takeWallTime(longPrimitive)").statement("wallStmtThree");
        statements[3].addListener(env.listenerNew());
        statements[4] = env.compileDeploy("@name('wallStmtFour') select * from SupportBean(intPrimitive=4)#keepall where MyMetricFunctions.takeWallTime(longPrimitive)").statement("wallStmtFour");
        statements[4].addListener(env.listenerNew());

        sendEvent(env, "E1", 1, CPU_GOAL_ONE_NANO);
        sendEvent(env, "E2", 2, CPU_GOAL_TWO_NANO);
        sendEvent(env, "E3", 3, WALL_GOAL_ONE_MSEC);
        sendEvent(env, "E4", 4, WALL_GOAL_TWO_MSEC);

        sendTimer(env, 10999);
        assertFalse(env.listener("stmt_metrics").isInvoked());

        sendTimer(env, 11000);
        tryAssertion(env, 11000);

        sendEvent(env, "E1", 1, CPU_GOAL_ONE_NANO);
        sendEvent(env, "E2", 2, CPU_GOAL_TWO_NANO);
        sendEvent(env, "E3", 3, WALL_GOAL_ONE_MSEC);
        sendEvent(env, "E4", 4, WALL_GOAL_TWO_MSEC);

        sendTimer(env, 21000);
        tryAssertion(env, 21000);

        // destroy all application stmts
        for (int i = 1; i < 5; i++) {
            env.undeployModuleContaining(statements[i].getName());
        }
        sendTimer(env, 31000);
        assertFalse(env.listener("stmt_metrics").isInvoked());

        env.undeployAll();
    }

    private void tryAssertion(RegressionEnvironment env, long timestamp) {
        String[] fields = "runtimeURI,statementName".split(",");

        SupportListener listener = env.listener("stmt_metrics");
        assertEquals(4, listener.getNewDataList().size());
        EventBean[] received = listener.getNewDataListFlattened();

        EPAssertionUtil.assertProps(received[0], fields, new Object[]{"default", "cpuStmtOne"});
        EPAssertionUtil.assertProps(received[1], fields, new Object[]{"default", "cpuStmtTwo"});
        EPAssertionUtil.assertProps(received[2], fields, new Object[]{"default", "wallStmtThree"});
        EPAssertionUtil.assertProps(received[3], fields, new Object[]{"default", "wallStmtFour"});

        long cpuOne = (Long) received[0].get("cpuTime");
        long cpuTwo = (Long) received[1].get("cpuTime");
        long wallOne = (Long) received[2].get("wallTime");
        long wallTwo = (Long) received[3].get("wallTime");

        assertTrue("cpuOne=" + cpuOne, cpuOne > CPU_GOAL_ONE_NANO);
        assertTrue("cpuTwo=" + cpuTwo, cpuTwo > CPU_GOAL_TWO_NANO);
        assertTrue("wallOne=" + wallOne, (wallOne + 50) > WALL_GOAL_ONE_MSEC);
        assertTrue("wallTwo=" + wallTwo, (wallTwo + 50) > WALL_GOAL_TWO_MSEC);

        for (int i = 0; i < 4; i++) {
            assertEquals(1L, received[i].get("numOutputIStream"));
            assertEquals(0L, received[i].get("numOutputRStream"));
            assertEquals(timestamp, received[i].get("timestamp"));
        }

        listener.reset();
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
