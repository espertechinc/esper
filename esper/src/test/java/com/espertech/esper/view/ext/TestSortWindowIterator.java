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
package com.espertech.esper.view.ext;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.collection.HashableMultiKey;
import com.espertech.esper.supportunit.event.EventFactoryHelper;
import com.espertech.esper.util.HashableMultiKeyCastingComparator;
import com.espertech.esper.util.HashableMultiKeyComparator;
import junit.framework.TestCase;

import java.util.*;

public class TestSortWindowIterator extends TestCase {
    private Map<String, EventBean> events;
    private SortedMap<Object, Object> testMap;
    private Comparator<Object> comparator;

    public void setUp() {
        events = EventFactoryHelper.makeEventMap(new String[]{"a", "b", "c", "d", "f", "g"});
        comparator = new HashableMultiKeyCastingComparator(new HashableMultiKeyComparator(new boolean[]{false}));
        testMap = new TreeMap<Object, Object>(comparator);
    }

    public void testEmpty() {
        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(null, it);
    }

    public void testOneElement() {
        LinkedList<EventBean> list = new LinkedList<EventBean>();
        list.add(events.get("a"));
        HashableMultiKey key = new HashableMultiKey(new Object[]{"akey"});
        testMap.put(key, list);

        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a")}, it);
    }

    public void testTwoInOneEntryElement() {
        LinkedList<EventBean> list = new LinkedList<EventBean>();
        list.add(events.get("a"));
        list.add(events.get("b"));
        HashableMultiKey key = new HashableMultiKey(new Object[]{"keyA"});
        testMap.put(key, list);

        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a"), events.get("b")}, it);
    }

    public void testTwoSeparateEntryElement() {
        LinkedList<EventBean> list1 = new LinkedList<EventBean>();
        list1.add(events.get("a"));
        HashableMultiKey keyB = new HashableMultiKey(new Object[]{"keyB"});
        testMap.put(keyB, list1);
        LinkedList<EventBean> list2 = new LinkedList<EventBean>();
        list2.add(events.get("b"));
        HashableMultiKey keyA = new HashableMultiKey(new Object[]{"keyA"});
        testMap.put(keyA, list2); // Actually before list1

        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("b"), events.get("a")}, it);
    }

    public void testTwoByTwoEntryElement() {
        LinkedList<EventBean> list1 = new LinkedList<EventBean>();
        list1.add(events.get("a"));
        list1.add(events.get("b"));
        HashableMultiKey keyB = new HashableMultiKey(new Object[]{"keyB"});
        testMap.put(keyB, list1);
        LinkedList<EventBean> list2 = new LinkedList<EventBean>();
        list2.add(events.get("c"));
        list2.add(events.get("d"));
        HashableMultiKey keyC = new HashableMultiKey(new Object[]{"keyC"});
        testMap.put(keyC, list2);

        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a"), events.get("b"), events.get("c"), events.get("d")}, it);
    }

    public void testMixedEntryElement() {
        LinkedList<EventBean> list1 = new LinkedList<EventBean>();
        list1.add(events.get("a"));
        HashableMultiKey keyA = new HashableMultiKey(new Object[]{"keyA"});
        testMap.put(keyA, list1);
        LinkedList<EventBean> list2 = new LinkedList<EventBean>();
        list2.add(events.get("c"));
        list2.add(events.get("d"));
        HashableMultiKey keyB = new HashableMultiKey(new Object[]{"keyB"});
        testMap.put(keyB, list2);
        LinkedList<EventBean> list3 = new LinkedList<EventBean>();
        list3.add(events.get("e"));
        list3.add(events.get("f"));
        list3.add(events.get("g"));
        HashableMultiKey keyC = new HashableMultiKey(new Object[]{"keyC"});
        testMap.put(keyC, list3);

        Iterator<EventBean> it = new SortWindowIterator(testMap);
        EPAssertionUtil.assertEqualsExactOrder(new EventBean[]{events.get("a"), events.get("c"), events.get("d"),
                events.get("e"), events.get("f"), events.get("g")}, it);
    }
}


