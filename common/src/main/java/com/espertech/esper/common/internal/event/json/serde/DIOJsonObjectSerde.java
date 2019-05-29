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
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.event.json.serde.DIOJsonSerdeHelper.readValue;
import static com.espertech.esper.common.internal.event.json.serde.DIOJsonSerdeHelper.writeValue;

public class DIOJsonObjectSerde implements DataInputOutputSerde<Map<String, Object>> {
    private final static byte NULL_TYPE = 0;
    private final static byte INT_TYPE = 1;
    private final static byte DOUBLE_TYPE = 2;
    private final static byte STRING_TYPE = 3;
    private final static byte BOOLEAN_TYPE = 4;
    private final static byte OBJECT_TYPE = 5;
    private final static byte ARRAY_TYPE = 6;

    public final static DIOJsonObjectSerde INSTANCE = new DIOJsonObjectSerde();

    private DIOJsonObjectSerde() {
    }

    public void write(Map<String, Object> object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        if (object == null) {
            output.writeBoolean(false);
            return;
        }
        output.writeBoolean(true);
        write(object, output);
    }

    public Map<String, Object> read(DataInput input, byte[] unitKey) throws IOException {
        boolean nonNull = input.readBoolean();
        return nonNull ? read(input) : null;
    }

    public void write(Map<String, Object> object, DataOutput output) throws IOException {
        output.writeInt(object.size());
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            output.writeUTF(entry.getKey());
            writeValue(entry.getValue(), output);
        }
    }

    public Map<String, Object> read(DataInput input) throws IOException {
        int size = input.readInt();
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(CollectionUtil.capacityHashMap(size));
        for (int i = 0; i < size; i++) {
            String key = input.readUTF();
            Object value = readValue(input);
            map.put(key, value);
        }
        return map;
    }
}
