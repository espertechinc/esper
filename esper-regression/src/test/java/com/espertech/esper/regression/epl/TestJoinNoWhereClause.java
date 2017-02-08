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

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.Configuration;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestJoinNoWhereClause extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private Object[] setOne;
    private Object[] setTwo;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);
        config.getEngineDefaults().getViewResources().setShareViews(false);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        setOne = new Object[5];
        setTwo = new Object[5];
        for (int i = 0; i < setOne.length; i++)
        {
            setOne[i] = new SupportMarketDataBean("IBM", 0, (long) i, "");

            SupportBean theEvent = new SupportBean();
            theEvent.setLongBoxed((long) i);
            setTwo[i] = theEvent;
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
        setOne = null;
        setTwo = null;
    }

    public void testJoinNoWhereClause()
    {
        String[] fields = new String[] {"stream_0.volume", "stream_1.longBoxed"};
        String joinStatement = "select * from " +
                SupportMarketDataBean.class.getName() + "#length(3)," +
                SupportBean.class.getName() + "()#length(3)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        // Send 2 events, should join on second one
        sendEvent(setOne[0]);
        EPAssertionUtil.assertPropsPerRowAnyOrder(joinView.iterator(), fields, null);

        sendEvent(setTwo[0]);
        assertEquals(1, updateListener.getLastNewData().length);
        assertEquals(setOne[0], updateListener.getLastNewData()[0].get("stream_0"));
        assertEquals(setTwo[0], updateListener.getLastNewData()[0].get("stream_1"));
        updateListener.reset();
        EPAssertionUtil.assertPropsPerRowAnyOrder(joinView.iterator(), fields,
                new Object[][]{{0L, 0L}});

        sendEvent(setOne[1]);
        sendEvent(setOne[2]);
        sendEvent(setTwo[1]);
        assertEquals(3, updateListener.getLastNewData().length);
        EPAssertionUtil.assertPropsPerRowAnyOrder(joinView.iterator(), fields,
                new Object[][]{{0L, 0L},
                        {1L, 0L},
                        {2L, 0L},
                        {0L, 1L},
                        {1L, 1L},
                        {2L, 1L}});
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
