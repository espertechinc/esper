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

public class DIOPrimitiveByteArraySerde implements DataInputOutputSerde<byte[]> {
    public final static DIOPrimitiveByteArraySerde INSTANCE = new DIOPrimitiveByteArraySerde();

    private DIOPrimitiveByteArraySerde() {
    }

    public void write(byte[] object, DataOutput output) throws IOException {
        writeInternal(object, output);
    }

    public byte[] read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public void write(byte[] object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(object, output);
    }

    public byte[] read(DataInput input, byte[] unitKey) throws IOException {
        return readInternal(input);
    }

    protected static void writeInternal(byte[] object, DataOutput output) throws IOException {
        output.writeInt(object.length);
        output.write(object);
    }

    protected static byte[] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        byte[] array = new byte[len];
        input.readFully(array);
        return array;
    }
}
