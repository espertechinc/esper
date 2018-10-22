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
import org.junit.Assert;

import java.util.Iterator;

import static org.junit.Assert.assertTrue;

public class ViewTimeWinWSystemTime implements RegressionExecution {
    private final static String SYMBOL = "CSCO.O";
    private final static String FEED = "feed1";

    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select * from SupportMarketDataBean(symbol='" + SYMBOL + "')#time(3.0)#weighted_avg(price, volume, symbol, feed)";
        env.compileDeployAddListenerMileZero(epl, "s0");

        Assert.assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("average"));

        // Send 2 events, E1 and E2 at +0sec
        env.sendEventBean(makeBean(SYMBOL, 10, 500));
        checkValue(env, 10);

        env.sendEventBean(makeBean(SYMBOL, 11, 500));
        checkValue(env, 10.5);

        // Sleep for 1.5 seconds
        sleep(1500);

        // Send 2 more events, E3 and E4 at +1.5sec
        env.sendEventBean(makeBean(SYMBOL, 10, 1000));
        checkValue(env, 10.25);
        env.sendEventBean(makeBean(SYMBOL, 10.5, 2000));
        checkValue(env, 10.375);

        // Sleep for 2 seconds, E1 and E2 should have left the window
        sleep(2000);
        checkValue(env, 10.333333333);

        // Send another event, E5 at +3.5sec
        env.sendEventBean(makeBean(SYMBOL, 10.2, 1000));
        checkValue(env, 10.3);

        // Sleep for 2.5 seconds, E3 and E4 should expire
        sleep(2500);
        checkValue(env, 10.2);

        // Sleep for 1 seconds, E5 should have expired
        sleep(1000);
        checkValue(env, Double.NaN);

        env.undeployAll();
    }

    private static SupportMarketDataBean makeBean(String symbol, double price, long volume) {
        return new SupportMarketDataBean(symbol, price, volume, FEED);
    }

    private void checkValue(RegressionEnvironment env, double avgE) {
        Iterator<EventBean> iterator = env.statement("s0").iterator();
        checkValue(iterator.next(), avgE);
        assertTrue(!iterator.hasNext());

        assertTrue(env.listener("s0").getLastNewData().length == 1);
        EventBean listenerValues = env.listener("s0").getLastNewData()[0];
        checkValue(listenerValues, avgE);

        env.listener("s0").reset();
    }

    private void checkValue(EventBean values, double avgE) {
        double avg = getDoubleValue(ViewFieldEnum.WEIGHTED_AVERAGE__AVERAGE, values);
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
        Assert.assertEquals(FEED, values.get("feed"));
        Assert.assertEquals(SYMBOL, values.get("symbol"));
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
