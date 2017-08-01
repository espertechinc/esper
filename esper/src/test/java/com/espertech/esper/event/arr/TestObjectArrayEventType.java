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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBeanComplexProps;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestObjectArrayEventType extends TestCase {

    private EventAdapterService eventAdapterService;
    private ObjectArrayEventType eventType;

    public void setUp() {
        eventAdapterService = SupportEventAdapterService.getService();

        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.OBJECTARR, "typename", true, true, true, false, false);
        String[] names = {"myInt", "myIntBoxed", "myString", "mySupportBean", "myComplexBean", "myNullType"};
        Object[] types = {Integer.class, Integer.class, String.class, SupportBean.class, SupportBeanComplexProps.class, null};

        Map<String, Object> namesAndTypes = new LinkedHashMap<String, Object>();
        for (int i = 0; i < names.length; i++) {
            namesAndTypes.put(names[i], types[i]);
        }

        eventType = new ObjectArrayEventType(metadata, "typename", 1, eventAdapterService, namesAndTypes, null, null, null);
    }

    public void testGetPropertyNames() {
        String[] properties = eventType.getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(properties, new String[]{"myInt", "myIntBoxed", "myString", "mySupportBean", "myComplexBean", "myNullType"});
    }

    public void testGetPropertyType() {
        assertEquals(Integer.class, eventType.getPropertyType("myInt"));
        assertEquals(Integer.class, eventType.getPropertyType("myIntBoxed"));
        assertEquals(String.class, eventType.getPropertyType("myString"));
        assertEquals(SupportBean.class, eventType.getPropertyType("mySupportBean"));
        assertEquals(SupportBeanComplexProps.class, eventType.getPropertyType("myComplexBean"));
        assertEquals(Integer.class, eventType.getPropertyType("mySupportBean.intPrimitive"));
        assertEquals(String.class, eventType.getPropertyType("myComplexBean.nested.nestedValue"));
        assertEquals(Integer.class, eventType.getPropertyType("myComplexBean.indexed[1]"));
        assertEquals(String.class, eventType.getPropertyType("myComplexBean.mapped('a')"));
        assertEquals(null, eventType.getPropertyType("myNullType"));

        assertNull(eventType.getPropertyType("dummy"));
        assertNull(eventType.getPropertyType("mySupportBean.dfgdg"));
        assertNull(eventType.getPropertyType("xxx.intPrimitive"));
        assertNull(eventType.getPropertyType("myComplexBean.nested.nestedValueXXX"));
    }

    public void testGetUnderlyingType() {
        assertEquals(Object[].class, eventType.getUnderlyingType());
    }

    public void testIsValidProperty() {
        assertTrue(eventType.isProperty("myInt"));
        assertTrue(eventType.isProperty("myIntBoxed"));
        assertTrue(eventType.isProperty("myString"));
        assertTrue(eventType.isProperty("mySupportBean.intPrimitive"));
        assertTrue(eventType.isProperty("myComplexBean.nested.nestedValue"));
        assertTrue(eventType.isProperty("myComplexBean.indexed[1]"));
        assertTrue(eventType.isProperty("myComplexBean.mapped('a')"));
        assertTrue(eventType.isProperty("myNullType"));

        assertFalse(eventType.isProperty("dummy"));
        assertFalse(eventType.isProperty("mySupportBean.dfgdg"));
        assertFalse(eventType.isProperty("xxx.intPrimitive"));
        assertFalse(eventType.isProperty("myComplexBean.nested.nestedValueXXX"));
    }

    public void testGetGetter() {
        SupportBean nestedSupportBean = new SupportBean();
        nestedSupportBean.setIntPrimitive(100);
        SupportBeanComplexProps complexPropBean = SupportBeanComplexProps.makeDefaultBean();

        assertEquals(null, eventType.getGetter("dummy"));

        Object[] values = new Object[]{20, 20, "a", nestedSupportBean, complexPropBean, null};
        EventBean eventBean = new ObjectArrayEventBean(values, eventType);

        assertEquals(20, eventType.getGetter("myInt").get(eventBean));
        assertEquals(20, eventType.getGetter("myIntBoxed").get(eventBean));
        assertEquals("a", eventType.getGetter("myString").get(eventBean));
        assertEquals(nestedSupportBean, eventType.getGetter("mySupportBean").get(eventBean));
        assertEquals(100, eventType.getGetter("mySupportBean.intPrimitive").get(eventBean));
        assertEquals("nestedValue", eventType.getGetter("myComplexBean.nested.nestedValue").get(eventBean));

        try {
            eventBean = SupportEventBeanFactory.createObject(new Object());
            eventType.getGetter("myInt").get(eventBean);
            assertTrue(false);
        } catch (ClassCastException ex) {
        }
    }

    public void testGetSuperTypes() {
        assertNull(eventType.getSuperTypes());
    }
}
