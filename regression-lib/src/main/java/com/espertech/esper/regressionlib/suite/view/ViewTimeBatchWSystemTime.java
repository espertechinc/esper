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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.view.derived.ViewFieldEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.util.DoubleValueAssertionUtil;

import static org.junit.Assert.*;

public class ViewTimeBatchWSystemTime implements RegressionExecution {
    private final static String SYMBOL = "CSCO.O";

    public void run(RegressionEnvironment env) {
        // Set up a 2 second time window
        String epl = "@name('s0') select * from SupportMarketDataBean(symbol='" + SYMBOL + "')#time_batch(2)#uni(volume)";
        env.compileDeployAddListenerMileZero(epl, "s0");

        checkMeanIterator(env, Double.NaN);
        env.assertListenerNotInvoked("s0");

        // Send a couple of events, check mean
        sendEvent(env, SYMBOL, 500);
        sendEvent(env, SYMBOL, 1000);
        checkMeanIterator(env, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        env.assertListenerNotInvoked("s0");      // No new data posted to the iterator, yet

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(env, SYMBOL, 1000);
        sendEvent(env, SYMBOL, 1200);
        checkMeanIterator(env, Double.NaN);              // The iterator is still showing no result yet as no batch was released
        env.assertListenerNotInvoked("s0");

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(env, 925);                 // Now the statistics view received the first batch
        checkMeanListener(env, 925);

        // Send more events
        sendEvent(env, SYMBOL, 500);
        sendEvent(env, SYMBOL, 600);
        sendEvent(env, SYMBOL, 1000);
        checkMeanIterator(env, 925);              // The iterator is still showing the old result as next batch not released
        env.assertListenerNotInvoked("s0");

        // Sleep for 1 seconds
        sleep(1000);

        // Send more events
        sendEvent(env, SYMBOL, 200);
        checkMeanIterator(env, 925);
        env.assertListenerNotInvoked("s0");

        // Sleep for 1.5 seconds, thus triggering a new batch
        sleep(1500);
        checkMeanIterator(env, 2300d / 4d); // Now the statistics view received the second batch, the mean now is over all events
        checkMeanListener(env, 2300d / 4d);

        // Send more events
        sendEvent(env, SYMBOL, 1200);
        checkMeanIterator(env, 2300d / 4d);
        env.assertListenerNotInvoked("s0");

        // Sleep for 2 seconds, no events received anymore
        sleep(2000);
        checkMeanIterator(env, 1200); // statistics view received the third batch
        checkMeanListener(env, 1200);

        env.undeployAll();
    }

    private void sendEvent(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(theEvent);
    }

    private void checkMeanListener(RegressionEnvironment env, double meanExpected) {
        env.assertListener("s0", listener -> {
            assertEquals(1, listener.getLastNewData().length);
            EventBean listenerValues = listener.getLastNewData()[0];
            checkValue(listenerValues, meanExpected);
            listener.reset();
        });
    }

    private void checkMeanIterator(RegressionEnvironment env, double meanExpected) {
        env.assertIterator("s0", iterator -> {
            checkValue(iterator.next(), meanExpected);
            assertFalse(iterator.hasNext());
        });
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
