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

public class TestMultiKey extends TestCase {
    MultiKey<String> keys1 = new MultiKey<String>(new String[]{"a", "b"});
    MultiKey<String> keys2 = new MultiKey<String>(new String[]{"a", "b"});
    MultiKey<String> keys3 = new MultiKey<String>(new String[]{"a", null});
    MultiKey<String> keys4 = new MultiKey<String>(new String[]{null, "b"});
    MultiKey<String> keys5 = new MultiKey<String>(new String[]{null, null});
    MultiKey<String> keys6 = new MultiKey<String>(new String[]{"a"});
    MultiKey<String> keys7 = new MultiKey<String>(new String[]{"a", "b", "c"});
    MultiKey<String> keys8 = new MultiKey<String>(new String[]{"a", "b", null});
    MultiKey<String> keys9 = new MultiKey<String>(new String[]{"a", "b", "c", "d"});
    MultiKey<String> keys10 = new MultiKey<String>(new String[]{"a", "b", "c", "d"});
    MultiKey<String> keys11 = new MultiKey<String>(new String[]{"espera", "esperb"});
    MultiKey<String> keys12 = new MultiKey<String>(new String[]{"esperc", "esperd"});
    MultiKey<String> keys13 = new MultiKey<String>(new String[]{"espere", "esperf"});

    public void testHashCode() {
        assertTrue(keys11.hashCode() != keys12.hashCode());
        assertTrue(keys12.hashCode() != keys13.hashCode());

        assertTrue(keys1.hashCode() == ("a".hashCode() * 31 ^ "b".hashCode()));
        assertTrue(keys3.hashCode() == "a".hashCode());
        assertTrue(keys4.hashCode() == "b".hashCode());
        assertTrue(keys5.hashCode() == 0);

        assertTrue(keys8.hashCode() == keys1.hashCode());
        assertTrue(keys1.hashCode() == keys2.hashCode());
        assertTrue(keys1.hashCode() != keys3.hashCode());
        assertTrue(keys1.hashCode() != keys4.hashCode());
        assertTrue(keys1.hashCode() != keys5.hashCode());

        assertTrue(keys7.hashCode() != keys8.hashCode());
        assertTrue(keys9.hashCode() == keys10.hashCode());
    }

    public void testEquals() {
        assertEquals(keys2, keys1);
        assertEquals(keys1, keys2);

        assertFalse(keys1.equals(keys3));
        assertFalse(keys3.equals(keys1));
        assertFalse(keys1.equals(keys4));
        assertFalse(keys2.equals(keys5));
        assertFalse(keys3.equals(keys4));
        assertFalse(keys4.equals(keys5));

        assertTrue(keys1.equals(keys1));
        assertTrue(keys2.equals(keys2));
        assertTrue(keys3.equals(keys3));
        assertTrue(keys4.equals(keys4));
        assertTrue(keys5.equals(keys5));

        assertFalse(keys1.equals(keys7));
        assertFalse(keys1.equals(keys8));
        assertFalse(keys1.equals(keys9));
        assertFalse(keys1.equals(keys10));
        assertTrue(keys9.equals(keys10));
    }

    public void testGet() {
        assertEquals(1, keys6.size());
        assertEquals(2, keys1.size());
        assertEquals(3, keys8.size());
        assertEquals(4, keys9.size());

        assertEquals("a", keys1.get(0));
        assertEquals("b", keys1.get(1));
        assertTrue(null == keys4.get(0));
        assertTrue("d" == keys10.get(3));
    }

    public void testWithSet() {
        EventBean[][] testEvents = new EventBean[][]{
                SupportEventBeanFactory.makeEvents(new String[]{"a", "b"}),
                SupportEventBeanFactory.makeEvents(new String[]{"a"}),
                SupportEventBeanFactory.makeEvents(new String[]{"a", "b", "c"}),
                SupportEventBeanFactory.makeEvents(new String[]{"a", "b"}),
        };

        Set<MultiKey> mapSet = new HashSet<MultiKey>();

        // Test contains
        mapSet.add(new MultiKey<EventBean>(testEvents[0]));
        assertTrue(mapSet.contains(new MultiKey<EventBean>(testEvents[0])));
        assertFalse(mapSet.contains(new MultiKey<EventBean>(testEvents[1])));
        assertFalse(mapSet.contains(new MultiKey<EventBean>(testEvents[2])));
        assertFalse(mapSet.contains(new MultiKey<EventBean>(testEvents[3])));

        // Test unique
        mapSet.add(new MultiKey<EventBean>(testEvents[0]));
        assertEquals(1, mapSet.size());

        mapSet.add(new MultiKey<EventBean>(testEvents[1]));
        mapSet.add(new MultiKey<EventBean>(testEvents[2]));
        mapSet.add(new MultiKey<EventBean>(testEvents[3]));
        assertEquals(4, mapSet.size());

        mapSet.remove(new MultiKey<EventBean>(testEvents[0]));
        assertEquals(3, mapSet.size());
        assertFalse(mapSet.contains(new MultiKey<EventBean>(testEvents[0])));

        mapSet.remove(new MultiKey<EventBean>(testEvents[1]));
        mapSet.remove(new MultiKey<EventBean>(testEvents[2]));
        mapSet.remove(new MultiKey<EventBean>(testEvents[3]));
        assertEquals(0, mapSet.size());
    }
}
