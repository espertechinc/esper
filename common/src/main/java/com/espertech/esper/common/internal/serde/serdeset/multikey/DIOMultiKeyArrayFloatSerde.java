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
package com.espertech.esper.common.internal.serde.serdeset.multikey;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyArrayFloat;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DIOMultiKeyArrayFloatSerde implements DataInputOutputSerde<MultiKeyArrayFloat> {
    public final static DIOMultiKeyArrayFloatSerde INSTANCE = new DIOMultiKeyArrayFloatSerde();

    public void write(MultiKeyArrayFloat mk, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(mk.getKeys(), output);
    }

    public MultiKeyArrayFloat read(DataInput input, byte[] unitKey) throws IOException {
        return new MultiKeyArrayFloat(readInternal(input));
    }

    private void writeInternal(float[] object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (float i : object) {
            output.writeFloat(i);
        }
    }

    private float[] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        float[] array = new float[len];
        for (int i = 0; i < len; i++) {
            array[i] = input.readFloat();
        }
        return array;
    }
}
