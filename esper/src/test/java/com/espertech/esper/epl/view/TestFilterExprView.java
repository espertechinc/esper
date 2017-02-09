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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportunit.epl.SupportExprEvaluator;
import com.espertech.esper.supportunit.epl.SupportExprNode;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.view.SupportMapView;
import junit.framework.TestCase;

public class TestFilterExprView extends TestCase {
    private FilterExprView filterExprViewAdapter;
    private SupportMapView childView;

    public void setUp() {
        filterExprViewAdapter = new FilterExprView(new SupportExprNode(null), new SupportExprEvaluator(), null);
        childView = new SupportMapView();
        filterExprViewAdapter.addView(childView);
    }

    public void testUpdate() {
        // Test all evaluate to true (ie. all pass the filter)
        EventBean[] oldEvents = SupportEventBeanFactory.makeEvents(new boolean[]{true, true});
        EventBean[] newEvents = SupportEventBeanFactory.makeEvents(new boolean[]{true, true});
        filterExprViewAdapter.update(newEvents, oldEvents);

        assertEquals(newEvents, childView.getLastNewData());
        assertEquals(oldEvents, childView.getLastOldData());
        childView.reset();

        // Test all evaluate to false (ie. none pass the filter)
        oldEvents = SupportEventBeanFactory.makeEvents(new boolean[]{false, false});
        newEvents = SupportEventBeanFactory.makeEvents(new boolean[]{false, false});
        filterExprViewAdapter.update(newEvents, oldEvents);

        assertFalse(childView.getAndClearIsInvoked());  // Should not be invoked if no events
        assertNull(childView.getLastNewData());
        assertNull(childView.getLastOldData());

        // Test some pass through the filter
        oldEvents = SupportEventBeanFactory.makeEvents(new boolean[]{false, true, false});
        newEvents = SupportEventBeanFactory.makeEvents(new boolean[]{true, false, true});
        filterExprViewAdapter.update(newEvents, oldEvents);

        assertEquals(2, childView.getLastNewData().length);
        assertSame(newEvents[0], childView.getLastNewData()[0]);
        assertSame(newEvents[2], childView.getLastNewData()[1]);
        assertEquals(1, childView.getLastOldData().length);
        assertSame(oldEvents[1], childView.getLastOldData()[0]);
    }
}
