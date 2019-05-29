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

public class DIOBoxedCharacterArray2DimNullableSerde implements DataInputOutputSerde<Character[][]> {
    public final static DIOBoxedCharacterArray2DimNullableSerde INSTANCE = new DIOBoxedCharacterArray2DimNullableSerde();

    private DIOBoxedCharacterArray2DimNullableSerde() {
    }

    public void write(Character[][] object, DataOutput output) throws IOException {
        writeInternal(object, output);
    }

    public Character[][] read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public void write(Character[][] object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(object, output);
    }

    public Character[][] read(DataInput input, byte[] unitKey) throws IOException {
        return readInternal(input);
    }

    private void writeInternal(Character[][] object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (Character[] i : object) {
            DIOBoxedCharacterArrayNullableSerde.INSTANCE.write(i, output);
        }
    }

    private Character[][] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        Character[][] array = new Character[len][];
        for (int i = 0; i < len; i++) {
            array[i] = DIOBoxedCharacterArrayNullableSerde.INSTANCE.read(input);
        }
        return array;
    }
}
