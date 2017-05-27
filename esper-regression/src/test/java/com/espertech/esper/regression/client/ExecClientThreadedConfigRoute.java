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
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.client.SupportListenerTimerHRes;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientThreadedConfigRoute implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecClientThreadedConfigRoute.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.getEngineDefaults().getThreading().setThreadPoolRouteExec(true);
        configuration.getEngineDefaults().getThreading().setThreadPoolRouteExecNumThreads(5);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addImport(SupportStaticMethodLib.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecClientThreadedConfigRoute.class)) {
            return;
        }

        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        for (int i = 0; i < countStatements; i++) {
            EPStatement stmt = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(10) from SupportBean");
            stmt.addListener(listener);
        }

        log.info("Sending trigger event");
        long start = System.nanoTime();
        epService.getEPRuntime().sendEvent(new SupportBean());
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        Thread.sleep(2000);
        assertEquals(100, listener.getNewEvents().size());
        listener.getNewEvents().clear();

        // destroy all statements
        epService.getEPAdministrator().destroyAllStatements();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(10) from SupportBean, SupportBean");
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        Thread.sleep(100);
        assertEquals(1, listener.getNewEvents().size());
    }
}
