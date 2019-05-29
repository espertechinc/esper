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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBeanComplexProps;
import com.espertech.esper.common.internal.support.SupportBeanSimple;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.supportunit.bean.SupportBeanCombinedProps;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestBeanEventType extends TestCase {
    private BeanEventType eventTypeSimple;
    private BeanEventType eventTypeComplex;
    private BeanEventType eventTypeNested;

    private EventBean eventSimple;
    private EventBean eventComplex;
    private EventBean eventNested;

    private SupportBeanSimple objSimple;
    private SupportBeanComplexProps objComplex;
    private SupportBeanCombinedProps objCombined;

    public void setUp() {
        eventTypeSimple = SupportEventTypeFactory.createBeanType(SupportBeanSimple.class);
        eventTypeComplex = SupportEventTypeFactory.createBeanType(SupportBeanComplexProps.class);
        eventTypeNested = SupportEventTypeFactory.createBeanType(SupportBeanCombinedProps.class);

        objSimple = new SupportBeanSimple("a", 20);
        objComplex = SupportBeanComplexProps.makeDefaultBean();
        objCombined = SupportBeanCombinedProps.makeDefaultBean();

        eventSimple = new BeanEventBean(objSimple, eventTypeSimple);
        eventComplex = new BeanEventBean(objComplex, eventTypeComplex);
        eventNested = new BeanEventBean(objCombined, eventTypeNested);
    }

    public void testFragments() {
        FragmentEventType nestedTypeFragment = eventTypeComplex.getFragmentType("nested");
        EventType nestedType = nestedTypeFragment.getFragmentType();
        assertEquals(SupportBeanComplexProps.SupportBeanSpecialGetterNested.class.getName(), nestedType.getName());
        assertEquals(SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, nestedType.getUnderlyingType());
        assertEquals(String.class, nestedType.getPropertyType("nestedValue"));
        assertNull(eventTypeComplex.getFragmentType("indexed[0]"));

        nestedTypeFragment = eventTypeNested.getFragmentType("indexed[0]");
        nestedType = nestedTypeFragment.getFragmentType();
        assertFalse(nestedTypeFragment.isIndexed());
        assertEquals(SupportBeanCombinedProps.NestedLevOne.class.getName(), nestedType.getName());
        assertEquals(Map.class, nestedType.getPropertyType("mapprop"));

        SupportEventTypeAssertionUtil.assertConsistency(eventTypeComplex);
        SupportEventTypeAssertionUtil.assertConsistency(eventTypeNested);
    }

    public void testGetPropertyNames() {
        String[] properties = eventTypeSimple.getPropertyNames();
        assertTrue(properties.length == 2);
        assertTrue(properties[0].equals("myInt"));
        assertTrue(properties[1].equals("myString"));
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("myInt", int.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false)
        }, eventTypeSimple.getPropertyDescriptors());

        properties = eventTypeComplex.getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(SupportBeanComplexProps.PROPERTIES, properties);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("simpleProperty", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("mapProperty", Map.class, String.class, false, false, false, true, false),
                new EventPropertyDescriptor("mapped", String.class, Object.class, false, true, false, true, false),
                new EventPropertyDescriptor("indexed", int.class, null, true, false, true, false, false),
                new EventPropertyDescriptor("nested", SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("arrayProperty", int[].class, int.class, false, false, true, false, false),
                new EventPropertyDescriptor("objectArray", Object[].class, Object.class, false, false, true, false, false),
        }, eventTypeComplex.getPropertyDescriptors());

        properties = eventTypeNested.getPropertyNames();
        EPAssertionUtil.assertEqualsAnyOrder(SupportBeanCombinedProps.PROPERTIES, properties);
    }

    public void testGetUnderlyingType() {
        assertEquals(SupportBeanSimple.class, eventTypeSimple.getUnderlyingType());
    }

    public void testGetPropertyType() {
        assertEquals(String.class, eventTypeSimple.getPropertyType("myString"));
        assertNull(null, eventTypeSimple.getPropertyType("dummy"));
    }

    public void testIsValidProperty() {
        assertTrue(eventTypeSimple.isProperty("myString"));
        assertFalse(eventTypeSimple.isProperty("dummy"));
    }

    public void testGetGetter() {
        assertEquals(null, eventTypeSimple.getGetter("dummy"));

        EventPropertyGetter getter = eventTypeSimple.getGetter("myInt");
        assertEquals(20, getter.get(eventSimple));
        getter = eventTypeSimple.getGetter("myString");
        assertEquals("a", getter.get(eventSimple));
    }

    public void testProperties() {
        Class nestedOne = SupportBeanCombinedProps.NestedLevOne.class;
        Class nestedOneArr = SupportBeanCombinedProps.NestedLevOne[].class;
        Class nestedTwo = SupportBeanCombinedProps.NestedLevTwo.class;

        // test nested/combined/indexed/mapped properties
        // PropertyName                 isProperty              getXsSimpleType         hasGetter   getterValue
        List<PropTestDesc> tests = new LinkedList<PropTestDesc>();

        tests = new LinkedList<PropTestDesc>();
        tests.add(new PropTestDesc("simpleProperty", true, String.class, true, "simple"));
        tests.add(new PropTestDesc("dummy", false, null, false, null));
        tests.add(new PropTestDesc("indexed", false, null, false, null));
        tests.add(new PropTestDesc("indexed[1]", true, int.class, true, 2));
        tests.add(new PropTestDesc("nested", true, SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, true, objComplex.getNested()));
        tests.add(new PropTestDesc("nested.nestedValue", true, String.class, true, objComplex.getNested().getNestedValue()));
        tests.add(new PropTestDesc("nested.nestedNested", true, SupportBeanComplexProps.SupportBeanSpecialGetterNestedNested.class, true, objComplex.getNested().getNestedNested()));
        tests.add(new PropTestDesc("nested.nestedNested.nestedNestedValue", true, String.class, true, objComplex.getNested().getNestedNested().getNestedNestedValue()));
        tests.add(new PropTestDesc("nested.dummy", false, null, false, null));
        tests.add(new PropTestDesc("mapped", false, null, false, null));
        tests.add(new PropTestDesc("mapped('keyOne')", true, String.class, true, "valueOne"));
        tests.add(new PropTestDesc("arrayProperty", true, int[].class, true, objComplex.getArrayProperty()));
        tests.add(new PropTestDesc("arrayProperty[1]", true, int.class, true, 20));
        tests.add(new PropTestDesc("mapProperty('xOne')", true, String.class, true, "yOne"));
        tests.add(new PropTestDesc("google('x')", false, null, false, null));
        tests.add(new PropTestDesc("mapped('x')", true, String.class, true, null));
        tests.add(new PropTestDesc("mapped('x').x", false, null, false, null));
        tests.add(new PropTestDesc("mapProperty", true, Map.class, true, objComplex.getMapProperty()));
        runTest(tests, eventTypeComplex, eventComplex);

        tests = new LinkedList<PropTestDesc>();
        tests.add(new PropTestDesc("dummy", false, null, false, null));
        tests.add(new PropTestDesc("myInt", true, int.class, true, objSimple.getMyInt()));
        tests.add(new PropTestDesc("myString", true, String.class, true, objSimple.getMyString()));
        tests.add(new PropTestDesc("dummy('a')", false, null, false, null));
        tests.add(new PropTestDesc("dummy[1]", false, null, false, null));
        tests.add(new PropTestDesc("dummy.nested", false, null, false, null));
        runTest(tests, eventTypeSimple, eventSimple);

        tests = new LinkedList<PropTestDesc>();
        tests.add(new PropTestDesc("indexed", false, null, false, null));
        tests.add(new PropTestDesc("indexed[1]", true, nestedOne, true, objCombined.getIndexed(1)));
        tests.add(new PropTestDesc("indexed.mapped", false, null, false, null));
        tests.add(new PropTestDesc("indexed[1].mapped", false, null, false, null));
        tests.add(new PropTestDesc("array", true, nestedOneArr, true, objCombined.getArray()));
        tests.add(new PropTestDesc("array.mapped", false, null, false, null));
        tests.add(new PropTestDesc("array[0]", true, nestedOne, true, objCombined.getArray()[0]));
        tests.add(new PropTestDesc("array[1].mapped", false, null, false, null));
        tests.add(new PropTestDesc("array[1].mapped('x')", true, nestedTwo, true, objCombined.getArray()[1].getMapped("x")));
        tests.add(new PropTestDesc("array[1].mapped('1mb')", true, nestedTwo, true, objCombined.getArray()[1].getMapped("1mb")));
        tests.add(new PropTestDesc("indexed[1].mapped('x')", true, nestedTwo, true, objCombined.getIndexed(1).getMapped("x")));
        tests.add(new PropTestDesc("indexed[1].mapped('x').value", true, String.class, true, null));
        tests.add(new PropTestDesc("indexed[1].mapped('1mb')", true, nestedTwo, true, objCombined.getIndexed(1).getMapped("1mb")));
        tests.add(new PropTestDesc("indexed[1].mapped('1mb').value", true, String.class, true, objCombined.getIndexed(1).getMapped("1mb").getValue()));
        tests.add(new PropTestDesc("array[1].mapprop", true, Map.class, true, objCombined.getIndexed(1).getMapprop()));
        tests.add(new PropTestDesc("array[1].mapprop('1ma')", true, SupportBeanCombinedProps.NestedLevTwo.class, true, objCombined.getArray()[1].getMapped("1ma")));
        tests.add(new PropTestDesc("array[1].mapprop('1ma').value", true, String.class, true, "1ma0"));
        tests.add(new PropTestDesc("indexed[1].mapprop", true, Map.class, true, objCombined.getIndexed(1).getMapprop()));
        runTest(tests, eventTypeNested, eventNested);

        tryInvalidIsProperty(eventTypeComplex, "x[");
        tryInvalidIsProperty(eventTypeComplex, "dummy()");
        tryInvalidIsProperty(eventTypeComplex, "nested.xx['a']");
        tryInvalidIsProperty(eventTypeNested, "dummy[(");
        tryInvalidIsProperty(eventTypeNested, "array[1].mapprop[x].value");
    }

    private static void tryInvalidIsProperty(BeanEventType type, String property) {
        assertNull(type.getPropertyType(property));
    }

    private static void runTest(List<PropTestDesc> tests, BeanEventType eventType, EventBean eventBean) {
        for (PropTestDesc desc : tests) {
            runTest(desc, eventType, eventBean);
        }
    }

    private static void runTest(PropTestDesc test, BeanEventType eventType, EventBean eventBean) {
        String propertyName = test.getPropertyName();

        assertEquals("isProperty mismatch on '" + propertyName + "',", test.isProperty(), eventType.isProperty(propertyName));
        assertEquals("getPropertyType mismatch on '" + propertyName + "',", test.getClazz(), eventType.getPropertyType(propertyName));

        EventPropertyGetter getter = eventType.getGetter(propertyName);
        if (getter == null) {
            assertFalse("getGetter null on '" + propertyName + "',", test.isHasGetter());
        } else {
            assertTrue("getGetter not null on '" + propertyName + "',", test.isHasGetter());
            if (test.getGetterReturnValue() == NullPointerException.class) {
                try {
                    getter.get(eventBean);
                    fail("getGetter not throwing null pointer on '" + propertyName);
                } catch (NullPointerException ex) {
                    // expected
                }
            } else {
                Object value = getter.get(eventBean);
                assertEquals("getter value mismatch on '" + propertyName + "',", test.getGetterReturnValue(), value);
            }
        }
    }

    public static class PropTestDesc {
        private String propertyName;
        private boolean isProperty;
        private Class clazz;
        private boolean hasGetter;
        private Object getterReturnValue;

        public PropTestDesc(String propertyName, boolean property, Class clazz, boolean hasGetter, Object getterReturnValue) {
            this.propertyName = propertyName;
            isProperty = property;
            this.clazz = clazz;
            this.hasGetter = hasGetter;
            this.getterReturnValue = getterReturnValue;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public boolean isProperty() {
            return isProperty;
        }

        public Class getClazz() {
            return clazz;
        }

        public boolean isHasGetter() {
            return hasGetter;
        }

        public Object getGetterReturnValue() {
            return getterReturnValue;
        }
    }

}
