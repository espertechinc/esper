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
package com.espertech.esper.event.property;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventTypeIdGeneratorImpl;
import com.espertech.esper.event.bean.BeanEventAdapter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.bean.BeanEventTypeFactory;
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TestNestedProperty extends TestCase {
    private NestedProperty[] nested;
    private EventBean theEvent;
    private BeanEventTypeFactory beanEventTypeFactory;

    public void setUp() {
        beanEventTypeFactory = new BeanEventAdapter(new ConcurrentHashMap<Class, BeanEventType>(), SupportEventAdapterService.getService(), new EventTypeIdGeneratorImpl());

        nested = new NestedProperty[2];
        nested[0] = makeProperty(new String[]{"nested", "nestedValue"});
        nested[1] = makeProperty(new String[]{"nested", "nestedNested", "nestedNestedValue"});

        theEvent = SupportEventBeanFactory.createObject(SupportBeanComplexProps.makeDefaultBean());
    }

    public void testGetGetter() {
        EventPropertyGetter getter = nested[0].getGetter((BeanEventType) theEvent.getEventType(), SupportEventAdapterService.getService());
        assertEquals("nestedValue", getter.get(theEvent));

        getter = nested[1].getGetter((BeanEventType) theEvent.getEventType(), SupportEventAdapterService.getService());
        assertEquals("nestedNestedValue", getter.get(theEvent));
    }

    public void testGetPropertyType() {
        assertEquals(String.class, nested[0].getPropertyType((BeanEventType) theEvent.getEventType(), SupportEventAdapterService.getService()));
        assertEquals(String.class, nested[1].getPropertyType((BeanEventType) theEvent.getEventType(), SupportEventAdapterService.getService()));
    }

    private NestedProperty makeProperty(String[] propertyNames) {
        List<Property> properties = new LinkedList<Property>();
        for (String prop : propertyNames) {
            properties.add(new SimpleProperty(prop));
        }
        return new NestedProperty(properties);
    }
}
