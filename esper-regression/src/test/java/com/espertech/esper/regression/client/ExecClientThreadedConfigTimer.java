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
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.client.SupportListenerTimerHRes;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientThreadedConfigTimer implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecClientThreadedConfigTimer.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.getEngineDefaults().getThreading().setThreadPoolTimerExec(true);
        configuration.getEngineDefaults().getThreading().setThreadPoolTimerExecNumThreads(5);
        configuration.addEventType("MyMap", new HashMap<>());
        configuration.addImport(SupportStaticMethodLib.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        if (SupportConfigFactory.skipTest(ExecClientThreadedConfigTimer.class)) {
            return;
        }
        sendTimer(0, epService);

        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        for (int i = 0; i < countStatements; i++) {
            EPStatement stmt = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(10) from pattern[every MyMap -> timer:interval(1)]");
            stmt.addListener(listener);
        }

        log.info("Sending trigger event");
        epService.getEPRuntime().sendEvent(new HashMap<String, Object>(), "MyMap");

        long start = System.nanoTime();
        sendTimer(1000, epService);
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        // wait for delivery
        while (true) {
            int countDelivered = listener.getNewEvents().size();
            if (countDelivered == countStatements) {
                break;
            }

            log.info("Delivered " + countDelivered + ", waiting for more");
            Thread.sleep(200);
        }

        assertEquals(100, listener.getNewEvents().size());
        // analyze result
        //List<Pair<Long, EventBean[]>> events = listener.getNewEvents();
        //OccuranceResult result = OccuranceAnalyzer.analyze(events, new long[] {100 * 1000 * 1000L, 10*1000 * 1000L});
        //log.info(result);
    }

    private void sendTimer(long timeInMSec, EPServiceProvider epService) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
