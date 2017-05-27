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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.DoubleValueAssertionUtil;
import com.espertech.esper.view.ViewFieldEnum;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ExecViewLengthWindowStats implements RegressionExecution {
    private final static String SYMBOL = "CSCO.O";
    private final static String FEED = "feed1";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIterator(epService);
        runAssertionWindowStats(epService);
    }

    private void runAssertionIterator(EPServiceProvider epService) {
        String epl = "select symbol, price from " + SupportMarketDataBean.class.getName() + "#length(2)";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendEvent(epService, "ABC", 20);
        sendEvent(epService, "DEF", 100);

        // check iterator results
        Iterator<EventBean> events = statement.iterator();
        EventBean theEvent = events.next();
        assertEquals("ABC", theEvent.get("symbol"));
        assertEquals(20d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));
        assertFalse(events.hasNext());

        sendEvent(epService, "EFG", 50);

        // check iterator results
        events = statement.iterator();
        theEvent = events.next();
        assertEquals("DEF", theEvent.get("symbol"));
        assertEquals(100d, theEvent.get("price"));

        theEvent = events.next();
        assertEquals("EFG", theEvent.get("symbol"));
        assertEquals(50d, theEvent.get("price"));
    }

    private void runAssertionWindowStats(EPServiceProvider epService) {
        String epl = "select irstream * from " + SupportMarketDataBean.class.getName() +
                "(symbol='" + SYMBOL + "')#length(3)#uni(price, symbol, feed)";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        listener.reset();

        assertEquals(Double.class, statement.getEventType().getPropertyType("average"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("variance"));
        assertEquals(Long.class, statement.getEventType().getPropertyType("datapoints"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("total"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("stddev"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("stddevpa"));

        sendEvent(epService, SYMBOL, 100);
        checkOld(listener, true, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        checkNew(statement, 1, 100, 100, 0, Double.NaN, Double.NaN, listener);

        sendEvent(epService, SYMBOL, 100.5);
        checkOld(listener, false, 1, 100, 100, 0, Double.NaN, Double.NaN);
        checkNew(statement, 2, 200.5, 100.25, 0.25, 0.353553391, 0.125, listener);

        sendEvent(epService, "DUMMY", 100.5);
        assertTrue(listener.getLastNewData() == null);
        assertTrue(listener.getLastOldData() == null);

        sendEvent(epService, SYMBOL, 100.7);
        checkOld(listener, false, 2, 200.5, 100.25, 0.25, 0.353553391, 0.125);
        checkNew(statement, 3, 301.2, 100.4, 0.294392029, 0.360555128, 0.13, listener);

        sendEvent(epService, SYMBOL, 100.6);
        checkOld(listener, false, 3, 301.2, 100.4, 0.294392029, 0.360555128, 0.13);
        checkNew(statement, 3, 301.8, 100.6, 0.081649658, 0.1, 0.01, listener);

        sendEvent(epService, SYMBOL, 100.9);
        checkOld(listener, false, 3, 301.8, 100.6, 0.081649658, 0.1, 0.01);
        checkNew(statement, 3, 302.2, 100.733333333, 0.124721913, 0.152752523, 0.023333333, listener);
        statement.destroy();

        // Test copying all properties
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String eplWildcard = "select * from SupportBean#length(3)#uni(intPrimitive, *)";
        statement = epService.getEPAdministrator().createEPL(eplWildcard);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(1.0, theEvent.get("average"));
        assertEquals("E1", theEvent.get("theString"));
        assertEquals(1, theEvent.get("intPrimitive"));
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, FEED);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void checkNew(EPStatement statement, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE, SupportUpdateListener listener) {
        Iterator<EventBean> iterator = statement.iterator();
        checkValues(iterator.next(), false, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
        assertFalse(iterator.hasNext());

        assertTrue(listener.getLastNewData().length == 1);
        EventBean childViewValues = listener.getLastNewData()[0];
        checkValues(childViewValues, false, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);

        listener.reset();
    }

    private void checkOld(SupportUpdateListener listener, boolean isFirst, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        assertTrue(listener.getLastOldData().length == 1);
        EventBean childViewValues = listener.getLastOldData()[0];
        checkValues(childViewValues, isFirst, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
    }

    private void checkValues(EventBean values, boolean isFirst, boolean isNewData, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        long count = getLongValue(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS, values);
        double sum = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL, values);
        double avg = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE, values);
        double stdevpa = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA, values);
        double stdev = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV, values);
        double variance = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE, values);

        assertEquals(count, countE);
        assertTrue(DoubleValueAssertionUtil.equals(sum, sumE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdevpa, stdevpaE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdev, stdevE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(variance, varianceE, 6));
        if (isFirst && !isNewData) {
            assertEquals(null, values.get("symbol"));
            assertEquals(null, values.get("feed"));
        } else {
            assertEquals(SYMBOL, values.get("symbol"));
            assertEquals(FEED, values.get("feed"));
        }
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean values) {
        return (Double) values.get(field.getName());
    }

    private long getLongValue(ViewFieldEnum field, EventBean values) {
        return (Long) values.get(field.getName());
    }
}
