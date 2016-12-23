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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.DoubleValueAssertionUtil;
import com.espertech.esper.view.ViewFieldEnum;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestViewTimeWindowWeightedAvg extends TestCase
{
    private static String SYMBOL = "CSCO.O";
    private static String FEED = "feed1";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testWindowStats()
    {
        // Set up a 1 second time window
        EPStatement weightedAvgView = epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() +
                "(symbol='" + SYMBOL + "')#time(3.0)#weighted_avg(price, volume, symbol, feed)");
        weightedAvgView.addListener(testListener);
        
        assertEquals(Double.class, weightedAvgView.getEventType().getPropertyType("average"));
        testListener.reset();

        // Send 2 events, E1 and E2 at +0sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10, 500));
        checkValue(weightedAvgView, 10);

        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 11, 500));
        checkValue(weightedAvgView, 10.5);

        // Sleep for 1.5 seconds
        sleep(1500);

        // Send 2 more events, E3 and E4 at +1.5sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10, 1000));
        checkValue(weightedAvgView, 10.25);
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10.5, 2000));
        checkValue(weightedAvgView, 10.375);

        // Sleep for 2 seconds, E1 and E2 should have left the window
        sleep(2000);
        checkValue(weightedAvgView, 10.333333333);

        // Send another event, E5 at +3.5sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10.2, 1000));
        checkValue(weightedAvgView, 10.3);

        // Sleep for 2.5 seconds, E3 and E4 should expire
        sleep(2500);
        checkValue(weightedAvgView, 10.2);

        // Sleep for 1 seconds, E5 should have expired
        sleep(1000);
        checkValue(weightedAvgView, Double.NaN);
    }

    private SupportMarketDataBean makeBean(String symbol, double price, long volume)
    {
        return new SupportMarketDataBean(symbol, price, volume, FEED);
    }

    private void checkValue(EPStatement weightedAvgView, double avgE)
    {
        Iterator<EventBean> iterator = weightedAvgView.iterator();
        checkValue(iterator.next(), avgE);
        assertTrue(iterator.hasNext() == false);

        assertTrue(testListener.getLastNewData().length == 1);
        EventBean listenerValues = testListener.getLastNewData()[0];
        checkValue(listenerValues, avgE);

        testListener.reset();
    }

    private void checkValue(EventBean values, double avgE)
    {
        double avg = getDoubleValue(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE, values);
        assertTrue(DoubleValueAssertionUtil.equals(avg,  avgE, 6));
        assertEquals(FEED, values.get("feed"));
        assertEquals(SYMBOL, values.get("symbol"));
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean theEvent)
    {
        return  (Double) theEvent.get(field.getName());
    }

    private void sleep(int msec)
    {
        try
        {
            Thread.sleep(msec);
        }
        catch (InterruptedException e)
        {
        }
    }
}
