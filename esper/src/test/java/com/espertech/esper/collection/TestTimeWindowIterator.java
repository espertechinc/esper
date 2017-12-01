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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportunit.event.EventFactoryHelper;
import com.espertech.esper.view.internal.TimeWindowIterator;
import com.espertech.esper.view.internal.TimeWindowPair;
import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;

public class TestTimeWindowIterator extends TestCase {
    private Map<String, EventBean> events;

    public void setUp() {
        events = EventFactoryHelper.makeEventMap(new String[]{"a", "b", "c", "d", "e", "f", "g"});
    }

    public void testEmpty() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        Iterator<EventBean> it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(null, it);
    }

    public void testOneElement() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        ArrayDeque<EventBean> list = new ArrayDeque<EventBean>();
        list.add(events.get("a"));
        addToWindow(testWindow, 10L, list);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("a")}, it);
    }

    public void testTwoInOneEntryElement() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        ArrayDeque<EventBean> list = new ArrayDeque<EventBean>();
        list.add(events.get("a"));
        list.add(events.get("b"));
        addToWindow(testWindow, 10L, list);

        Iterator<EventBean> it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a"), events.get("b")}, it);
    }

    public void testTwoSeparateEntryElement() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        list2.add(events.get("b"));
        addToWindow(testWindow, 5L, list2); // Actually before list1
        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        list1.add(events.get("a"));
        addToWindow(testWindow, 10L, list1);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("b"), events.get("a")}, it);
    }

    public void testTwoByTwoEntryElement() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        list1.add(events.get("a"));
        list1.add(events.get("b"));
        addToWindow(testWindow, 10L, list1);
        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        list2.add(events.get("c"));
        list2.add(events.get("d"));
        addToWindow(testWindow, 15L, list2);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("a"), events.get("b"), events.get("c"), events.get("d")}, it);
    }

    public void testMixedEntryElement() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();
        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        list1.add(events.get("a"));
        addToWindow(testWindow, 10L, list1);
        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        list2.add(events.get("c"));
        list2.add(events.get("d"));
        addToWindow(testWindow, 15L, list2);
        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        list3.add(events.get("e"));
        list3.add(events.get("f"));
        list3.add(events.get("g"));
        addToWindow(testWindow, 20L, list3);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("a"), events.get("c"), events.get("d"),
                events.get("e"), events.get("f"), events.get("g")}, it);
    }

    public void testEmptyList() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 10L, list1);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder((Object[]) null, it);
    }

    public void testTwoEmptyList() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 10L, list1);
        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 20L, list2);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder((Object[]) null, it);
    }

    public void testThreeEmptyList() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 10L, list1);
        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 20L, list2);
        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 30L, list3);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder((Object[]) null, it);
    }

    public void testEmptyListFrontTail() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 10L, list1);

        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        list2.add(events.get("c"));
        list2.add(events.get("d"));
        addToWindow(testWindow, 15L, list2);

        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 20L, list3);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("c"), events.get("d")}, it);
    }

    public void testEmptyListSprinkle() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        list1.add(events.get("a"));
        addToWindow(testWindow, 10L, list1);

        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 15L, list2);

        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        list3.add(events.get("c"));
        list3.add(events.get("d"));
        addToWindow(testWindow, 20L, list3);

        ArrayDeque<EventBean> list4 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 40L, list4);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("a"), events.get("c"), events.get("d")}, it);
    }

    public void testEmptyListFront() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        addToWindow(testWindow, 10L, list1);

        ArrayDeque<EventBean> list2 = new ArrayDeque<EventBean>();
        list2.add(events.get("a"));
        addToWindow(testWindow, 15L, list2);

        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        list3.add(events.get("c"));
        list3.add(events.get("d"));
        addToWindow(testWindow, 20L, list3);

        ArrayDeque<EventBean> list4 = new ArrayDeque<EventBean>();
        list4.add(events.get("e"));
        addToWindow(testWindow, 40L, list4);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("a"), events.get("c"), events.get("d"), events.get("e")}, it);
    }

    public void testObjectAndNull() {
        ArrayDeque<TimeWindowPair> testWindow = new ArrayDeque<TimeWindowPair>();

        ArrayDeque<EventBean> list1 = new ArrayDeque<EventBean>();
        list1.add(events.get("c"));
        list1.add(events.get("d"));
        addToWindow(testWindow, 10L, list1);

        addToWindow(testWindow, 20L, events.get("a"));

        addToWindow(testWindow, 30L, null);

        ArrayDeque<EventBean> list3 = new ArrayDeque<EventBean>();
        list3.add(events.get("e"));
        addToWindow(testWindow, 40L, list3);

        Iterator it = new TimeWindowIterator(testWindow);
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{events.get("c"), events.get("d"), events.get("a"), events.get("e")}, it);
    }

    private void addToWindow(ArrayDeque<TimeWindowPair> testWindow,
                             long key,
                             Object value) {
        testWindow.add(new TimeWindowPair(key, value));
    }
}
