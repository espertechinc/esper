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
 * Binding for non-null long values.
 */
public class DIOLongSerde implements DataInputOutputSerde<Long> {
    public final static DIOLongSerde INSTANCE = new DIOLongSerde();

    private DIOLongSerde() {
    }

    public void write(Long object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        output.writeLong(object);
    }

    public void write(Long object, DataOutput stream) throws IOException {
        stream.writeLong(object);
    }

    public Long read(DataInput s, byte[] resourceKey) throws IOException {
        return s.readLong();
    }

    public Long read(DataInput input) throws IOException {
        return input.readLong();
    }
}
