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

import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;

import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertEquals;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.fail;

public class SupportEventPropUtil {
    public static void assertPropsEquals(EventPropertyDescriptor[] received, SupportEventPropDesc... expected) {
        assertEquals(received.length, expected.length);

        Map<String, EventPropertyDescriptor> receivedProps = new HashMap<>();
        for (EventPropertyDescriptor descReceived : received) {
            if (receivedProps.containsKey(descReceived.getPropertyName())) {
                fail("duplicate '" + descReceived.getPropertyName() + "'");
            }
            receivedProps.put(descReceived.getPropertyName(), descReceived);
        }

        Map<String, SupportEventPropDesc> expectedProps = new HashMap<>();
        for (SupportEventPropDesc expectedDesc : expected) {
            if (expectedProps.containsKey(expectedDesc.getPropertyName())) {
                fail("duplicate '" + expectedDesc.getPropertyName() + "'");
            }
            expectedProps.put(expectedDesc.getPropertyName(), expectedDesc);
        }

        for (EventPropertyDescriptor receivedDesc : received) {
            SupportEventPropDesc expectedDesc = expectedProps.get(receivedDesc.getPropertyName());
            if (expectedDesc == null) {
                fail("could not find in expected the name '" + receivedDesc.getPropertyName() + "'");
            }
            assertPropEquals(expectedDesc, receivedDesc);
        }
    }

    public static void assertPropEquals(SupportEventPropDesc expected, EventPropertyDescriptor received) {
        String message = "comparing '" + expected.getPropertyName() + "'";
        assertEquals(message, expected.getPropertyName(), received.getPropertyName());
        assertEquals(message, expected.getPropertyType(), received.getPropertyEPType());
        assertEquals(message, expected.getComponentType(), received.getPropertyComponentType());
        assertEquals(message, expected.getPropertyType(), received.getPropertyEPType());
        assertEquals(message, expected.isFragment(), received.isFragment());
        assertEquals(message, expected.isIndexed(), received.isIndexed());
        assertEquals(message, expected.isRequiresIndex(), received.isRequiresIndex());
        assertEquals(message, expected.isMapped(), received.isMapped());
        assertEquals(message, expected.isRequiresMapkey(), received.isRequiresMapkey());
    }

    public static void assertTypes(EventType type, String[] fields, EPTypeClass[] classes) {
        int count = 0;
        for (String field : fields) {
            assertEquals("position " + count, classes[count++], type.getPropertyEPType(field));
        }
    }

    public static void assertTypes(EventType type, String field, EPTypeClass clazz) {
        assertTypes(type, new String[]{field}, new EPTypeClass[]{clazz});
    }

    public static void assertTypesAllSame(EventType type, String[] fields, EPTypeClass clazz) {
        int count = 0;
        for (String field : fields) {
            assertEquals("position " + count, clazz, type.getPropertyEPType(field));
        }
    }
}
