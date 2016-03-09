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

import junit.framework.TestCase;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import com.espertech.esper.client.*;
import com.espertech.esper.regression.client.SupportListenerTimerHRes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestThreadedConfigRoute extends TestCase
{
    private static final Log log = LogFactory.getLog(TestThreadedConfigRoute.class);

    public void testOp() throws Exception
    {
        if (SupportConfigFactory.skipTest(TestThreadedConfigRoute.class)) {
            return;
        }

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        config.getEngineDefaults().getExpression().setUdfCache(false);
        config.getEngineDefaults().getThreading().setThreadPoolRouteExec(true);
        config.getEngineDefaults().getThreading().setThreadPoolRouteExecNumThreads(5);
        config.addEventType("SupportBean", SupportBean.class);
        config.addImport(SupportStaticMethodLib.class.getName());

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        for (int i = 0; i < countStatements; i++)
        {
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

        epService.destroy();
    }
}
