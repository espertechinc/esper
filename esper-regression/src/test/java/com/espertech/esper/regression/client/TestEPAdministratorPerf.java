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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestEPAdministratorPerf extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableTimerDebug(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    public void test1kValidStmtsPerformance()
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            String text = "select * from " + SupportBean.class.getName();
            EPStatement stmt = epService.getEPAdministrator().createEPL(text, "s1");
            assertEquals("s1", stmt.getName());
            stmt.stop();
            stmt.start();
            stmt.stop();
            stmt.destroy();
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue(".test10kValid delta=" + delta, delta < 5000);
    }

    public void test1kInvalidStmts()
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++)
        {
            try
            {
                String text = "select xxx from " + SupportBean.class.getName();
                epService.getEPAdministrator().createEPL(text, "s1");
            }
            catch (Exception ex)
            {
                // expected
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue(".test1kInvalid delta=" + delta, delta < 2500);
    }
}
