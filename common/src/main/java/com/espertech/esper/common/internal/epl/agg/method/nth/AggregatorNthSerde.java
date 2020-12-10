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
package com.espertech.esper.common.internal.epl.agg.method.nth;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.espertech.esper.common.internal.epl.agg.core.AggregationSerdeUtil.readVersionChecked;
import static com.espertech.esper.common.internal.epl.agg.core.AggregationSerdeUtil.writeVersion;

public class AggregatorNthSerde {

    protected static final short SERDE_VERSION = 1;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param output                      output
     * @param unitKey                     unit key
     * @param writer                      writer
     * @param serdeNullable               binding
     * @param circularBuffer              buffer
     * @param numDataPoints               points
     * @param currentBufferElementPointer pointer
     * @param sizeBuf                     size
     * @throws IOException io error
     */
    public static void write(DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer, DataInputOutputSerde serdeNullable, Object[] circularBuffer, long numDataPoints, int currentBufferElementPointer, int sizeBuf) throws IOException {
        writeVersion(SERDE_VERSION, output);
        output.writeBoolean(circularBuffer != null);
        if (circularBuffer != null) {
            output.writeLong(numDataPoints);
            output.writeInt(currentBufferElementPointer);
            for (int i = 0; i < sizeBuf; i++) {
                serdeNullable.write(circularBuffer[i], output, unitKey, writer);
            }
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input         input
     * @param unitKey       unit key
     * @param serdeNullable binding
     * @param sizeBuf       size
     * @return state
     * @throws IOException ioerror
     */
    public static AggregationNthState read(DataInput input, byte[] unitKey, DataInputOutputSerde serdeNullable, int sizeBuf) throws IOException {
        readVersionChecked(SERDE_VERSION, input);
        boolean filled = input.readBoolean();
        AggregationNthState state = new AggregationNthState();
        if (!filled) {
            return state;
        }
        Object[] circularBuffer = new Object[sizeBuf];
        state.setCircularBuffer(circularBuffer);
        state.setNumDataPoints(input.readLong());
        state.setCurrentBufferElementPointer(input.readInt());
        for (int i = 0; i < sizeBuf; i++) {
            circularBuffer[i] = serdeNullable.read(input, unitKey);
        }
        return state;
    }
}
