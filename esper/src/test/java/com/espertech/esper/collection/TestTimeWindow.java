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
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.view.internal.TimeWindow;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;

public class TestTimeWindow extends TestCase {
    private final TimeWindow window = new TimeWindow(false);
    private final TimeWindow windowRemovable = new TimeWindow(true);
    private final EventBean[] beans = new EventBean[6];

    public void setUp() {
        for (int i = 0; i < beans.length; i++) {
            beans[i] = createBean();
        }
    }

    public void testAdd() {
        assertTrue(window.getOldestTimestamp() == null);
        assertTrue(window.isEmpty());

        window.add(19, beans[0]);
        assertTrue(window.getOldestTimestamp() == 19L);
        assertFalse(window.isEmpty());
        window.add(19, beans[1]);
        assertTrue(window.getOldestTimestamp() == 19L);
        window.add(20, beans[2]);
        assertTrue(window.getOldestTimestamp() == 19L);
        window.add(20, beans[3]);
        window.add(21, beans[4]);
        window.add(22, beans[5]);
        assertTrue(window.getOldestTimestamp() == 19L);

        ArrayDeque<EventBean> beanList = window.expireEvents(19);
        assertTrue(beanList == null);

        beanList = window.expireEvents(20);
        assertTrue(beanList.size() == 2);
        assertTrue(beanList.poll() == beans[0]);
        assertTrue(beanList.poll() == beans[1]);

        beanList = window.expireEvents(21);
        assertTrue(beanList.size() == 2);
        assertTrue(beanList.poll() == beans[2]);
        assertTrue(beanList.poll() == beans[3]);
        assertFalse(window.isEmpty());
        assertTrue(window.getOldestTimestamp() == 21);

        beanList = window.expireEvents(22);
        assertTrue(beanList.size() == 1);
        assertTrue(beanList.poll() == beans[4]);
        assertFalse(window.isEmpty());
        assertTrue(window.getOldestTimestamp() == 22);

        beanList = window.expireEvents(23);
        assertTrue(beanList.size() == 1);
        assertTrue(beanList.poll() == beans[5]);
        assertTrue(window.isEmpty());
        assertTrue(window.getOldestTimestamp() == null);

        beanList = window.expireEvents(23);
        assertTrue(beanList == null);
        assertTrue(window.isEmpty());
        assertTrue(window.getOldestTimestamp() == null);
    }

    public void testAddRemove() {
        assertTrue(windowRemovable.getOldestTimestamp() == null);
        assertTrue(windowRemovable.isEmpty());

        windowRemovable.add(19, beans[0]);
        assertTrue(windowRemovable.getOldestTimestamp() == 19L);
        assertFalse(windowRemovable.isEmpty());
        windowRemovable.add(19, beans[1]);
        assertTrue(windowRemovable.getOldestTimestamp() == 19L);
        windowRemovable.add(20, beans[2]);
        assertTrue(windowRemovable.getOldestTimestamp() == 19L);
        windowRemovable.add(20, beans[3]);
        windowRemovable.add(21, beans[4]);
        windowRemovable.add(22, beans[5]);
        assertTrue(windowRemovable.getOldestTimestamp() == 19L);

        windowRemovable.remove(beans[4]);
        windowRemovable.remove(beans[0]);
        windowRemovable.remove(beans[3]);

        ArrayDeque<EventBean> beanList = windowRemovable.expireEvents(19);
        assertTrue(beanList == null);

        beanList = windowRemovable.expireEvents(20);
        assertTrue(beanList.size() == 1);
        assertTrue(beanList.getFirst() == beans[1]);

        beanList = windowRemovable.expireEvents(21);
        assertTrue(beanList.size() == 1);
        assertTrue(beanList.getFirst() == beans[2]);
        assertFalse(windowRemovable.isEmpty());
        assertTrue(windowRemovable.getOldestTimestamp() == 22);

        beanList = windowRemovable.expireEvents(22);
        assertTrue(beanList.size() == 0);

        beanList = windowRemovable.expireEvents(23);
        assertTrue(beanList.size() == 1);
        assertTrue(beanList.getFirst() == beans[5]);
        assertTrue(windowRemovable.isEmpty());
        assertTrue(windowRemovable.getOldestTimestamp() == null);

        beanList = windowRemovable.expireEvents(23);
        assertTrue(beanList == null);
        assertTrue(windowRemovable.isEmpty());
        assertTrue(windowRemovable.getOldestTimestamp() == null);

        assertEquals(0, windowRemovable.getReverseIndex().size());
    }

    public void testTimeWindowPerformance() {
        log.info(".testTimeWindowPerformance Starting");

        TimeWindow window = new TimeWindow(false);

        // 1E7 yields for implementations...on 2.8GHz JDK 1.5
        // about 7.5 seconds for a LinkedList-backed
        // about 20 seconds for a LinkedHashMap-backed
        // about 30 seconds for a TreeMap-backed-backed
        for (int i = 0; i < 10; i++) {
            window.add(i, SupportEventBeanFactory.createObject("a"));

            window.expireEvents(i - 100);
        }

        log.info(".testTimeWindowPerformance Done");
    }

    private EventBean createBean() {
        return SupportEventBeanFactory.createObject(new SupportBean());
    }

    private static final Logger log = LoggerFactory.getLogger(TestTimeWindow.class);
}
