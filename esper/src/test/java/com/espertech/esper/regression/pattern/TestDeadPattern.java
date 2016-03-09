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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.bean.SupportBean_C;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class TestDeadPattern extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("A", SupportBean_A.class.getName());
        config.addEventType("B", SupportBean_B.class.getName());
        config.addEventType("C", SupportBean_C.class.getName());

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    public void testDeadPattern()
    {
        String pattern = "(A() -> B()) and not C()";
        // Adjust to 20000 to better test the limit
        for (int i = 0; i < 1000; i++)
        {
            epService.getEPAdministrator().createPattern(pattern);
        }

        epService.getEPRuntime().sendEvent(new SupportBean_C("C1"));

        long startTime = System.currentTimeMillis();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        long delta =  System.currentTimeMillis() - startTime;

        log.info(".testDeadPattern delta=" + delta);
        assertTrue("performance: delta=" +  delta, delta < 20);
    }

    private static Log log = LogFactory.getLog(TestDeadPattern.class);
}
