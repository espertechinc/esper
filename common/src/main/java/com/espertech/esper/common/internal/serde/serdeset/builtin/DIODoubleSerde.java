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
 * Binding for non-null double values.
 */
public class DIODoubleSerde implements DataInputOutputSerde<Double> {
    public final static DIODoubleSerde INSTANCE = new DIODoubleSerde();

    /**
     * Ctor.
     */
    private DIODoubleSerde() {
    }

    public void write(Double object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeDouble(object);
    }

    public void write(Double object, DataOutput stream) throws IOException {
        stream.writeDouble(object);
    }

    public Double read(DataInput s, byte[] resourceKey) throws IOException {
        return s.readDouble();
    }

    public Double read(DataInput input) throws IOException {
        return input.readDouble();
    }
}
