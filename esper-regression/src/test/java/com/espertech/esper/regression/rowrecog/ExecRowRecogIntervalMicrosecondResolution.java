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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportEngineFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecRowRecogIntervalMicrosecondResolution implements RegressionExecution {

    public void run(EPServiceProvider defaultService) throws Exception {
        Map<TimeUnit, EPServiceProvider> epServices = SupportEngineFactory.setupEnginesByTimeUnit();

        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        }

        runAssertionWithTime(epServices.get(TimeUnit.MILLISECONDS), 0, 10000);
        runAssertionWithTime(epServices.get(TimeUnit.MICROSECONDS), 0, 10000000);
    }

    private void runAssertionWithTime(EPServiceProvider epService, long startTime, long flipTime) {
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));

        String text = "select * from SupportBean " +
                "match_recognize (" +
                " measures A as a" +
                " pattern (A*)" +
                " interval 10 seconds" +
                ")";

        EPStatement stmt = isolated.getEPAdministrator().createEPL(text, "s0", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime - 1));
        assertFalse(listener.getIsInvokedAndReset());

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime));
        assertTrue(listener.getIsInvokedAndReset());

        isolated.destroy();
    }
}
