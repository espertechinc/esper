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
import com.espertech.esper.supportunit.bean.SupportBean_S0;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestTransformEventIterator extends TestCase {
    private TransformEventIterator iterator;

    public void testEmpty() {
        iterator = makeIterator(new int[0]);
        assertFalse(iterator.hasNext());
    }

    public void testOne() {
        iterator = makeIterator(new int[]{10});
        assertTrue(iterator.hasNext());
        assertEquals(10, iterator.next().get("id"));
        assertFalse(iterator.hasNext());
    }

    public void testTwo() {
        iterator = makeIterator(new int[]{10, 20});
        assertTrue(iterator.hasNext());
        assertEquals(10, iterator.next().get("id"));
        assertTrue(iterator.hasNext());
        assertEquals(20, iterator.next().get("id"));
        assertFalse(iterator.hasNext());
    }

    private TransformEventIterator makeIterator(int[] values) {
        List<EventBean> events = new LinkedList<EventBean>();
        for (int i = 0; i < values.length; i++) {
            SupportBean bean = new SupportBean();
            bean.setIntPrimitive(values[i]);
            EventBean theEvent = SupportEventBeanFactory.createObject(bean);
            events.add(theEvent);
        }
        return new TransformEventIterator(events.iterator(), new MyTransform());
    }

    public class MyTransform implements TransformEventMethod {
        public EventBean transform(EventBean theEvent) {
            Integer value = (Integer) theEvent.get("intPrimitive");
            return SupportEventBeanFactory.createObject(new SupportBean_S0(value));
        }

        public EventBean[] transform(EventBean[] events) {
            return new EventBean[0];
        }
    }
}
