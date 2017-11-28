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
package com.espertech.esper.view.stat;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.util.DoubleValueAssertionUtil;
import com.espertech.esper.supportunit.view.SupportBeanClassView;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.view.ViewFieldEnum;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestUnivariateStatisticsView extends TestCase {
    UnivariateStatisticsView myView;
    SupportBeanClassView childView;

    public void setUp() throws Exception {
        // Set up sum view and a test child view
        EventType type = UnivariateStatisticsView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        UnivariateStatisticsViewFactory factory = new UnivariateStatisticsViewFactory();
        factory.setEventType(type);
        factory.setFieldExpression(SupportExprNodeFactory.makeIdentNodeMD("price"));
        factory.fieldExpressionEvaluator = factory.fieldExpression.getForge().getExprEvaluator();
        myView = new UnivariateStatisticsView(factory, SupportStatementContextFactory.makeAgentInstanceViewFactoryContext());

        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    // Check values against Microsoft Excel computed values
    public void testViewComputedValues() {
        // Set up feed for sum view
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 3);
        stream.addView(myView);

        // Send two events to the stream
        assertTrue(childView.getLastNewData() == null);

        // Send a first event, checkNew values
        EventBean marketData = makeBean("IBM", 10, 0);
        stream.insert(marketData);
        checkOld(0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        checkNew(1, 10, 10, 0, Double.NaN, Double.NaN);

        // Send a second event, checkNew values
        marketData = makeBean("IBM", 12, 0);
        stream.insert(marketData);
        checkOld(1, 10, 10, 0, Double.NaN, Double.NaN);
        checkNew(2, 22, 11, 1, Math.sqrt(2.0), 2);

        // Send a third event, checkNew values
        marketData = makeBean("IBM", 9.5, 0);
        stream.insert(marketData);
        checkOld(2, 22, 11, 1, Math.sqrt(2.0), 2);
        checkNew(3, 31.5, 10.5, 1.08012345, 1.322875656, 1.75);

        // Send a 4th event, this time the first event should be gone, checkNew values
        marketData = makeBean("IBM", 9, 0);
        stream.insert(marketData);
        checkOld(3, 31.5, 10.5, 1.08012345, 1.322875656, 1.75);
        checkNew(3, 30.5, 10.16666667, 1.312334646, 1.607275127, 2.583333333);
    }

    public void testGetSchema() {
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS.getName()) == Long.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE.getName()) == Double.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV.getName()) == Double.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA.getName()) == Double.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE.getName()) == Double.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL.getName()) == Double.class);
    }

    private void checkNew(long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        Iterator<EventBean> iterator = myView.iterator();
        checkValues(iterator.next(), countE, sumE, avgE, stdevpaE, stdevE, varianceE);
        assertTrue(iterator.hasNext() == false);

        assertTrue(childView.getLastNewData().length == 1);
        EventBean childViewValues = childView.getLastNewData()[0];
        checkValues(childViewValues, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
    }

    private void checkOld(long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        assertTrue(childView.getLastOldData().length == 1);
        EventBean childViewValues = childView.getLastOldData()[0];
        checkValues(childViewValues, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
    }

    private void checkValues(EventBean values, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        long count = getLongValue(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS, values);
        double sum = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL, values);
        double avg = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE, values);
        double stdevpa = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA, values);
        double stdev = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV, values);
        double variance = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE, values);

        assertEquals(count, countE);
        assertEquals(sum, sumE);
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdevpa, stdevpaE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdev, stdevE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(variance, varianceE, 6));
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean eventBean) {
        return (Double) eventBean.get(field.getName());
    }

    private long getLongValue(ViewFieldEnum field, EventBean eventBean) {
        return (Long) eventBean.get(field.getName());
    }

    private EventBean makeBean(String symbol, double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, "");
        return SupportEventBeanFactory.createObject(bean);
    }
}
