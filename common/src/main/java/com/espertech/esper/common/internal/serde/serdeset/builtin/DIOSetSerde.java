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
package com.espertech.esper.common.internal.serde.serdeset.builtin;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DIOSetSerde implements DataInputOutputSerde<Set<Object>> {
    private final DataInputOutputSerde inner;

    public DIOSetSerde(DataInputOutputSerde inner) {
        this.inner = inner;
    }

    public void write(Set<Object> set, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeInt(set.size());
        for (Object object : set) {
            inner.write(object, output, unitKey, writer);
        }
    }

    public Set<Object> read(DataInput input, byte[] unitKey) throws IOException {
        int size = input.readInt();
        HashSet<Object> set = new HashSet<>(CollectionUtil.capacityHashMap(size));
        for (int i = 0; i < size; i++) {
            set.add(inner.read(input, unitKey));
        }
        return set;
    }
}
