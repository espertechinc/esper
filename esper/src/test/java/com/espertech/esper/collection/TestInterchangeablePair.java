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
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class TestInterchangeablePair extends TestCase {
    private InterchangeablePair<String, String> pair1a = new InterchangeablePair<String, String>("a", "b");
    private InterchangeablePair<String, String> pair1b = new InterchangeablePair<String, String>("a", "c");
    private InterchangeablePair<String, String> pair1c = new InterchangeablePair<String, String>("c", "b");
    private InterchangeablePair<String, String> pair1d = new InterchangeablePair<String, String>("a", "b");
    private InterchangeablePair<String, String> pair1e = new InterchangeablePair<String, String>("b", "a");

    private InterchangeablePair<String, String> pair2a = new InterchangeablePair<String, String>("a", null);
    private InterchangeablePair<String, String> pair2b = new InterchangeablePair<String, String>("b", null);
    private InterchangeablePair<String, String> pair2c = new InterchangeablePair<String, String>("a", null);

    private InterchangeablePair<String, String> pair3a = new InterchangeablePair<String, String>(null, "b");
    private InterchangeablePair<String, String> pair3b = new InterchangeablePair<String, String>(null, "c");
    private InterchangeablePair<String, String> pair3c = new InterchangeablePair<String, String>(null, "b");

    private InterchangeablePair<String, String> pair4a = new InterchangeablePair<String, String>(null, null);
    private InterchangeablePair<String, String> pair4b = new InterchangeablePair<String, String>(null, null);

    public void testEquals() {
        assertTrue(pair1a.equals(pair1d) && pair1d.equals(pair1a));
        assertTrue(pair1a.equals(pair1e) && pair1e.equals(pair1a));
        assertFalse(pair1a.equals(pair1b));
        assertFalse(pair1a.equals(pair1c));
        assertFalse(pair1a.equals(pair2a));
        assertFalse(pair1a.equals(pair3a));
        assertFalse(pair1a.equals(pair4a));

        assertTrue(pair2a.equals(pair2c) && pair2c.equals(pair2a));
        assertTrue(pair2b.equals(pair3a) && pair3a.equals(pair2b));
        assertFalse(pair2a.equals(pair2b));
        assertFalse(pair2a.equals(pair1a));
        assertFalse(pair2b.equals(pair1e));
        assertFalse(pair2b.equals(pair3b));
        assertFalse(pair2a.equals(pair4a));

        assertTrue(pair3a.equals(pair3c) && pair3c.equals(pair3a));
        assertTrue(pair3c.equals(pair2b) && pair2b.equals(pair3c));
        assertFalse(pair3a.equals(pair3b));
        assertFalse(pair3b.equals(pair3a));
        assertFalse(pair3a.equals(pair1a));
        assertFalse(pair3a.equals(pair2a));
        assertFalse(pair3a.equals(pair4a));

        assertTrue(pair4a.equals(pair4b) && pair4b.equals(pair4a));
        assertFalse(pair4a.equals(pair1b) || pair4a.equals(pair2a) || pair4a.equals(pair3a));
    }

    public void testHashCode() {
        assertTrue(pair1a.hashCode() == ("a".hashCode() ^ "b".hashCode()));
        assertTrue(pair2a.hashCode() == "a".hashCode());
        assertTrue(pair3a.hashCode() == "b".hashCode());
        assertTrue(pair4a.hashCode() == 0);

        assertTrue(pair1a.hashCode() != pair2a.hashCode());
        assertTrue(pair1a.hashCode() != pair3a.hashCode());
        assertTrue(pair1a.hashCode() != pair4a.hashCode());

        assertTrue(pair1a.hashCode() == pair1d.hashCode());
        assertTrue(pair2a.hashCode() == pair2c.hashCode());
        assertTrue(pair3a.hashCode() == pair3c.hashCode());
        assertTrue(pair4a.hashCode() == pair4b.hashCode());

        assertTrue(pair2b.hashCode() == pair3a.hashCode());
    }

    public void testSetBehavior() {
        Set<InterchangeablePair<EventBean, EventBean>> eventPairs = new HashSet<InterchangeablePair<EventBean, EventBean>>();

        EventBean[] events = new EventBean[4];
        for (int i = 0; i < events.length; i++) {
            events[i] = SupportEventBeanFactory.createObject(new Integer(i));
        }

        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[0], events[1]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[0], events[2]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[1], events[2]));
        assertEquals(3, eventPairs.size());

        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[0], events[1]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[1], events[2]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[2], events[0]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[2], events[1]));
        eventPairs.add(new InterchangeablePair<EventBean, EventBean>(events[1], events[0]));
        assertEquals(3, eventPairs.size());

        assertTrue(eventPairs.contains(new InterchangeablePair<EventBean, EventBean>(events[1], events[0])));
        assertFalse(eventPairs.contains(new InterchangeablePair<EventBean, EventBean>(events[3], events[0])));
        assertTrue(eventPairs.contains(new InterchangeablePair<EventBean, EventBean>(events[1], events[2])));
        assertTrue(eventPairs.contains(new InterchangeablePair<EventBean, EventBean>(events[2], events[0])));

        eventPairs.remove(new InterchangeablePair<EventBean, EventBean>(events[2], events[0]));
        assertFalse(eventPairs.contains(new InterchangeablePair<EventBean, EventBean>(events[2], events[0])));
        eventPairs.remove(new InterchangeablePair<EventBean, EventBean>(events[1], events[2]));
        eventPairs.remove(new InterchangeablePair<EventBean, EventBean>(events[1], events[0]));

        assertTrue(eventPairs.isEmpty());
    }
}
