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
 * Binding for nullable boolean values.
 */
public class DIONullableBooleanSerde implements DataInputOutputSerde<Boolean> {
    public final static DIONullableBooleanSerde INSTANCE = new DIONullableBooleanSerde();

    private DIONullableBooleanSerde() {
    }

    public void write(Boolean object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(Boolean b, DataOutput stream) throws IOException {
        boolean isNull = b == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            stream.writeBoolean(b);
        }
    }

    public Boolean read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public Boolean read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private Boolean readInternal(DataInput input) throws IOException {
        boolean isNull = input.readBoolean();
        if (isNull) {
            return null;
        }
        return input.readBoolean();
    }
}
