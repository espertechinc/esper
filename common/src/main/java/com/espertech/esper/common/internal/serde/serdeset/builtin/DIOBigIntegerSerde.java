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
import java.math.BigInteger;

/**
 * Binding for nullable boolean values.
 */
public class DIOBigIntegerSerde implements DataInputOutputSerde<BigInteger> {
    public final static DIOBigIntegerSerde INSTANCE = new DIOBigIntegerSerde();

    private DIOBigIntegerSerde() {
    }

    public void write(BigInteger object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(BigInteger bigInteger, DataOutput stream) throws IOException {
        boolean isNull = bigInteger == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            DIOBigDecimalBigIntegerUtil.writeBigInt(bigInteger, stream);
        }
    }

    public BigInteger read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public BigInteger read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private BigInteger readInternal(DataInput input) throws IOException {
        boolean isNull = input.readBoolean();
        if (isNull) {
            return null;
        }
        return DIOBigDecimalBigIntegerUtil.readBigInt(input);
    }
}
