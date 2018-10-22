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
package com.espertech.esper.common.internal.event.property;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryCompileTime;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

public class TestIndexedProperty extends TestCase {
    private IndexedProperty[] indexed;
    private EventBean theEvent;
    private BeanEventType eventType;

    public void setUp() {
        indexed = new IndexedProperty[4];
        indexed[0] = new IndexedProperty("indexed", 0);
        indexed[1] = new IndexedProperty("indexed", 1);
        indexed[2] = new IndexedProperty("arrayProperty", 0);
        indexed[3] = new IndexedProperty("arrayProperty", 1);

        theEvent = SupportEventBeanFactory.createObject(SupportBeanComplexProps.makeDefaultBean());
        eventType = (BeanEventType) theEvent.getEventType();
    }

    public void testGetGetter() {
        int[] expected = new int[]{1, 2, 10, 20};
        for (int i = 0; i < indexed.length; i++) {
            EventPropertyGetter getter = indexed[i].getGetter(eventType, EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
            assertEquals(expected[i], getter.get(theEvent));
        }

        // try invalid case
        IndexedProperty ind = new IndexedProperty("dummy", 0);
        assertNull(ind.getGetter(eventType, EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
    }

    public void testGetPropertyType() {
        Class[] expected = new Class[]{int.class, int.class, int.class, int.class};
        for (int i = 0; i < indexed.length; i++) {
            assertEquals(expected[i], indexed[i].getPropertyType(eventType, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
        }

        // try invalid case
        IndexedProperty ind = new IndexedProperty("dummy", 0);
        assertNull(ind.getPropertyType(eventType, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
    }
}
