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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecVariablesTimer implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", long.class, "12");
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Long.class, "2");
        epService.getEPAdministrator().getConfiguration().addVariable("var3", Long.class, null);

        long startTime = System.currentTimeMillis();
        String stmtTextSet = "on pattern [every timer:interval(100 milliseconds)] set var1 = current_timestamp, var2 = var1 + 1, var3 = var1 + var2";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        SupportUpdateListener listenerSet = new SupportUpdateListener();
        stmtSet.addListener(listenerSet);

        Thread.sleep(1000);
        stmtSet.destroy();

        EventBean[] received = listenerSet.getNewDataListFlattened();
        assertTrue("received : " + received.length, received.length >= 5);

        for (int i = 0; i < received.length; i++) {
            long var1 = (Long) received[i].get("var1");
            long var2 = (Long) received[i].get("var2");
            long var3 = (Long) received[i].get("var3");
            assertTrue(var1 >= startTime);
            assertEquals(var1, var2 - 1);
            assertEquals(var3, var2 + var1);
        }
    }
}
