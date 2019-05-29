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
package com.espertech.esper.common.internal.event.json.serde;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayInputStream;
import com.espertech.esper.common.internal.serde.serdeset.builtin.FastByteArrayOutputStream;
import junit.framework.TestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class SupportDIOJson extends TestCase {

    static <T> void assertSerde(DataInputOutputSerde<T> serde, T serialized) {
        Object deserialized;
        try {
            FastByteArrayOutputStream fos = new FastByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(fos);
            serde.write(serialized, dos, null, null);
            dos.close();
            fos.close();
            byte[] bytes = fos.getByteArrayFast();

            FastByteArrayInputStream fis = new FastByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(fis);
            deserialized = serde.read(dis, null);
            dis.close();
            fis.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        if (serialized == null) {
            assertNull(deserialized);
            return;
        }
        compareValue(serialized, deserialized);
    }

    private static void compareMaps(Map<String, Object> expected, Map<String, Object> actual) {
        assertEquals(expected.size(), actual.size());
        Iterator<Map.Entry<String, Object>> expect = expected.entrySet().iterator();
        Iterator<Map.Entry<String, Object>> have = expected.entrySet().iterator();
        while (expect.hasNext()) {
            compareEntry(expect.next(), have.next());
        }
    }

    private static void compareEntry(Map.Entry<String, Object> expected, Map.Entry<String, Object> actual) {
        assertEquals(expected.getKey(), actual.getKey());
        compareValue(expected.getValue(), actual.getValue());
    }

    private static void compareValue(Object expected, Object actual) {
        if (expected instanceof Object[]) {
            compareArrays((Object[]) expected, (Object[]) actual);
        } else if (expected instanceof Map) {
            compareMaps((Map) expected, (Map) actual);
        } else {
            assertEquals(expected, actual);
        }
    }

    private static void compareArrays(Object[] expected, Object[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            compareValue(expected[i], actual[i]);
        }
    }
}
