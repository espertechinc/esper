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
 * Binding for non-null boolean values.
 */
public class DIOBooleanSerde implements DataInputOutputSerde<Boolean> {
    public final static DIOBooleanSerde INSTANCE = new DIOBooleanSerde();

    private DIOBooleanSerde() {
    }

    public void write(Boolean object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeBoolean(object);
    }

    public void write(Boolean object, DataOutput stream) throws IOException {
        stream.writeBoolean(object);
    }

    public Boolean read(DataInput s, byte[] resourceKey) throws IOException {
        return s.readBoolean();
    }

    public Boolean read(DataInput input) throws IOException {
        return input.readBoolean();
    }
}
