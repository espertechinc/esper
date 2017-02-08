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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewStartStop extends TestCase
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

    public void testSameWindowReuse()
    {
        String viewExpr = "select * from " + SupportBean.class.getName() + "#length(3)";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(viewExpr);
        stmtOne.addListener(testListener);

        // send a couple of events
        sendEvent(1);
        sendEvent(2);
        sendEvent(3);
        sendEvent(4);

        // create same statement again
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(viewExpr);
        stmtTwo.addListener(testListenerTwo);

        // Send event, no old data should be received
        sendEvent(5);
        assertNull(testListenerTwo.getLastOldData());
    }

    public void testStartStop()
    {
        String viewExpr = "select count(*) as size from " + SupportBean.class.getName();
        EPStatement sizeView = epService.getEPAdministrator().createEPL(viewExpr);

        // View created is automatically started
        assertEquals(0l, sizeView.iterator().next().get("size"));
        sizeView.stop();

        // Send an event, view stopped
        sendEvent();
        assertNull(sizeView.iterator());

        // Start view
        sizeView.start();
        assertEquals(0l, sizeView.iterator().next().get("size"));

        // Send event
        sendEvent();
        assertEquals(1l, sizeView.iterator().next().get("size"));

        // Stop view
        sizeView.stop();
        assertNull(sizeView.iterator());

        // Start again, iterator is zero
        sizeView.start();
        assertEquals(0l, sizeView.iterator().next().get("size"));
    }

    public void testAddRemoveListener()
    {
        String viewExpr = "select count(*) as size from " + SupportBean.class.getName();
        EPStatement sizeView = epService.getEPAdministrator().createEPL(viewExpr);

        // View is started when created

        // Add listener send event
        sizeView.addListener(testListener);
        assertNull(testListener.getLastNewData());
        assertEquals(0l, sizeView.iterator().next().get("size"));
        sendEvent();
        assertEquals(1l, testListener.getAndResetLastNewData()[0].get("size"));
        assertEquals(1l, sizeView.iterator().next().get("size"));

        // Stop view, send event, view
        sizeView.stop();
        sendEvent();
        assertNull(sizeView.iterator());
        assertNull(testListener.getLastNewData());

        // Start again
        sizeView.removeListener(testListener);
        sizeView.addListener(testListener);
        sizeView.start();

        sendEvent();
        assertEquals(1l, testListener.getAndResetLastNewData()[0].get("size"));
        assertEquals(1l, sizeView.iterator().next().get("size"));

        // Stop again, leave listeners
        sizeView.stop();
        sizeView.start();
        sendEvent();
        assertEquals(1l, testListener.getAndResetLastNewData()[0].get("size"));

        // Remove listener, send event
        sizeView.removeListener(testListener);
        sendEvent();
        assertNull(testListener.getLastNewData());

        // Add listener back, send event
        sizeView.addListener(testListener);
        sendEvent();
        assertEquals(3l, testListener.getAndResetLastNewData()[0].get("size"));
    }

    private void sendEvent()
    {
        sendEvent(-1);
    }

    private void sendEvent(int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
