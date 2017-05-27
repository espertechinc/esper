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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportListenerSleeping;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientThreadedConfigOutbound implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.getEngineDefaults().getThreading().setThreadPoolOutbound(true);
        configuration.getEngineDefaults().getThreading().setThreadPoolOutboundNumThreads(5);
        configuration.addEventType("SupportBean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportListenerSleeping listener = new SupportListenerSleeping(200);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmt.addListener(listener);

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        Thread.sleep(1000);
        assertEquals(5, listener.getNewEvents().size());
    }
}
