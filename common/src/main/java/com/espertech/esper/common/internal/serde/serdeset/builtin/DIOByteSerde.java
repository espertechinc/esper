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
 * Binding for non-null byte values.
 */
public class DIOByteSerde implements DataInputOutputSerde<Byte> {
    public final static DIOByteSerde INSTANCE = new DIOByteSerde();

    private DIOByteSerde() {
    }

    public void write(Byte object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeByte(object);
    }

    public void write(Byte object, DataOutput stream) throws IOException {
        stream.writeByte(object);
    }

    public Byte read(DataInput s, byte[] resourceKey) throws IOException {
        return s.readByte();
    }

    public Byte read(DataInput input) throws IOException {
        return input.readByte();
    }
}
