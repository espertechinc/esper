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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.Set;

public class TestPropertyIndexedEventTable extends TestCase {
    private String[] propertyNames;
    private EventType eventType;
    private EventBean[] testEvents;
    private Object[] testEventsUnd;
    private PropertyIndexedEventTable index;

    public void setUp() {
        propertyNames = new String[]{"intPrimitive", "theString"};
        eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        PropertyIndexedEventTableFactory factory = new PropertyIndexedEventTableFactory(1, eventType, propertyNames, false, null);
        index = (PropertyIndexedEventTable) factory.makeEventTables(null, null)[0];

        // Populate with testEvents
        int intValues[] = new int[]{0, 1, 1, 2, 1, 0};
        String stringValues[] = new String[]{"a", "b", "c", "a", "b", "c"};

        testEvents = new EventBean[intValues.length];
        testEventsUnd = new Object[intValues.length];
        for (int i = 0; i < intValues.length; i++) {
            testEvents[i] = makeBean(intValues[i], stringValues[i]);
            testEventsUnd[i] = testEvents[i].getUnderlying();
        }
        index.add(testEvents, null);
    }

    public void testFind() {
        Set<EventBean> result = index.lookup(new Object[]{1, "a"});
        assertNull(result);

        result = index.lookup(new Object[]{1, "b"});
        assertEquals(2, result.size());
        assertTrue(result.contains(testEvents[1]));
        assertTrue(result.contains(testEvents[4]));

        result = index.lookup(new Object[]{0, "c"});
        assertEquals(1, result.size());
        assertTrue(result.contains(testEvents[5]));

        result = index.lookup(new Object[]{0, "a"});
        assertEquals(1, result.size());
        assertTrue(result.contains(testEvents[0]));
    }

    public void testAdd() {
        // Add event without these properties should fail
        EventBean theEvent = SupportEventBeanFactory.createObject(new SupportBean_A("d"));
        try {
            index.add(new EventBean[]{theEvent}, null);
            TestCase.fail();
        } catch (PropertyAccessException ex) {
            // Expected
        }

        // Add null should fail
        try {
            index.add(new EventBean[]{null}, null);
            TestCase.fail();
        } catch (NullPointerException ex) {
            // Expected
        }
    }

    public void testRemove() {
        index.remove(testEvents, null);
    }

    public void testAddArray() {
        PropertyIndexedEventTableFactory factory = new PropertyIndexedEventTableFactory(1, eventType, propertyNames, false, null);
        index = (PropertyIndexedEventTable) factory.makeEventTables(null, null)[0];

        // Add just 2
        EventBean[] events = new EventBean[2];
        events[0] = testEvents[1];
        events[1] = testEvents[4];
        index.add(events, null);

        Set<EventBean> result = index.lookup(new Object[]{1, "b"});
        assertEquals(2, result.size());
    }

    public void testRemoveArray() {
        index.remove(testEvents, null);

        Set<EventBean> result = index.lookup(new Object[]{1, "b"});
        assertNull(result);

        // Remove again - already removed but won't throw an exception
        index.remove(testEvents, null);
    }

    public void testMixed() {
        index.remove(new EventBean[]{testEvents[1]}, null);
        Set<EventBean> result = index.lookup(new Object[]{1, "b"});
        assertEquals(1, result.size());
        assertTrue(result.contains(testEvents[4]));

        // iterate
        Object[] underlying = EPAssertionUtil.iteratorToArrayUnderlying(index.iterator());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{testEventsUnd[0], testEventsUnd[2], testEventsUnd[3], testEventsUnd[4], testEventsUnd[5]}, underlying);

        index.remove(new EventBean[]{testEvents[4]}, null);
        result = index.lookup(new Object[]{1, "b"});
        assertNull(result);

        // iterate
        underlying = EPAssertionUtil.iteratorToArrayUnderlying(index.iterator());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{testEventsUnd[0], testEventsUnd[2], testEventsUnd[3], testEventsUnd[5]}, underlying);

        index.add(new EventBean[]{testEvents[1]}, null);
        result = index.lookup(new Object[]{1, "b"});
        assertEquals(1, result.size());
        assertTrue(result.contains(testEvents[1]));

        // iterate
        underlying = EPAssertionUtil.iteratorToArrayUnderlying(index.iterator());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{testEventsUnd[0], testEventsUnd[1], testEventsUnd[2], testEventsUnd[3], testEventsUnd[5]}, underlying);
    }

    public void testIterator() {
        Object[] underlying = EPAssertionUtil.iteratorToArrayUnderlying(index.iterator());
        EPAssertionUtil.assertEqualsAnyOrder(testEventsUnd, underlying);
    }

    private EventBean makeBean(int intValue, String stringValue) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intValue);
        bean.setTheString(stringValue);
        return SupportEventBeanFactory.createObject(bean);
    }
}
