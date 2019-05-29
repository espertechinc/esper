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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DIOPrimitiveLongArray2DimNullableSerde implements DataInputOutputSerde<long[][]> {
    public final static DIOPrimitiveLongArray2DimNullableSerde INSTANCE = new DIOPrimitiveLongArray2DimNullableSerde();

    private DIOPrimitiveLongArray2DimNullableSerde() {
    }

    public void write(long[][] object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (long[] i : object) {
            writeArray(i, output);
        }
    }

    public long[][] read(DataInput input, byte[] unitKey) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        long[][] array = new long[len][];
        for (int i = 0; i < len; i++) {
            array[i] = readArray(input);
        }
        return array;
    }

    private void writeArray(long[] array, DataOutput output) throws IOException {
        if (array == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(array.length);
        for (long i : array) {
            output.writeLong(i);
        }
    }

    private long[] readArray(DataInput input) throws IOException {
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
