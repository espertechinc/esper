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
package com.espertech.esper.util;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.Serializable;

public class TestSerializerFactory extends TestCase {

    public void testTypes() throws IOException {
        Object[] expected = new Object[]{2, 3L, 4f, 5.0d, "abc", new byte[]{10, 20}, (byte) 20, (short) 21, true, new MyBean("E1")};
        Class[] classes = new Class[expected.length];
        for (int i = 0; i < expected.length; i++) {
            classes[i] = expected.getClass();
        }

        Serializer[] serializers = SerializerFactory.getSerializers(classes);
        byte[] bytes = SerializerFactory.serialize(serializers, expected);

        Object[] result = SerializerFactory.deserialize(expected.length, bytes, serializers);
        EPAssertionUtil.assertEqualsExactOrder(expected, result);

        // null values are simply not serialized
        bytes = SerializerFactory.serialize(new Serializer[]{SerializerFactory.getSerializer(Integer.class)}, new Object[]{null});
        assertEquals(0, bytes.length);
    }

    public static class MyBean implements Serializable {
        private String id;

        public MyBean(String id) {
            this.id = id;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyBean myBean = (MyBean) o;

            if (!id.equals(myBean.id)) return false;

            return true;
        }

        public int hashCode() {
            return id.hashCode();
        }
    }
}
