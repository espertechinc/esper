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
import com.espertech.esper.client.*;

public class TestThreadedConfigOutbound extends TestCase
{
    public void testOp() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        config.getEngineDefaults().getExpression().setUdfCache(false);
        config.getEngineDefaults().getThreading().setThreadPoolOutbound(true);
        config.getEngineDefaults().getThreading().setThreadPoolOutboundNumThreads(5);
        config.addEventType("SupportBean", SupportBean.class);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        SupportListenerSleeping listener = new SupportListenerSleeping(200);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean");
        stmt.addListener(listener);

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportBean());
        }
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        Thread.sleep(1000);
        assertEquals(5, listener.getNewEvents().size());

        epService.destroy();
    }
}
