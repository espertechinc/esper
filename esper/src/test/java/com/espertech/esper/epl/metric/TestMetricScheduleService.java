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
package com.espertech.esper.epl.metric;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestMetricScheduleService extends TestCase {
    private MetricScheduleService svc;
    private SupportMetricExecution execs[];
    private List<MetricExec> executions;

    public void setUp() {
        svc = new MetricScheduleService();

        execs = new SupportMetricExecution[100];
        for (int i = 0; i < execs.length; i++) {
            execs[i] = new SupportMetricExecution();
        }

        executions = new ArrayList<MetricExec>();
    }

    public void testFlow() {
        svc.setTime(1000);
        assertNull(svc.getNearestTime());

        svc.add(2000, execs[0]);
        assertEquals(3000, (long) svc.getNearestTime());

        svc.add(2100, execs[1]);
        assertEquals(3000, (long) svc.getNearestTime());

        svc.add(2000, execs[2]);
        assertEquals(3000, (long) svc.getNearestTime());

        svc.setTime(1100);
        svc.add(100, execs[3]);
        assertEquals(1200, (long) svc.getNearestTime());

        svc.setTime(1199);
        svc.evaluate(executions);
        assertTrue(executions.isEmpty());

        svc.setTime(1200);
        svc.evaluate(executions);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{execs[3]}, executions.iterator());
        assertEquals(3000, (long) svc.getNearestTime());

        executions.clear();
        svc.setTime(2999);
        svc.evaluate(executions);
        assertTrue(executions.isEmpty());

        svc.setTime(3000);
        svc.evaluate(executions);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{execs[0], execs[2]}, executions.iterator());
        assertEquals(3100, (long) svc.getNearestTime());

        svc.clear();
        assertNull(svc.getNearestTime());

        executions.clear();
        svc.setTime(Long.MAX_VALUE - 1);
        svc.evaluate(executions);
        assertTrue(executions.isEmpty());
        assertNull(svc.getNearestTime());
    }
}
