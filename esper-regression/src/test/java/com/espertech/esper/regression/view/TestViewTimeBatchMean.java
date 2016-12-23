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

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;

import java.util.Iterator;

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.util.DoubleValueAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.view.ViewFieldEnum;
import com.espertech.esper.client.EventBean;

public class TestViewTimeBatchMean extends TestCase
{
    private static String SYMBOL = "CSCO.O";

    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        Configuration config = new Configuration();
        config.addEventType("SupportBean", SupportBean.class);
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        assertFalse(epService.getEPRuntime().isExternalClockingEnabled());
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testTimeBatchMean()
    {
        // Set up a 2 second time window
        EPStatement timeBatchMean = epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() +
                "(symbol='" + SYMBOL + "')#time_batch(2)#uni(volume)");
        timeBatchMean.addListener(testListener);

        testListener.reset();
        checkMeanIterator(timeBatchMean, Double.NaN);
        assertFalse(testListener.isInvoked());

        // Send a couple of events, check mean
        sendEvent(SYMBOL, 500);
        sendEvent(SYMBOL, 1000);
        checkMeanIterator(timeBatchMean, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        assertFalse(testListener.isInvoked());      // No new data posted to the iterator, yet

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(SYMBOL, 1000);
        sendEvent(SYMBOL, 1200);
        checkMeanIterator(timeBatchMean, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        assertFalse(testListener.isInvoked());

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(timeBatchMean, 925);                 // Now the statistics view received the first batch
        assertTrue(testListener.isInvoked());   // Listener has been invoked
        checkMeanListener(925);

        // Send more events
        sendEvent(SYMBOL, 500);
        sendEvent(SYMBOL, 600);
        sendEvent(SYMBOL, 1000);
        checkMeanIterator(timeBatchMean, 925);              // The iterator is still showing the old result as next batch not released
        assertFalse(testListener.isInvoked());

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(SYMBOL, 200);
        checkMeanIterator(timeBatchMean, 925);
        assertFalse(testListener.isInvoked());

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(timeBatchMean, 2300d / 4d); // Now the statistics view received the second batch, the mean now is over all events
        assertTrue(testListener.isInvoked());   // Listener has been invoked
        checkMeanListener(2300d / 4d);

        // Send more events
        sendEvent(SYMBOL, 1200);
        checkMeanIterator(timeBatchMean, 2300d / 4d);
        assertFalse(testListener.isInvoked());

        // Sleep for 2 seconds, no events received anymore
        sleep(2000);
        checkMeanIterator(timeBatchMean, 1200); // statistics view received the third batch
        assertTrue(testListener.isInvoked());   // Listener has been invoked
        checkMeanListener(1200);

        // try to compile with flow control, these are tested elsewhere
        epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(10 sec, 'FORCE_UPDATE, START_EAGER')");
    }

    private void sendEvent(String symbol, long volume)
    {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void checkMeanListener(double meanExpected)
    {
        assertTrue(testListener.getLastNewData().length == 1);
        EventBean listenerValues = testListener.getLastNewData()[0];
        checkValue(listenerValues, meanExpected);
        testListener.reset();
    }

    private void checkMeanIterator(EPStatement timeBatchMean, double meanExpected)
    {
        Iterator<EventBean> iterator = timeBatchMean.iterator();
        checkValue(iterator.next(), meanExpected);
        assertTrue(iterator.hasNext() == false);
    }

    private void checkValue(EventBean values, double avgE)
    {
        double avg = getDoubleValue(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE, values);
        assertTrue(DoubleValueAssertionUtil.equals(avg,  avgE, 6));
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean theEvent)
    {
        return (Double) theEvent.get(field.getName());
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
