/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;

public class TestThreadedConfigTimer extends TestCase
{
    private static final Log log = LogFactory.getLog(TestThreadedConfigTimer.class);

    public void testOp() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestThreadedConfigTimer.class)) {
            return;
        }

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        config.getEngineDefaults().getExpression().setUdfCache(false);
        config.getEngineDefaults().getThreading().setThreadPoolTimerExec(true);
        config.getEngineDefaults().getThreading().setThreadPoolTimerExecNumThreads(5);
        config.addEventType("MyMap", new HashMap<String, Object>());
        config.addImport(SupportStaticMethodLib.class.getName());

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        sendTimer(0, epService);

        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        for (int i = 0; i < countStatements; i++)
        {
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
        while(true)
        {
            int countDelivered = listener.getNewEvents().size();
            if (countDelivered == countStatements)
            {
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

    private void sendTimer(long timeInMSec, EPServiceProvider epService)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
