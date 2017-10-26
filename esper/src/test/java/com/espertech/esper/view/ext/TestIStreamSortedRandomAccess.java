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
import com.espertech.esper.collection.HashableMultiKey;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.util.HashableMultiKeyCastingComparator;
import com.espertech.esper.util.HashableMultiKeyComparator;
import com.espertech.esper.view.window.RandomAccessByIndex;
import com.espertech.esper.view.window.RandomAccessByIndexObserver;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class TestIStreamSortedRandomAccess extends TestCase {
    private IStreamSortRankRandomAccess access;
    private TreeMap<Object, Object> sortedEvents;
    private EventBean[] events;

    public void setUp() {
        RandomAccessByIndexObserver updateObserver = new RandomAccessByIndexObserver() {
            public void updated(RandomAccessByIndex randomAccessByIndex) {
            }
        };
        access = new IStreamSortRankRandomAccessImpl(updateObserver);
        sortedEvents = new TreeMap<Object, Object>(new HashableMultiKeyCastingComparator(new HashableMultiKeyComparator(new boolean[]{false})));

        events = new EventBean[100];
        for (int i = 0; i < events.length; i++) {
            events[i] = SupportEventBeanFactory.createObject(new SupportBean());
        }
    }

    public void testGet() {
        access.refresh(sortedEvents, 0, 10);
        assertNull(access.getNewData(0));
        assertNull(access.getNewData(1));

        add("C", events[0]);
        access.refresh(sortedEvents, 1, 10);
        assertData(new EventBean[]{events[0]});

        add("E", events[1]);
        access.refresh(sortedEvents, 2, 10);
        assertData(new EventBean[]{events[0], events[1]});

        add("A", events[2]);
        access.refresh(sortedEvents, 3, 10);
        assertData(new EventBean[]{events[2], events[0], events[1]});

        add("C", events[4]);
        access.refresh(sortedEvents, 4, 10);
        assertData(new EventBean[]{events[2], events[4], events[0], events[1]});

        add("E", events[5]);
        access.refresh(sortedEvents, 5, 10);
        assertData(new EventBean[]{events[2], events[4], events[0], events[5], events[1]});

        add("A", events[6]);
        access.refresh(sortedEvents, 6, 10);
        assertData(new EventBean[]{events[6], events[2], events[4], events[0], events[5], events[1]});

        add("B", events[7]);
        access.refresh(sortedEvents, 7, 10);
        assertData(new EventBean[]{events[6], events[2], events[7], events[4], events[0], events[5], events[1]});

        add("F", events[8]);
        access.refresh(sortedEvents, 8, 10);
        assertData(new EventBean[]{events[6], events[2], events[7], events[4], events[0], events[5], events[1], events[8]});
        //                          A           A           B       C           C           E           E           F

        add("D", events[9]);
        access.refresh(sortedEvents, 9, 10);
        assertSame(events[9], access.getNewData(5));
    }

    private void assertData(EventBean[] events) {
        for (int i = 0; i < events.length; i++) {
            assertSame("Failed for index " + i, events[i], access.getNewData(i));
        }
        assertNull(access.getNewData(events.length));
    }

    private void add(String key, EventBean theEvent) {
        ((SupportBean) theEvent.getUnderlying()).setTheString(key);
        HashableMultiKey mkey = new HashableMultiKey(new Object[]{key});
        List<EventBean> eventList = (List<EventBean>) sortedEvents.get(mkey);
        if (eventList == null) {
            eventList = new LinkedList<EventBean>();
        }
        eventList.add(0, theEvent);
        sortedEvents.put(mkey, eventList);
    }
}
