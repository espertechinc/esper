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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventTypeIdGeneratorImpl;
import com.espertech.esper.supportunit.bean.ISupportD;
import com.espertech.esper.supportunit.bean.ISupportDImpl;
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import com.espertech.esper.supportunit.bean.SupportBeanSimple;
import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestBeanEventAdapter extends TestCase {
    private BeanEventTypeFactory beanEventTypeFactory;

    public void setUp() {
        beanEventTypeFactory = new BeanEventAdapter(new ConcurrentHashMap<Class, BeanEventType>(), SupportEventAdapterService.getService(), new EventTypeIdGeneratorImpl());
    }

    public void testCreateBeanType() {
        BeanEventType eventType = beanEventTypeFactory.createBeanType("a", SupportBeanSimple.class, true, true, true);

        assertEquals(SupportBeanSimple.class, eventType.getUnderlyingType());
        assertEquals(2, eventType.getPropertyNames().length);

        // Second call to create the event type, should be the same instance as the first
        EventType eventTypeTwo = beanEventTypeFactory.createBeanType("b", SupportBeanSimple.class, true, true, true);
        assertTrue(eventTypeTwo == eventType);

        // Third call to create the event type, getting a given event type id
        EventType eventTypeThree = beanEventTypeFactory.createBeanType("c", SupportBeanSimple.class, true, true, true);
        assertTrue(eventTypeThree == eventType);
    }

    public void testInterfaceProperty() {
        // Assert implementations have full set of properties
        ISupportDImpl theEvent = new ISupportDImpl("D", "BaseD", "BaseDBase");
        EventType typeBean = beanEventTypeFactory.createBeanType(theEvent.getClass().getName(), theEvent.getClass(), true, true, true);
        EventBean bean = new BeanEventBean(theEvent, typeBean);
        assertEquals("D", bean.get("d"));
        assertEquals("BaseD", bean.get("baseD"));
        assertEquals("BaseDBase", bean.get("baseDBase"));
        assertEquals(3, bean.getEventType().getPropertyNames().length);
        EPAssertionUtil.assertEqualsAnyOrder(bean.getEventType().getPropertyNames(),
                new String[]{"d", "baseD", "baseDBase"});

        // Assert intermediate interfaces have full set of fields
        EventType interfaceType = beanEventTypeFactory.createBeanType("d", ISupportD.class, true, true, true);
        EPAssertionUtil.assertEqualsAnyOrder(interfaceType.getPropertyNames(),
                new String[]{"d", "baseD", "baseDBase"});
    }

    public void testMappedIndexedNestedProperty() throws Exception {
        EventType eventType = beanEventTypeFactory.createBeanType("e", SupportBeanComplexProps.class, true, true, true);

        assertEquals(Map.class, eventType.getPropertyType("mapProperty"));
        assertEquals(String.class, eventType.getPropertyType("mapped('x')"));
        assertEquals(int.class, eventType.getPropertyType("indexed[1]"));
        assertEquals(SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, eventType.getPropertyType("nested"));
        assertEquals(int[].class, eventType.getPropertyType("arrayProperty"));
    }
}
