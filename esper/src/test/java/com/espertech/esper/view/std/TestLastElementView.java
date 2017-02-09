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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.event.EventFactoryHelper;
import com.espertech.esper.supportunit.view.SupportBeanClassView;
import com.espertech.esper.supportunit.view.SupportStreamImpl;
import com.espertech.esper.supportunit.view.SupportViewDataChecker;
import junit.framework.TestCase;

import java.util.Map;

public class TestLastElementView extends TestCase {
    private LastElementView myView;
    private SupportBeanClassView childView;

    public void setUp() {
        // Set up length window view and a test child view
        myView = new LastElementView(null);
        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    public void testViewPush() {
        // Set up a feed for the view under test - it will have a depth of 3 trades
        SupportStreamImpl stream = new SupportStreamImpl(SupportBean_A.class, 3);
        stream.addView(myView);

        Map<String, EventBean> events = EventFactoryHelper.makeEventMap(
                new String[]{"a0", "a1", "b0", "c0", "c1", "c2", "d0", "e0"});

        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, null);
        EPAssertionUtil.assertEqualsExactOrder(null, myView.iterator());

        // View should keep the last element for iteration, should report new data as it arrives
        stream.insert(new EventBean[]{events.get("a0"), events.get("a1")});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{events.get("a0")});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{events.get("a0"), events.get("a1")});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a1")}, myView.iterator());

        stream.insert(new EventBean[]{events.get("b0")});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{events.get("a1")});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{events.get("b0")});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("b0")}, myView.iterator());

        stream.insert(new EventBean[]{events.get("c0"), events.get("c1"), events.get("c2")});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{events.get("b0"), events.get("c0"), events.get("c1")});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{events.get("c0"), events.get("c1"), events.get("c2")});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("c2")}, myView.iterator());

        stream.insert(new EventBean[]{events.get("d0")});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{events.get("c2")});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{events.get("d0")});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("d0")}, myView.iterator());

        stream.insert(new EventBean[]{events.get("e0")});
        SupportViewDataChecker.checkOldData(childView, new EventBean[]{events.get("d0")});
        SupportViewDataChecker.checkNewData(childView, new EventBean[]{events.get("e0")});
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("e0")}, myView.iterator());
    }
}