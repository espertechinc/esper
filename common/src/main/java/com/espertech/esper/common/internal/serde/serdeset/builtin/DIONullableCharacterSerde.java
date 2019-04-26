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
 * Binding for nullable character values.
 */
public class DIONullableCharacterSerde implements DataInputOutputSerde<Character> {
    public final static DIONullableCharacterSerde INSTANCE = new DIONullableCharacterSerde();

    private DIONullableCharacterSerde() {
    }

    public void write(Character object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(Character object, DataOutput stream) throws IOException {
        boolean isNull = object == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            stream.writeChar(object);
        }
    }

    public Character read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public Character read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private Character readInternal(DataInput input) throws IOException {
        boolean isNull = input.readBoolean();
        if (isNull) {
            return null;
        }
        return input.readChar();
    }
}
