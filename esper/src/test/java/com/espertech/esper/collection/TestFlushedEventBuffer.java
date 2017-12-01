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
import com.espertech.esper.event.FlushedEventBuffer;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

public class TestFlushedEventBuffer extends TestCase {
    private FlushedEventBuffer buffer;
    private EventBean[] events;

    public void setUp() {
        buffer = new FlushedEventBuffer();
        events = new EventBean[10];

        for (int i = 0; i < events.length; i++) {
            events[i] = SupportEventBeanFactory.createObject(i);
        }
    }

    public void testFlow() {
        // test empty buffer
        buffer.add(null);
        assertNull(buffer.getAndFlush());
        buffer.flush();

        // test add single events
        buffer.add(new EventBean[]{events[0]});
        EventBean[] results = buffer.getAndFlush();
        assertTrue((results.length == 1) && (results[0] == events[0]));

        buffer.add(new EventBean[]{events[0]});
        buffer.add(new EventBean[]{events[1]});
        results = buffer.getAndFlush();
        assertTrue((results.length == 2));
        assertSame(events[0], results[0]);
        assertSame(events[1], results[1]);

        buffer.flush();
        assertNull(buffer.getAndFlush());

        // Add multiple events
        buffer.add(new EventBean[]{events[2], events[3]});
        buffer.add(new EventBean[]{events[4], events[5]});
        results = buffer.getAndFlush();
        assertTrue((results.length == 4));
        assertSame(events[2], results[0]);
        assertSame(events[3], results[1]);
        assertSame(events[4], results[2]);
        assertSame(events[5], results[3]);

        buffer.flush();
        assertNull(buffer.getAndFlush());
    }
}
