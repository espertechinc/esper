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
 * Binding for (nullable) String-typed values.
 */
public class DIOCharSequenceSerde implements DataInputOutputSerde<CharSequence> {
    public final static DIOCharSequenceSerde INSTANCE = new DIOCharSequenceSerde();

    private DIOCharSequenceSerde() {
    }

    public void write(CharSequence object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(CharSequence object, DataOutput stream) throws IOException {
        if (object != null) {
            stream.writeBoolean(true);
            stream.writeUTF(object.toString());
        } else {
            stream.writeBoolean(false);
        }
    }

    public CharSequence read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public CharSequence read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private CharSequence readInternal(DataInput input) throws IOException {
        if (input.readBoolean()) {
            return input.readUTF();
        }
        return null;
    }
}
