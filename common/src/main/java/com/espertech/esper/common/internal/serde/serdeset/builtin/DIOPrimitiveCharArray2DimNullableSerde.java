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

public class DIOPrimitiveCharArray2DimNullableSerde implements DataInputOutputSerde<char[][]> {
    public final static DIOPrimitiveCharArray2DimNullableSerde INSTANCE = new DIOPrimitiveCharArray2DimNullableSerde();

    private DIOPrimitiveCharArray2DimNullableSerde() {
    }

    public void write(char[][] object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        for (char[] i : object) {
            writeArray(i, output);
        }
    }

    public char[][] read(DataInput input, byte[] unitKey) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        char[][] array = new char[len][];
        for (int i = 0; i < len; i++) {
            array[i] = readArray(input);
        }
        return array;
    }

    private void writeArray(char[] array, DataOutput output) throws IOException {
        if (array == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(array.length);
        for (char i : array) {
            output.writeChar(i);
        }
    }

    private char[] readArray(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        char[] array = new char[len];
        for (int i = 0; i < len; i++) {
            array[i] = input.readChar();
        }
        return array;
    }
}
