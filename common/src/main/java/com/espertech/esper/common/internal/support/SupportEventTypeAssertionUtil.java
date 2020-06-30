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
package com.espertech.esper.common.internal.support;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.scopetest.ScopeTestHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.*;

public class SupportEventTypeAssertionUtil {

    public static void assertFragments(EventBean event, boolean isNative, boolean array, String propertyExpressions) {
        String[] names = propertyExpressions.split(",");
        for (String name : names) {
            if (!array) {
                assertFragmentNonArray(event, isNative, name);
            } else {
                assertFragmentArray(event, isNative, name);
            }
        }
    }

    public static void assertConsistency(EventBean eventBean) {
        assertConsistencyRecursive(eventBean, new HashSet<EventType>());
    }

    public static void assertConsistency(EventType eventType) {
        assertConsistencyRecursive(eventType, new HashSet<EventType>());
    }

    public static String print(EventBean theEvent) {
        StringWriter writer = new StringWriter();
        print(theEvent, writer, 0, new Stack<String>());
        return writer.toString();
    }

    private static void print(EventBean theEvent, StringWriter writer, int indent, Stack<String> propertyStack) {
        writeIndent(writer, indent);
        writer.append("Properties : \n");
        printProperties(theEvent, writer, indent + 2, propertyStack);

        // count fragments
        int countFragments = 0;
        for (EventPropertyDescriptor desc : theEvent.getEventType().getPropertyDescriptors()) {
            if (desc.isFragment()) {
                countFragments++;
            }
        }
        if (countFragments == 0) {
            return;
        }

        writeIndent(writer, indent);
        writer.append("Fragments : (" + countFragments + ") \n");
        for (EventPropertyDescriptor desc : theEvent.getEventType().getPropertyDescriptors()) {
            if (!desc.isFragment()) {
                continue;
            }

            writeIndent(writer, indent + 2);
            writer.append(desc.getPropertyName());
            writer.append(" : ");

            if (desc.isRequiresIndex()) {
                writer.append("\n");
                int count = 0;
                while (true) {
                    try {
                        writeIndent(writer, indent + 4);
                        writer.append("bean #");
                        writer.append(Integer.toString(count));
                        EventBean result = (EventBean) theEvent.getFragment(desc.getPropertyName() + "[" + count + "]");
                        if (result == null) {
                            writer.append("(null EventBean)\n");
                        } else {
                            writer.append("\n");
                            propertyStack.push(desc.getPropertyName());
                            print(result, writer, indent + 6, propertyStack);
                            propertyStack.pop();
                        }
                        count++;
                    } catch (PropertyAccessException ex) {
                        writer.append("-- no access --\n");
                        break;
                    }
                }
            } else {
                Object fragment = theEvent.getFragment(desc.getPropertyName());
                if (fragment == null) {
                    writer.append("(null)\n");
                    continue;
                }

                if (fragment instanceof EventBean) {
                    EventBean fragmentBean = (EventBean) fragment;
                    writer.append("EventBean type ");
                    writer.append(fragmentBean.getEventType().getName());
                    writer.append("...\n");

                    // prevent getThis() loops
                    if (fragmentBean.getEventType() == theEvent.getEventType()) {
                        writeIndent(writer, indent + 2);
                        writer.append("Skipping");
                    } else {
                        propertyStack.push(desc.getPropertyName());
                        print(fragmentBean, writer, indent + 4, propertyStack);
                        propertyStack.pop();
                    }
                } else {
                    EventBean[] fragmentBeans = (EventBean[]) fragment;
                    writer.append("EventBean[] type ");
                    if (fragmentBeans.length == 0) {
                        writer.append("(empty array)\n");
                    } else {
                        writer.append(fragmentBeans[0].getEventType().getName());
                        writer.append("...\n");
                        for (int i = 0; i < fragmentBeans.length; i++) {
                            writeIndent(writer, indent + 4);
                            writer.append("bean #" + i + "...\n");

                            propertyStack.push(desc.getPropertyName());
                            print(fragmentBeans[i], writer, indent + 6, propertyStack);
                            propertyStack.pop();
                        }
                    }
                }
            }
        }
    }

    private static void printProperties(EventBean eventBean, StringWriter writer, int indent, Stack<String> propertyStack) {
        EventPropertyDescriptor[] properties = eventBean.getEventType().getPropertyDescriptors();

        // write simple properties
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();

            if (properties[i].isIndexed() || properties[i].isMapped()) {
                continue;
            }

            writeIndent(writer, indent);
            writer.append(propertyName);
            writer.append(" : ");

            Object resultGet = eventBean.get(propertyName);
            writeValue(writer, resultGet);
            writer.append("\n");
        }

        // write indexed properties
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();

            if (!properties[i].isIndexed()) {
                continue;
            }

            writeIndent(writer, indent);
            writer.append(propertyName);
            String type = "array";
            if (properties[i].isRequiresIndex()) {
                type = type + " requires-index";
            }
            writer.append(" (" + type + ") : ");

            if (properties[i].isRequiresIndex()) {
                int count = 0;
                writer.append("\n");
                while (true) {
                    try {
                        writeIndent(writer, indent + 2);
                        writer.append("#");
                        writer.append(Integer.toString(count));
                        writer.append(" ");
                        Object result = eventBean.get(propertyName + "[" + count + "]");
                        writeValue(writer, result);
                        writer.append("\n");
                        count++;
                    } catch (PropertyAccessException ex) {
                        writer.append("-- no access --\n");
                        break;
                    }
                }
            } else {
                Object result = eventBean.get(propertyName);
                writeValue(writer, result);
                writer.append("\n");
            }
        }

        // write mapped properties
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();

            if (!properties[i].isMapped()) {
                continue;
            }

            writeIndent(writer, indent);
            writer.append(propertyName);
            String type = "mapped";
            if (properties[i].isRequiresMapkey()) {
                type = type + " requires-mapkey";
            }
            writer.append(" (" + type + ") : ");

            if (!properties[i].isRequiresMapkey()) {
                Object result = eventBean.get(propertyName);
                writeValue(writer, result);
                writer.append("\n");
            } else {
                writer.append("??map key unknown??\n");
            }
        }
    }

    private static void assertConsistencyRecursive(EventBean eventBean, Set<EventType> alreadySeenTypes) {
        assertConsistencyRecursive(eventBean.getEventType(), alreadySeenTypes);

        EventPropertyDescriptor[] properties = eventBean.getEventType().getPropertyDescriptors();
        for (int i = 0; i < properties.length; i++) {
            String failedMessage = "failed assertion for property '" + properties[i].getPropertyName() + "' ";
            String propertyName = properties[i].getPropertyName();

            // assert getter
            if ((!properties[i].isRequiresIndex()) && (!properties[i].isRequiresMapkey())) {
                EventPropertyGetter getter = eventBean.getEventType().getGetter(propertyName);
                Object resultGetter = getter.get(eventBean);
                Object resultGet = eventBean.get(propertyName);

                if ((resultGetter == null) && (resultGet == null)) {
                    // fine
                } else if (resultGet instanceof NodeList) {
                    assertEquals(failedMessage, ((NodeList) resultGet).getLength(), ((NodeList) resultGetter).getLength());
                } else if (resultGet.getClass().isArray()) {
                    assertEquals(failedMessage, Array.getLength(resultGet), Array.getLength(resultGetter));
                } else {
                    assertEquals(failedMessage, resultGet, resultGetter);
                }

                if (resultGet != null) {
                    if (resultGet instanceof EventBean[] || resultGet instanceof EventBean) {
                        assertTrue(properties[i].isFragment());
                    } else {
                        Class propertyType = properties[i].getPropertyType();
                        Class resultGetClass = resultGet.getClass();
                        assertTrue(failedMessage, JavaClassHelper.isSubclassOrImplementsInterface(resultGetClass, JavaClassHelper.getBoxedType(propertyType)));
                    }
                }
            }

            // fragment
            if (!properties[i].isFragment()) {
                ScopeTestHelper.assertNull(failedMessage, eventBean.getFragment(propertyName));
                continue;
            }

            FragmentEventType fragmentType = eventBean.getEventType().getFragmentType(propertyName);
            ScopeTestHelper.assertNotNull(failedMessage, fragmentType);

            // fragment can be null
            Object fragment = eventBean.getFragment(propertyName);
            if (fragment == null) {
                return;
            }

            if (!fragmentType.isIndexed()) {
                assertTrue(failedMessage, fragment instanceof EventBean);
                EventBean fragmentEvent = (EventBean) fragment;
                assertConsistencyRecursive(fragmentEvent, alreadySeenTypes);
            } else {
                assertTrue(failedMessage, fragment instanceof EventBean[]);
                EventBean[] events = (EventBean[]) fragment;
                assertTrue(failedMessage, events.length > 0);
                for (EventBean theEvent : events) {
                    assertConsistencyRecursive(theEvent, alreadySeenTypes);
                }
            }
        }
    }

    private static void assertConsistencyRecursive(EventType eventType, Set<EventType> alreadySeenTypes) {
        if (alreadySeenTypes.contains(eventType)) {
            return;
        }
        alreadySeenTypes.add(eventType);

        assertConsistencyProperties(eventType);

        // test fragments
        for (EventPropertyDescriptor descriptor : eventType.getPropertyDescriptors()) {
            String failedMessage = "failed assertion for property '" + descriptor.getPropertyName() + "' ";
            if (!descriptor.isFragment()) {
                ScopeTestHelper.assertNull(failedMessage, eventType.getFragmentType(descriptor.getPropertyName()));
                continue;
            }

            FragmentEventType fragment = eventType.getFragmentType(descriptor.getPropertyName());
            if (!descriptor.isRequiresIndex()) {
                ScopeTestHelper.assertNotNull(failedMessage, fragment);
                if (fragment.isIndexed()) {
                    assertTrue(descriptor.isIndexed());
                }
                assertConsistencyRecursive(fragment.getFragmentType(), alreadySeenTypes);
            } else {
                fragment = eventType.getFragmentType(descriptor.getPropertyName() + "[0]");
                ScopeTestHelper.assertNotNull(failedMessage, fragment);
                assertTrue(descriptor.isIndexed());
                assertConsistencyRecursive(fragment.getFragmentType(), alreadySeenTypes);
            }
        }
    }

    private static void assertConsistencyProperties(EventType eventType) {
        List<String> propertyNames = new ArrayList<String>();

        EventPropertyDescriptor[] properties = eventType.getPropertyDescriptors();
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();
            propertyNames.add(propertyName);
            String failedMessage = "failed assertion for property '" + propertyName + "' ";

            // assert presence of descriptor
            assertSame(properties[i], eventType.getPropertyDescriptor(propertyName));

            // test properties that can simply be in a property expression
            if ((!properties[i].isRequiresIndex()) && (!properties[i].isRequiresMapkey())) {
                assertTrue(failedMessage, eventType.isProperty(propertyName));
                assertSame(failedMessage, eventType.getPropertyType(propertyName), properties[i].getPropertyType());
                ScopeTestHelper.assertNotNull(failedMessage, eventType.getGetter(propertyName));
            }

            // test indexed property
            if (properties[i].isIndexed()) {
                String propertyNameIndexed = propertyName + "[0]";
                assertTrue(failedMessage, eventType.isProperty(propertyNameIndexed));
                ScopeTestHelper.assertNotNull(failedMessage, eventType.getPropertyType(propertyNameIndexed));
                ScopeTestHelper.assertNotNull(failedMessage, eventType.getGetter(propertyNameIndexed));
            }

            // test mapped property
            if (properties[i].isRequiresMapkey()) {
                String propertyNameMapped = propertyName + "('a')";
                assertTrue(failedMessage, eventType.isProperty(propertyNameMapped));
                ScopeTestHelper.assertNotNull(failedMessage, eventType.getPropertyType(propertyNameMapped));
                ScopeTestHelper.assertNotNull(failedMessage, eventType.getGetter(propertyNameMapped));
            }

            // consistent flags
            assertFalse(failedMessage, properties[i].isIndexed() && properties[i].isMapped());
            if (properties[i].isRequiresIndex()) {
                assertTrue(failedMessage, properties[i].isIndexed());
            }
            if (properties[i].isRequiresMapkey()) {
                assertTrue(failedMessage, properties[i].isMapped());
            }
        }

        // assert same property names
        EPAssertionUtil.assertEqualsAnyOrder(eventType.getPropertyNames(), propertyNames.toArray());
    }

    private static void writeIndent(StringWriter writer, int indent) {
        for (int i = 0; i < indent; i++) {
            writer.write(' ');
        }
    }

    private static void writeValue(StringWriter writer, Object result) {
        if (result == null) {
            writer.append("(null)");
            return;
        }

        if (result.getClass().isArray()) {
            writer.append("Array len=");
            writer.append(Integer.toString(Array.getLength(result)));
            writer.append("{");
            String delimiter = "";
            for (int i = 0; i < Array.getLength(result); i++) {
                writer.append(delimiter);
                writeValue(writer, Array.get(result, i));
                delimiter = ", ";
            }
            writer.append("}");
        } else {
            writer.append(result.toString());
        }
    }

    public static void assertEventTypeProperties(Object[][] expectedArr, EventType eventType, SupportEventTypeAssertionEnum... assertions) {
        for (int propNum = 0; propNum < expectedArr.length; propNum++) {
            String message = "Failed assertion for property " + propNum;
            EventPropertyDescriptor prop = eventType.getPropertyDescriptors()[propNum];

            for (int i = 0; i < assertions.length; i++) {
                SupportEventTypeAssertionEnum assertion = assertions[i];
                Object expected = expectedArr[propNum][i];
                Object value = assertion.getExtractor().extract(prop, eventType);
                if (expected == Object[].class && ((Class) value).isArray() && !((Class) value).getComponentType().isPrimitive()) {
                    continue;
                }
                assertEquals(message + " at assertion " + assertion, expected, value);
            }
        }
    }

    private static void assertFragmentNonArray(EventBean event, boolean isNative, String propertyExpression) {
        EventBean fragmentBean = (EventBean) event.getFragment(propertyExpression);
        FragmentEventType fragmentType = event.getEventType().getFragmentType(propertyExpression);
        assertFalse("failed for " + propertyExpression, fragmentType.isIndexed());
        assertEquals("failed for " + propertyExpression, isNative, fragmentType.isNative());
        assertSame("failed for " + propertyExpression, fragmentBean.getEventType(), fragmentType.getFragmentType());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentBean);
    }

    private static void assertFragmentArray(EventBean event, boolean isNative, String propertyExpression) {
        EventBean[] fragmentBean = (EventBean[]) event.getFragment(propertyExpression);
        FragmentEventType fragmentType = event.getEventType().getFragmentType(propertyExpression);
        assertTrue("failed for " + propertyExpression, fragmentType.isIndexed());
        assertEquals("failed for " + propertyExpression, isNative, fragmentType.isNative());
        assertSame("failed for " + propertyExpression, fragmentBean[0].getEventType(), fragmentType.getFragmentType());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentBean[0]);
    }

    public static void assertPropertiesTypes(EventType eventType, String names, Class... type) {
        String[] split = names.split(",");
        for (int i = 0; i < split.length; i++) {
            assertEquals("Type for " + split[i], type[i], eventType.getPropertyType(split[i]));
        }
    }
}