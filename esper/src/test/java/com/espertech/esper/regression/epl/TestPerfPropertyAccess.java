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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBeanCombinedProps;
import com.espertech.esper.support.client.SupportConfigFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
                SupportBeanCombinedProps.class.getName() + ".win:length(1)" +
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

    private static final Log log = LogFactory.getLog(TestPerfPropertyAccess.class);
}
