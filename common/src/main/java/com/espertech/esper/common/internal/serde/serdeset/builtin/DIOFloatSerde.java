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
 * Binding for non-null float values.
 */
public class DIOFloatSerde implements DataInputOutputSerde<Float> {
    public final static DIOFloatSerde INSTANCE = new DIOFloatSerde();

    private DIOFloatSerde() {
    }

    public void write(Float object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeFloat(object);
    }

    public void write(Float object, DataOutput stream) throws IOException {
        stream.writeFloat(object);
    }

    public Float read(DataInput input) throws IOException {
        return input.readFloat();
    }

    public Float read(DataInput input, byte[] resourceKey) throws IOException {
        return input.readFloat();
    }
}
