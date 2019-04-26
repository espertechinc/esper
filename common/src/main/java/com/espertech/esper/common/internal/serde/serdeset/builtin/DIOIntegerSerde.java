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
 * Binding for non-null integer values.
 */
public class DIOIntegerSerde implements DataInputOutputSerde<Integer> {
    public final static DIOIntegerSerde INSTANCE = new DIOIntegerSerde();

    private DIOIntegerSerde() {
    }

    public void write(Integer object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(Integer object, DataOutput stream) throws IOException {
        stream.writeInt(object);
    }

    public Integer read(DataInput s, byte[] resourceKey) throws IOException {
        return s.readInt();
    }

    public Integer read(DataInput input) throws IOException {
        return input.readInt();
    }
}
