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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewSize extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSize()
    {
        String statementText = "select irstream size from " + SupportMarketDataBean.class.getName() + "#size";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);

        sendEvent("DELL", 1L);
        assertSize(1, 0);

        sendEvent("DELL", 1L);
        assertSize(2, 1);

        selectTestView.destroy();
        statementText = "select size, symbol, feed from " + SupportMarketDataBean.class.getName() + "#size(symbol, feed)";
        selectTestView = epService.getEPAdministrator().createEPL(statementText);
        selectTestView.addListener(listener);
        String[] fields = "size,symbol,feed".split(",");

        sendEvent("DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, "DELL", "f1"});

        sendEvent("DELL", 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2L, "DELL", "f1"});
    }

    private void sendEvent(String symbol, Long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "f1");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertSize(long newSize, long oldSize)
    {
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), "size", new Object[]{newSize}, new Object[]{oldSize});
    }
}
