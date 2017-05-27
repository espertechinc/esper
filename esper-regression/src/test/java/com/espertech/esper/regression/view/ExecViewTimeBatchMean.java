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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.DoubleValueAssertionUtil;
import com.espertech.esper.view.ViewFieldEnum;

import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecViewTimeBatchMean implements RegressionExecution {
    private final static String SYMBOL = "CSCO.O";

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        // Set up a 2 second time window
        EPStatement timeBatchMean = epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() +
                        "(symbol='" + SYMBOL + "')#time_batch(2)#uni(volume)");
        SupportUpdateListener listener = new SupportUpdateListener();
        timeBatchMean.addListener(listener);

        listener.reset();
        checkMeanIterator(timeBatchMean, Double.NaN);
        assertFalse(listener.isInvoked());

        // Send a couple of events, check mean
        sendEvent(epService, SYMBOL, 500);
        sendEvent(epService, SYMBOL, 1000);
        checkMeanIterator(timeBatchMean, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        assertFalse(listener.isInvoked());      // No new data posted to the iterator, yet

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(epService, SYMBOL, 1000);
        sendEvent(epService, SYMBOL, 1200);
        checkMeanIterator(timeBatchMean, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        assertFalse(listener.isInvoked());

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(timeBatchMean, 925);                 // Now the statistics view received the first batch
        assertTrue(listener.isInvoked());   // Listener has been invoked
        checkMeanListener(listener, 925);

        // Send more events
        sendEvent(epService, SYMBOL, 500);
        sendEvent(epService, SYMBOL, 600);
        sendEvent(epService, SYMBOL, 1000);
        checkMeanIterator(timeBatchMean, 925);              // The iterator is still showing the old result as next batch not released
        assertFalse(listener.isInvoked());

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(epService, SYMBOL, 200);
        checkMeanIterator(timeBatchMean, 925);
        assertFalse(listener.isInvoked());

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(timeBatchMean, 2300d / 4d); // Now the statistics view received the second batch, the mean now is over all events
        assertTrue(listener.isInvoked());   // Listener has been invoked
        checkMeanListener(listener, 2300d / 4d);

        // Send more events
        sendEvent(epService, SYMBOL, 1200);
        checkMeanIterator(timeBatchMean, 2300d / 4d);
        assertFalse(listener.isInvoked());

        // Sleep for 2 seconds, no events received anymore
        sleep(2000);
        checkMeanIterator(timeBatchMean, 1200); // statistics view received the third batch
        assertTrue(listener.isInvoked());   // Listener has been invoked
        checkMeanListener(listener, 1200);

        // try to compile with flow control, these are tested elsewhere
        epService.getEPAdministrator().createEPL("select * from SupportBean#time_batch(10 sec, 'FORCE_UPDATE, START_EAGER')");
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void checkMeanListener(SupportUpdateListener listener, double meanExpected) {
        assertTrue(listener.getLastNewData().length == 1);
        EventBean listenerValues = listener.getLastNewData()[0];
        checkValue(listenerValues, meanExpected);
        listener.reset();
    }

    private void checkMeanIterator(EPStatement timeBatchMean, double meanExpected) {
        Iterator<EventBean> iterator = timeBatchMean.iterator();
        checkValue(iterator.next(), meanExpected);
        assertTrue(!iterator.hasNext());
    }

    private void checkValue(EventBean values, double avgE) {
        double avg = getDoubleValue(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE, values);
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean theEvent) {
        return (Double) theEvent.get(field.getName());
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
        }
    }
}
