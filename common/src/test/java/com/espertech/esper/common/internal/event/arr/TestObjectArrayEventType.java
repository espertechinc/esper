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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestObjectArrayEventType extends TestCase {

    private ObjectArrayEventType eventType;

    public void setUp() {
        EventTypeMetadata metadata = new EventTypeMetadata("MyType", null, EventTypeTypeClass.STREAM, EventTypeApplicationType.OBJECTARR, NameAccessModifier.PROTECTED, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        String[] names = {"myInt", "myIntBoxed", "myString", "mySupportBean", "myComplexBean", "myNullType"};
        Object[] types = {Integer.class, Integer.class, String.class, SupportBean.class, SupportBeanComplexProps.class, null};

        Map<String, Object> namesAndTypes = new LinkedHashMap<String, Object>();
        for (int i = 0; i < names.length; i++) {
            namesAndTypes.put(names[i], types[i]);
        }

        eventType = new ObjectArrayEventType(metadata, namesAndTypes, null, null, null, null, SupportEventTypeFactory.BEAN_EVENT_TYPE_FACTORY);
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
    }

    public void testGetSuperTypes() {
        assertNull(eventType.getSuperTypes());
    }
}
