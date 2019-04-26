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
package com.espertech.esper.common.internal.serde.serdeset.additional;

import com.espertech.esper.common.internal.collection.RefCountedSet;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

public class DIORefCountedSet implements DataInputOutputSerde<RefCountedSet<Object>> {
    private final DataInputOutputSerde inner;

    public DIORefCountedSet(DataInputOutputSerde inner) {
        this.inner = inner;
    }

    public void write(RefCountedSet<Object> valueSet, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeInt(valueSet.getRefSet().size());
        for (Map.Entry<Object, Integer> entry : valueSet.getRefSet().entrySet()) {
            inner.write(entry.getKey(), output, unitKey, writer);
            output.writeInt(entry.getValue());
        }
        output.writeInt(valueSet.getNumValues());
    }

    public RefCountedSet<Object> read(DataInput input, byte[] unitKey) throws IOException {
        RefCountedSet<Object> valueSet = new RefCountedSet<>();
        Map<Object, Integer> refSet = valueSet.getRefSet();
        int size = input.readInt();
        for (int i = 0; i < size; i++) {
            Object key = inner.read(input, unitKey);
            int ref = input.readInt();
            refSet.put(key, ref);
        }
        valueSet.setNumValues(input.readInt());
        return valueSet;
    }
}
