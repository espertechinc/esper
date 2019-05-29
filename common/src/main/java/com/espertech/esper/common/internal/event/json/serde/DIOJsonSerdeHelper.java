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

import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DIOJsonSerdeHelper {
    private final static byte NULL_TYPE = 0;
    private final static byte INT_TYPE = 1;
    private final static byte DOUBLE_TYPE = 2;
    private final static byte STRING_TYPE = 3;
    private final static byte BOOLEAN_TYPE = 4;
    private final static byte OBJECT_TYPE = 5;
    private final static byte ARRAY_TYPE = 6;
    
    static void write(Map<String, Object> object, DataOutput output) throws IOException {
        output.writeInt(object.size());
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            output.writeUTF(entry.getKey());
            writeValue(entry.getValue(), output);
        }
    }

    public static Map<String, Object> read(DataInput input) throws IOException {
        int size = input.readInt();
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(CollectionUtil.capacityHashMap(size));
        for (int i = 0; i < size; i++) {
            String key = input.readUTF();
            Object value = readValue(input);
            map.put(key, value);
        }
        return map;
    }

    static void writeValue(Object value, DataOutput output) throws IOException {
        if (value == null) {
            output.write(NULL_TYPE);
        } else if (value instanceof Integer) {
            output.write(INT_TYPE);
            output.writeInt((int) value);
        } else if (value instanceof Double) {
            output.write(DOUBLE_TYPE);
            output.writeDouble((double) value);
        } else if (value instanceof String) {
            output.write(STRING_TYPE);
            output.writeUTF((String) value);
        } else if (value instanceof Boolean) {
            output.write(BOOLEAN_TYPE);
            output.writeBoolean((boolean) value);
        } else if (value instanceof Map) {
            output.write(OBJECT_TYPE);
            write((Map<String, Object>) value, output);
        } else if (value instanceof Object[]) {
            output.write(ARRAY_TYPE);
            writeArray((Object[]) value, output);
        } else {
            throw new IOException("Unrecognized json object type value of type " + value.getClass() + "'");
        }
    }

    static Object readValue(DataInput input) throws IOException {
        int type = input.readByte();
        if (type == NULL_TYPE) {
            return null;
        } else if (type == INT_TYPE) {
            return input.readInt();
        } else if (type == DOUBLE_TYPE) {
            return input.readDouble();
        } else if (type == STRING_TYPE) {
            return input.readUTF();
        } else if (type == BOOLEAN_TYPE) {
            return input.readBoolean();
        } else if (type == OBJECT_TYPE) {
            return read(input);
        } else if (type == ARRAY_TYPE) {
            return readArray(input);
        } else {
            throw new IOException("Unrecognized json object type value of type " + type + "'");
        }
    }

    static void writeArray(Object[] value, DataOutput output) throws IOException {
        output.writeInt(value.length);
        for (Object o : value) {
            writeValue(o, output);
        }
    }

    static Object[] readArray(DataInput input) throws IOException {
        int size = input.readInt();
        Object[] result = new Object[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = readValue(input);
        }
        return result;
    }
}
