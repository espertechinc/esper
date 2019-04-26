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
 * Binding for nullable long values.
 */
public class DIONullableLongSerde implements DataInputOutputSerde<Long> {
    public final static DIONullableLongSerde INSTANCE = new DIONullableLongSerde();

    private DIONullableLongSerde() {
    }

    public void write(Long object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(Long object, DataOutput stream) throws IOException {
        boolean isNull = object == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            stream.writeLong(object);
        }
    }

    public Long read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public Long read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private Long readInternal(DataInput s) throws IOException {
        boolean isNull = s.readBoolean();
        if (isNull) {
            return null;
        }
        return s.readLong();
    }
}
