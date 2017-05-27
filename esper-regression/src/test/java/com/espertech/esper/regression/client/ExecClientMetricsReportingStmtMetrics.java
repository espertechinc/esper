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

import com.espertech.esper.client.*;
import com.espertech.esper.client.metric.StatementMetric;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.regression.client.ExecClientMetricsReportingNW.applyMetricsConfig;
import static org.junit.Assert.*;

public class ExecClientMetricsReportingStmtMetrics implements RegressionExecution {
    private final static long CPU_GOAL_ONE_NANO = 80 * 1000 * 1000;
    private final static long CPU_GOAL_TWO_NANO = 50 * 1000 * 1000;
    private final static long WALL_GOAL_ONE_MSEC = 200;
    private final static long WALL_GOAL_TWO_MSEC = 400;

    public void configure(Configuration configuration) throws Exception {
        applyMetricsConfig(configuration, -1, -1, true);

        ConfigurationMetricsReporting.StmtGroupMetrics configOne = new ConfigurationMetricsReporting.StmtGroupMetrics();
        configOne.setInterval(10000);
        configOne.addIncludeLike("%cpuStmt%");
        configOne.addIncludeLike("%wallStmt%");
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("nonmetrics", configOne);

        // exclude metrics themselves from reporting
        ConfigurationMetricsReporting.StmtGroupMetrics configTwo = new ConfigurationMetricsReporting.StmtGroupMetrics();
        configTwo.setInterval(-1);
        configOne.addExcludeLike("%metrics%");
        configuration.getEngineDefaults().getMetricsReporting().addStmtGroup("metrics", configTwo);
    }

    public void run(EPServiceProvider epService) throws Exception {

        sendTimer(epService, 1000);

        EPStatement[] statements = new EPStatement[5];
        statements[0] = epService.getEPAdministrator().createEPL("select * from " + StatementMetric.class.getName(), "stmt_metrics");
        SupportUpdateListener listener = new SupportUpdateListener();
        statements[0].addListener(listener);

        statements[1] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=1)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)", "cpuStmtOne");
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statements[1].addListener(listenerTwo);
        statements[2] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=2)#keepall where MyMetricFunctions.takeCPUTime(longPrimitive)", "cpuStmtTwo");
        statements[2].addListener(listenerTwo);
        statements[3] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=3)#keepall where MyMetricFunctions.takeWallTime(longPrimitive)", "wallStmtThree");
        statements[3].addListener(listenerTwo);
        statements[4] = epService.getEPAdministrator().createEPL("select * from SupportBean(intPrimitive=4)#keepall where MyMetricFunctions.takeWallTime(longPrimitive)", "wallStmtFour");
        statements[4].addListener(listenerTwo);

        sendEvent(epService, "E1", 1, CPU_GOAL_ONE_NANO);
        sendEvent(epService, "E2", 2, CPU_GOAL_TWO_NANO);
        sendEvent(epService, "E3", 3, WALL_GOAL_ONE_MSEC);
        sendEvent(epService, "E4", 4, WALL_GOAL_TWO_MSEC);

        sendTimer(epService, 10999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 11000);
        tryAssertion(epService, listener, 11000);

        sendEvent(epService, "E1", 1, CPU_GOAL_ONE_NANO);
        sendEvent(epService, "E2", 2, CPU_GOAL_TWO_NANO);
        sendEvent(epService, "E3", 3, WALL_GOAL_ONE_MSEC);
        sendEvent(epService, "E4", 4, WALL_GOAL_TWO_MSEC);

        sendTimer(epService, 21000);
        tryAssertion(epService, listener, 21000);

        // destroy all application stmts
        for (int i = 1; i < 5; i++) {
            statements[i].destroy();
        }
        sendTimer(epService, 31000);
        assertFalse(listener.isInvoked());
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener, long timestamp) {
        String[] fields = "engineURI,statementName".split(",");

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

    private void sendTimer(EPServiceProvider epService, long currentTime) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(currentTime));
    }

    private void sendEvent(EPServiceProvider epService, String id, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(id, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
