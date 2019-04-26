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
package com.espertech.esper.common.internal.serde.serdeset.multikey;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyArrayLong;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DIOMultiKeyArrayLongSerde implements DataInputOutputSerde<MultiKeyArrayLong> {
    public final static DIOMultiKeyArrayLongSerde INSTANCE = new DIOMultiKeyArrayLongSerde();

    public void write(MultiKeyArrayLong mk, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(mk.getKeys(), output);
    }

    public MultiKeyArrayLong read(DataInput input, byte[] unitKey) throws IOException {
        return new MultiKeyArrayLong(readInternal(input));
    }

    private void writeInternal(long[] object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (long i : object) {
            output.writeLong(i);
        }
    }

    private long[] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        long[] array = new long[len];
        for (int i = 0; i < len; i++) {
            array[i] = input.readLong();
        }
        return array;
    }
}
