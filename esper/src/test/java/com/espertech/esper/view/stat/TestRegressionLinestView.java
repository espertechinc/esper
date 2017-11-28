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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.epl.SupportExprNodeFactory;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.util.DoubleValueAssertionUtil;
import com.espertech.esper.supportunit.view.SupportBeanClassView;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.view.ViewFieldEnum;
import junit.framework.TestCase;

import java.util.Iterator;

public class TestRegressionLinestView extends TestCase {
    RegressionLinestView myView;
    SupportBeanClassView childView;

    public void setUp() throws Exception {
        // Set up sum view and a test child view
        EventType type = RegressionLinestView.createEventType(SupportStatementContextFactory.makeContext(), null, 1);
        RegressionLinestViewFactory viewFactory = new RegressionLinestViewFactory();
        ExprNode x = SupportExprNodeFactory.makeIdentNodeMD("price");
        ExprEvaluator xEval = x.getForge().getExprEvaluator();
        ExprNode y = SupportExprNodeFactory.makeIdentNodeMD("volume");
        ExprEvaluator yEval = y.getForge().getExprEvaluator();
        myView = new RegressionLinestView(viewFactory, SupportStatementContextFactory.makeAgentInstanceContext(), x, xEval, y, yEval, type, null);

        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    // Check values against Microsoft Excel computed values
    public void testViewComputedValues() {
        // Set up feed for sum view
        SupportStreamImpl stream = new SupportStreamImpl(SupportMarketDataBean.class, 3);
        stream.addView(myView);

        // Send a first event, checkNew values
        EventBean marketData = makeBean("IBM", 70, 1000);
        stream.insert(marketData);
        checkOld(Double.NaN, Double.NaN);
        checkNew(Double.NaN, Double.NaN);

        // Send a second event, checkNew values
        marketData = makeBean("IBM", 70.5, 1500);
        stream.insert(marketData);
        checkOld(Double.NaN, Double.NaN);
        checkNew(1000, -69000);

        // Send a third event, checkNew values
        marketData = makeBean("IBM", 70.1, 1200);
        stream.insert(marketData);
        checkOld(1000, -69000);
        checkNew(928.5714286, -63952.380953);

        // Send a 4th event, this time the first event should be gone, checkNew values
        marketData = makeBean("IBM", 70.25, 1000);
        stream.insert(marketData);
        checkOld(928.5714286, -63952.380953);
        checkNew(877.5510204, -60443.877555);
    }

    public void testGetSchema() {
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.REGRESSION__SLOPE.getName()) == Double.class);
        assertTrue(myView.getEventType().getPropertyType(ViewFieldEnum.REGRESSION__YINTERCEPT.getName()) == Double.class);
    }

    private void checkNew(double slopeE, double yinterceptE) {
        Iterator<EventBean> iterator = myView.iterator();
        checkValues(iterator.next(), slopeE, yinterceptE);
        assertTrue(iterator.hasNext() == false);

        assertTrue(childView.getLastNewData().length == 1);
        EventBean childViewValues = childView.getLastNewData()[0];
        checkValues(childViewValues, slopeE, yinterceptE);
    }

    private void checkOld(double slopeE, double yinterceptE) {
        assertTrue(childView.getLastOldData().length == 1);
        EventBean childViewValues = childView.getLastOldData()[0];
        checkValues(childViewValues, slopeE, yinterceptE);
    }

    private void checkValues(EventBean eventBean, double slopeE, double yinterceptE) {
        double slope = getDoubleValue(ViewFieldEnum.REGRESSION__SLOPE, eventBean);
        double yintercept = getDoubleValue(ViewFieldEnum.REGRESSION__YINTERCEPT, eventBean);
        assertTrue(DoubleValueAssertionUtil.equals(slope, slopeE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(yintercept, yinterceptE, 6));
    }

    private double getDoubleValue(ViewFieldEnum field, EventBean theEvent) {
        return (Double) theEvent.get(field.getName());
    }

    private EventBean makeBean(String symbol, double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, "");
        return SupportEventBeanFactory.createObject(bean);
    }
}
