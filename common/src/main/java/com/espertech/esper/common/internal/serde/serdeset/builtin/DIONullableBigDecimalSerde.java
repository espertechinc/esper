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
import java.math.BigDecimal;
import java.math.BigInteger;

public class DIONullableBigDecimalSerde implements DataInputOutputSerde<BigDecimal> {
    public final static DIONullableBigDecimalSerde INSTANCE = new DIONullableBigDecimalSerde();

    private DIONullableBigDecimalSerde() {
    }

    public void write(BigDecimal object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        write(object, output);
    }

    public void write(BigDecimal bigDecimal, DataOutput stream) throws IOException {
        boolean isNull = bigDecimal == null;
        stream.writeBoolean(isNull);
        if (!isNull) {
            stream.writeInt(bigDecimal.scale());
            DIOSerdeBigDecimalBigInteger.writeBigInt(bigDecimal.unscaledValue(), stream);
        }
    }

    public BigDecimal read(DataInput input) throws IOException {
        return readInternal(input);
    }

    public BigDecimal read(DataInput input, byte[] resourceKey) throws IOException {
        return readInternal(input);
    }

    private BigDecimal readInternal(DataInput s) throws IOException {
        boolean isNull = s.readBoolean();
        if (isNull) {
            return null;
        }
        int scale = s.readInt();
        BigInteger bigInt = DIOSerdeBigDecimalBigInteger.readBigInt(s);
        return new BigDecimal(bigInt, scale);
    }
}
