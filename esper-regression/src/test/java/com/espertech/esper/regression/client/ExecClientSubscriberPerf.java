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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientSubscriberPerf implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        String pkg = SupportBean.class.getPackage().getName();
        configuration.addEventTypeAutoName(pkg);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPerformanceSyntheticUndelivered(epService);
        runAssertionPerformanceSynthetic(epService);
    }

    private void runAssertionPerformanceSyntheticUndelivered(EPServiceProvider epService) {
        final int numLoop = 100000;
        epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean(intPrimitive > 10)");

        long start = System.currentTimeMillis();
        for (int i = 0; i < numLoop; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000 + i));
        }
        long end = System.currentTimeMillis();

        assertTrue("delta=" + (end - start), end - start < 1000);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPerformanceSynthetic(EPServiceProvider epService) {
        final int numLoop = 100000;
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean(intPrimitive > 10)");
        final List<Object[]> results = new ArrayList<Object[]>();

        UpdateListener listener = new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                String theString = (String) newEvents[0].get("theString");
                int val = (Integer) newEvents[0].get("intPrimitive");
                results.add(new Object[]{theString, val});
            }
        };
        stmt.addListener(listener);

        long start = System.currentTimeMillis();
        for (int i = 0; i < numLoop; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", 1000 + i));
        }
        long end = System.currentTimeMillis();

        assertEquals(numLoop, results.size());
        for (int i = 0; i < numLoop; i++) {
            EPAssertionUtil.assertEqualsAnyOrder(results.get(i), new Object[]{"E1", 1000 + i});
        }
        assertTrue("delta=" + (end - start), end - start < 1000);

        stmt.destroy();
    }
}
