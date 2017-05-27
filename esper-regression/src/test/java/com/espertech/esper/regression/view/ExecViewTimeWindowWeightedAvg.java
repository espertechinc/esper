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
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.DoubleValueAssertionUtil;
import com.espertech.esper.view.ViewFieldEnum;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecViewTimeWindowWeightedAvg implements RegressionExecution {
    private final static String SYMBOL = "CSCO.O";
    private final static String FEED = "feed1";

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // Set up a 1 second time window
        EPStatement weightedAvgView = epService.getEPAdministrator().createEPL(
                "select * from " + SupportMarketDataBean.class.getName() +
                        "(symbol='" + SYMBOL + "')#time(3.0)#weighted_avg(price, volume, symbol, feed)");
        SupportUpdateListener testListener = new SupportUpdateListener();
        weightedAvgView.addListener(testListener);

        assertEquals(Double.class, weightedAvgView.getEventType().getPropertyType("average"));
        testListener.reset();

        // Send 2 events, E1 and E2 at +0sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10, 500));
        checkValue(epService, testListener, weightedAvgView, 10);

        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 11, 500));
        checkValue(epService, testListener, weightedAvgView, 10.5);

        // Sleep for 1.5 seconds
        sleep(1500);

        // Send 2 more events, E3 and E4 at +1.5sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10, 1000));
        checkValue(epService, testListener, weightedAvgView, 10.25);
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10.5, 2000));
        checkValue(epService, testListener, weightedAvgView, 10.375);

        // Sleep for 2 seconds, E1 and E2 should have left the window
        sleep(2000);
        checkValue(epService, testListener, weightedAvgView, 10.333333333);

        // Send another event, E5 at +3.5sec
        epService.getEPRuntime().sendEvent(makeBean(SYMBOL, 10.2, 1000));
        checkValue(epService, testListener, weightedAvgView, 10.3);

        // Sleep for 2.5 seconds, E3 and E4 should expire
        sleep(2500);
        checkValue(epService, testListener, weightedAvgView, 10.2);

        // Sleep for 1 seconds, E5 should have expired
        sleep(1000);
        checkValue(epService, testListener, weightedAvgView, Double.NaN);
    }

    private SupportMarketDataBean makeBean(String symbol, double price, long volume) {
        return new SupportMarketDataBean(symbol, price, volume, FEED);
    }

    private void checkValue(EPServiceProvider epService, SupportUpdateListener testListener, EPStatement weightedAvgView, double avgE) {
        Iterator<EventBean> iterator = weightedAvgView.iterator();
        checkValue(iterator.next(), avgE);
        assertTrue(!iterator.hasNext());

        assertTrue(testListener.getLastNewData().length == 1);
        EventBean listenerValues = testListener.getLastNewData()[0];
        checkValue(listenerValues, avgE);

        testListener.reset();
    }

    private void checkValue(EventBean values, double avgE) {
        double avg = getDoubleValue(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE, values);
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
        assertEquals(FEED, values.get("feed"));
        assertEquals(SYMBOL, values.get("symbol"));
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
