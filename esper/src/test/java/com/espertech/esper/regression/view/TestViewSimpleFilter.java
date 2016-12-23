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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewSimpleFilter extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testNotEqualsOp()
    {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                "(theString != 'a')");
        statement.addListener(testListener);

        sendEvent("a");
        assertFalse(testListener.isInvoked());

        Object theEvent = sendEvent("b");
        assertSame(theEvent, testListener.getAndResetLastNewData()[0].getUnderlying());

        sendEvent("a");
        assertFalse(testListener.isInvoked());

        theEvent = sendEvent(null);
        assertFalse(testListener.isInvoked());
    }

    public void testCombinationEqualsOp()
    {
        EPStatement statement = epService.getEPAdministrator().createEPL(
                "select * from " + SupportBean.class.getName() +
                "(theString != 'a', intPrimitive=0)");
        statement.addListener(testListener);

        sendEvent("b", 1);
        assertFalse(testListener.isInvoked());

        sendEvent("a", 0);
        assertFalse(testListener.isInvoked());

        Object theEvent = sendEvent("x", 0);
        assertSame(theEvent, testListener.getAndResetLastNewData()[0].getUnderlying());

        theEvent = sendEvent(null, 0);
        assertFalse(testListener.isInvoked());
    }

    private Object sendEvent(String stringValue)
    {
        return sendEvent(stringValue, -1);
    }

    private Object sendEvent(String stringValue, int intPrimitive)
    {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(stringValue);
        theEvent.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
