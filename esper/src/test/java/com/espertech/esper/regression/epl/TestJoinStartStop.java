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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_A;
import com.espertech.esper.support.bean.SupportBean_B;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.core.service.EPServiceProviderSPI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

public class TestJoinStartStop extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private Object[] setOne = new Object[5];
    private Object[] setTwo = new Object[5];

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        long[] volumesOne = new long[] { 10, 20, 20, 40, 50 };
        long[] volumesTwo = new long[] { 10, 20, 30, 40, 50 };

        for (int i = 0; i < setOne.length; i++)
        {
            setOne[i] = new SupportMarketDataBean("IBM", volumesOne[i], (long) i, "");
            setTwo[i] = new SupportMarketDataBean("CSCO", volumesTwo[i], (long) i, "");
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testJoinUniquePerId()
    {
        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "(symbol='IBM').win:length(3) s0, " +
                SupportMarketDataBean.class.getName() + "(symbol='CSCO').win:length(3) s1" +
            " where s0.volume=s1.volume";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement, "MyJoin");
        joinView.addListener(updateListener);

        sendEvent(setOne[0]);
        sendEvent(setTwo[0]);
        assertNotNull(updateListener.getLastNewData());
        updateListener.reset();

        joinView.stop();
        sendEvent(setOne[1]);
        sendEvent(setTwo[1]);
        assertFalse(updateListener.isInvoked());

        joinView.start();
        sendEvent(setOne[2]);
        assertFalse(updateListener.isInvoked());

        joinView.stop();
        sendEvent(setOne[3]);
        sendEvent(setOne[4]);
        sendEvent(setTwo[3]);

        joinView.start();
        sendEvent(setTwo[4]);
        assertFalse(updateListener.isInvoked());

        // assert type-statement reference
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportMarketDataBean.class.getName()));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportMarketDataBean.class.getName());
        assertTrue(stmtNames.contains("MyJoin"));

        joinView.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportMarketDataBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportMarketDataBean.class.getName());
        EPAssertionUtil.assertEqualsAnyOrder(null, stmtNames.toArray());
        assertFalse(stmtNames.contains("MyJoin"));
    }

    public void testInvalidJoin()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);

        String invalidJoin = "select * from A, B";
        tryInvalid(invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for A [select * from A, B]");

        invalidJoin = "select * from A.win:time(5 min), B";
        tryInvalid(invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for B [select * from A.win:time(5 min), B]");

        invalidJoin = "select * from A.win:time(5 min), pattern[A->B]";
        tryInvalid(invalidJoin,
                "Error starting statement: Joins require that at least one view is specified for each stream, no view was specified for pattern event stream [select * from A.win:time(5 min), pattern[A->B]]");
    }

    private void tryInvalid(String invalidJoin, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(invalidJoin);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private static final Log log = LogFactory.getLog(TestJoinStartStop.class);
}
