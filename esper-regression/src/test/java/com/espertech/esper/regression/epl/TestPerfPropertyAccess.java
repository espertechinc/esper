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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import junit.framework.TestCase;

public class TestPerfPropertyAccess extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        updateListener = null;
    }

    public void testPerfPropertyAccess()
    {
        String methodName = ".testPerfPropertyAccess";

        String joinStatement = "select * from " +
                SupportBeanCombinedProps.class.getName() + "#length(1)" +
            " where indexed[0].mapped('a').value = 'dummy'";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        // Send events for each stream
        SupportBeanCombinedProps theEvent = SupportBeanCombinedProps.makeDefaultBean();
        log.info(methodName + " Sending events");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
        {
            sendEvent(theEvent);
        }
        log.info(methodName + " Done sending events");

        long endTime = System.currentTimeMillis();
        log.info(methodName + " delta=" + (endTime - startTime));

        // Stays at 250, below 500ms
        assertTrue((endTime - startTime) < 1000);
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Logger log = LoggerFactory.getLogger(TestPerfPropertyAccess.class);
}
