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
package com.espertech.esper.view.window;

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

public class TestLengthWindowView extends TestCase {
    private LengthWindowView myView;
    private SupportBeanClassView childView;

    public void setUp() {
        // Set up length window view and a test child view
        myView = new LengthWindowView(null, new LengthWindowViewFactory(), 5, null);
        childView = new SupportBeanClassView(SupportMarketDataBean.class);
        myView.addView(childView);
    }

    public void testIncorrectUse() {
        try {
            myView = new LengthWindowView(null, null, 0, null);
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }
    }

    // Check values against Microsoft Excel computed values
    public void testViewPush() {
        // Set up a feed for the view under test - it will have a depth of 3 trades
        SupportStreamImpl stream = new SupportStreamImpl(SupportBean_A.class, 3);
        stream.addView(myView);

        Map<String, EventBean> events = EventFactoryHelper.makeEventMap(
                new String[]{"a0", "b0", "b1", "c0", "c1", "d0", "e0", "e1", "e2", "f0", "f1",
                        "g0", "g1", "g2", "g3", "g4",
                        "h0", "h1", "h2", "h3", "h4", "h5", "h6",
                        "i0"});

        // Fill the window with events up to the depth of 5
        stream.insert(makeArray(events, new String[]{"a0"}));
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"a0"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"a0"}), myView.iterator());

        stream.insert(makeArray(events, new String[]{"b0", "b1"}));
        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"b0", "b1"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"a0", "b0", "b1"}), myView.iterator());

        stream.insert(makeArray(events, new String[]{"c0", "c1"}));

        SupportViewDataChecker.checkOldData(childView, null);
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"c0", "c1"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"a0", "b0", "b1", "c0", "c1"}), myView.iterator());

        // Send further events, expect to get events back that fall out of the window (a0)
        stream.insert(makeArray(events, new String[]{"d0"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"a0"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"d0"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"b0", "b1", "c0", "c1", "d0"}), myView.iterator());

        stream.insert(makeArray(events, new String[]{"e0", "e1", "e2"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"b0", "b1", "c0"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"e0", "e1", "e2"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"c1", "d0", "e0", "e1", "e2"}), myView.iterator());

        stream.insert(makeArray(events, new String[]{"f0", "f1"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"c1", "d0"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"f0", "f1"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"e0", "e1", "e2", "f0", "f1"}), myView.iterator());

        // Push as many events as the window takes
        stream.insert(makeArray(events, new String[]{"g0", "g1", "g2", "g3", "g4"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"e0", "e1", "e2", "f0", "f1"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"g0", "g1", "g2", "g3", "g4"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"g0", "g1", "g2", "g3", "g4"}), myView.iterator());

        // Push 2 more events then the window takes
        stream.insert(makeArray(events, new String[]{"h0", "h1", "h2", "h3", "h4", "h5", "h6"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"g0", "g1", "g2", "g3", "g4", "h0", "h1"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"h0", "h1", "h2", "h3", "h4", "h5", "h6"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"h2", "h3", "h4", "h5", "h6"}), myView.iterator());

        // Push 1 last event to make sure the last overflow was handled correctly
        stream.insert(makeArray(events, new String[]{"i0"}));
        SupportViewDataChecker.checkOldData(childView, makeArray(events, new String[]{"h2"}));
        SupportViewDataChecker.checkNewData(childView, makeArray(events, new String[]{"i0"}));
        EPAssertionUtil.assertEqualsExactOrder(makeArray(events, new String[]{"h3", "h4", "h5", "h6", "i0"}), myView.iterator());
    }

    private EventBean[] makeArray(Map<String, EventBean> events, String[] ids) {
        return EventFactoryHelper.makeArray(events, ids);
    }
}