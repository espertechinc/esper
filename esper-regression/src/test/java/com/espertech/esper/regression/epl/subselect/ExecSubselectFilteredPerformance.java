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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.bean.SupportBean_S3;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecSubselectFilteredPerformance implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
        configuration.addEventType("S3", SupportBean_S3.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerformanceOneCriteria(epService);
        runAssertionPerformanceTwoCriteria(epService);
        runAssertionPerformanceJoin3CriteriaSceneOne(epService);
        runAssertionPerformanceJoin3CriteriaSceneTwo(epService);
    }

    private void runAssertionPerformanceOneCriteria(EPServiceProvider epService) {
        String stmtText = "select (select p10 from S1#length(100000) where id = s0.id) as value from S0 as s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload with 10k events
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(i, Integer.toString(i)));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(new SupportBean_S0(index, Integer.toString(index)));
            assertEquals(Integer.toString(index), listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
        stmt.destroy();
    }

    private void runAssertionPerformanceTwoCriteria(EPServiceProvider epService) {
        String stmtText = "select (select p10 from S1#length(100000) where s0.id = id and p10 = s0.p00) as value from S0 as s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload with 10k events
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S1(i, Integer.toString(i)));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            int index = 5000 + i % 1000;
            epService.getEPRuntime().sendEvent(new SupportBean_S0(index, Integer.toString(index)));
            assertEquals(Integer.toString(index), listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1000);
        stmt.destroy();
    }

    private void runAssertionPerformanceJoin3CriteriaSceneOne(EPServiceProvider epService) {
        String stmtText = "select (select p00 from S0#length(100000) where p00 = s1.p10 and p01 = s2.p20 and p02 = s3.p30) as value " +
                "from S1#length(100000) as s1, S2#length(100000) as s2, S3#length(100000) as s3 where s1.id = s2.id and s2.id = s3.id";
        tryPerfJoin3Criteria(epService, stmtText);
    }

    private void runAssertionPerformanceJoin3CriteriaSceneTwo(EPServiceProvider epService) {
        String stmtText = "select (select p00 from S0#length(100000) where p01 = s2.p20 and p00 = s1.p10 and p02 = s3.p30 and id >= 0) as value " +
                "from S3#length(100000) as s3, S1#length(100000) as s1, S2#length(100000) as s2 where s2.id = s3.id and s1.id = s2.id";
        tryPerfJoin3Criteria(epService, stmtText);
    }

    private void tryPerfJoin3Criteria(EPServiceProvider epService, String stmtText) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // preload with 10k events
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(i, Integer.toString(i), Integer.toString(i + 1), Integer.toString(i + 2)));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            int index = i;
            epService.getEPRuntime().sendEvent(new SupportBean_S1(i, Integer.toString(index)));
            epService.getEPRuntime().sendEvent(new SupportBean_S2(i, Integer.toString(index + 1)));
            epService.getEPRuntime().sendEvent(new SupportBean_S3(i, Integer.toString(index + 2)));
            assertEquals(Integer.toString(index), listener.assertOneGetNewAndReset().get("value"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        assertTrue("Failed perf test, delta=" + delta, delta < 1500);
        stmt.destroy();
    }
}
