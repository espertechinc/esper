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
package com.espertech.esper.event;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.supportunit.bean.SupportBeanSimple;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestWrapperEventType extends TestCase {
    private EventType underlyingEventTypeOne;
    private EventType underlyingEventTypeTwo;
    private EventTypeSPI eventType;
    private Map<String, Object> properties;
    private EventAdapterService eventAdapterService;

    protected void setUp() {
        underlyingEventTypeOne = new BeanEventType(null, 1, SupportBeanSimple.class, SupportEventAdapterService.getService(), null);
        underlyingEventTypeTwo = new BeanEventType(null, 1, SupportBean_A.class, SupportEventAdapterService.getService(), null);
        properties = new HashMap<String, Object>();
        properties.put("additionalString", String.class);
        properties.put("additionalInt", Integer.class);
        eventAdapterService = SupportEventAdapterService.getService();
        EventTypeMetadata meta = EventTypeMetadata.createWrapper("test", true, false, false);
        eventType = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeOne, properties, eventAdapterService);
    }

    public void testTypeUpdate() {
        Map<String, Object> typeOne = new HashMap<String, Object>();
        typeOne.put("field1", String.class);
        MapEventType underlying = new MapEventType(EventTypeMetadata.createAnonymous("noname", EventTypeMetadata.ApplicationType.MAP), "noname", 1, eventAdapterService, typeOne, null, null, null);
        EventTypeMetadata meta = EventTypeMetadata.createWrapper("test", true, false, false);
        eventType = new WrapperEventType(meta, "mytype", 1, underlying, properties, eventAdapterService);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"additionalString", "additionalInt", "field1"}, eventType.getPropertyNames());
        underlying.addAdditionalProperties(Collections.<String, Object>singletonMap("field2", String.class), eventAdapterService);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{"additionalString", "additionalInt", "field1", "field2"}, eventType.getPropertyNames());
        assertEquals(4, eventType.getPropertyDescriptors().length);
        assertEquals(String.class, eventType.getPropertyDescriptor("field2").getPropertyType());
    }

    public void testInvalidRepeatedNames() {
        properties.clear();
        properties.put("myString", String.class);

        try {
            // The myString property occurs in both the event and the map
            eventType = new WrapperEventType(null, "mytype", 1, underlyingEventTypeOne, properties, eventAdapterService);
            fail();
        } catch (EPException ex) {
            // Expected
        }
    }

    public void testGetPropertyNames() {
        String[] expected = new String[]{"myInt", "myString", "additionalInt", "additionalString"};
        EPAssertionUtil.assertEqualsAnyOrder(expected, eventType.getPropertyNames());
    }

    public void testGetPropertyType() {
        assertEquals(int.class, eventType.getPropertyType("myInt"));
        assertEquals(Integer.class, eventType.getPropertyType("additionalInt"));
        assertEquals(String.class, eventType.getPropertyType("additionalString"));
        assertEquals(String.class, eventType.getPropertyType("myString"));
        assertNull(eventType.getPropertyType("unknownProperty"));
    }

    public void testIsProperty() {
        assertTrue(eventType.isProperty("myInt"));
        assertTrue(eventType.isProperty("additionalInt"));
        assertTrue(eventType.isProperty("additionalString"));
        assertTrue(eventType.isProperty("myString"));
        assertFalse(eventType.isProperty("unknownProperty"));
    }

    public void testEquals() {
        Map<String, Object> otherProperties = new HashMap<String, Object>(properties);
        EventTypeMetadata meta = EventTypeMetadata.createWrapper("test", true, false, false);
        EventTypeSPI otherType = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeOne, otherProperties, eventAdapterService);
        assertTrue(eventType.equalsCompareType(otherType));
        assertTrue(otherType.equalsCompareType(eventType));

        otherType = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeTwo, otherProperties, eventAdapterService);
        assertFalse(eventType.equalsCompareType(otherType));
        assertFalse(otherType.equalsCompareType(eventType));

        otherProperties.put("anotherProperty", Integer.class);
        otherType = new WrapperEventType(meta, "mytype", 1, underlyingEventTypeOne, otherProperties, eventAdapterService);
        assertFalse(eventType.equalsCompareType(otherType));
        assertFalse(otherType.equalsCompareType(eventType));

        otherType = (EventTypeSPI) underlyingEventTypeOne;
        assertFalse(eventType.equalsCompareType(otherType));
        assertFalse(otherType.equalsCompareType(eventType));
    }
}
