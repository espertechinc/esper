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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DIOBigDecimalBigIntegerUtil {

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return big dec
     * @throws IOException io error
     */
    public static BigDecimal readBigDec(DataInput input) throws IOException {
        int scale = input.readInt();
        BigInteger bigInt = readBigInt(input);
        return new BigDecimal(bigInt, scale);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param bigDecimal value
     * @param output     output
     * @throws IOException io error
     */
    public static void writeBigDec(BigDecimal bigDecimal, DataOutput output) throws IOException {
        output.writeInt(bigDecimal.scale());
        writeBigInt(bigDecimal.unscaledValue(), output);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param bigInteger value
     * @param stream     output
     * @throws IOException io error
     */
    public static void writeBigInt(BigInteger bigInteger, DataOutput stream) throws IOException {
        byte[] a = bigInteger.toByteArray();
        if (a.length > Short.MAX_VALUE) {
            throw new IllegalArgumentException("BigInteger byte array is larger than 0x7fff bytes");
        }
        int firstByte = a[0];
        stream.writeShort((firstByte < 0) ? (-a.length) : a.length);
        stream.writeByte(firstByte);
        stream.write(a, 1, a.length - 1);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param input input
     * @return big int
     * @throws IOException io error
     */
    public static BigInteger readBigInt(DataInput input) throws IOException {
        int len = input.readShort();
        if (len < 0) {
            len = -len;
        }
        byte[] a = new byte[len];
        a[0] = input.readByte();
        input.readFully(a, 1, a.length - 1);
        return new BigInteger(a);
    }
}
