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

/**
 * Binding for nullable byte values.
 */
public class DIONullableByteSerde implements DataInputOutputSerde<Byte> {
    public final static DIONullableByteSerde INSTANCE = new DIONullableByteSerde();

    private DIONullableByteSerde() {
    }

    public void write(Byte object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(Byte object, DataOutput stream) throws IOException {
        boolean isNull = object == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            stream.writeByte(object);
        }
    }

    public Byte read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public Byte read(DataInput s, byte[] resourceKey) throws IOException {
        return readInternal(s);
    }

    private Byte readInternal(DataInput input) throws IOException {
        boolean isNull = input.readBoolean();
        if (isNull) {
            return null;
        }
        return input.readByte();
    }
}
