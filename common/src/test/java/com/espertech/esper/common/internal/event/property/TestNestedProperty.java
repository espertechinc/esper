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

import java.util.LinkedList;
import java.util.List;

public class TestNestedProperty extends TestCase {
    private NestedProperty[] nested;
    private EventBean theEvent;

    public void setUp() {

        nested = new NestedProperty[2];
        nested[0] = makeProperty(new String[]{"nested", "nestedValue"});
        nested[1] = makeProperty(new String[]{"nested", "nestedNested", "nestedNestedValue"});

        theEvent = SupportEventBeanFactory.createObject(SupportBeanComplexProps.makeDefaultBean());
    }

    public void testGetGetter() {
        EventPropertyGetter getter = nested[0].getGetter((BeanEventType) theEvent.getEventType(), EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
        assertEquals("nestedValue", getter.get(theEvent));

        getter = nested[1].getGetter((BeanEventType) theEvent.getEventType(), EventBeanTypedEventFactoryCompileTime.INSTANCE, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
        assertEquals("nestedNestedValue", getter.get(theEvent));
    }

    public void testGetPropertyType() {
        assertEquals(String.class, nested[0].getPropertyType((BeanEventType) theEvent.getEventType(), SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
        assertEquals(String.class, nested[1].getPropertyType((BeanEventType) theEvent.getEventType(), SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY));
    }

    private NestedProperty makeProperty(String[] propertyNames) {
        List<Property> properties = new LinkedList<Property>();
        for (String prop : propertyNames) {
            properties.add(new SimpleProperty(prop));
        }
        return new NestedProperty(properties);
    }
}
