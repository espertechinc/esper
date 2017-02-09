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
package com.espertech.esper.type;

/**
 * Placeholder for a byte value in an event expression.
 */
public final class ByteValue extends PrimitiveValueBase {
    private Byte byteValue;

    public PrimitiveValueType getType() {
        return PrimitiveValueType.BYTE;
    }

    /**
     * Parses a string value as a byte.
     *
     * @param value to parse
     * @return byte value
     */
    public static byte parseString(String value) {
        return Byte.decode(value);
    }

    public final void parse(String value) {
        byteValue = Byte.parseByte(value);
    }

    public final Object getValueObject() {
        return byteValue;
    }

    public final void setByte(byte x) {
        this.byteValue = x;
    }

    public final String toString() {
        if (byteValue == null) {
            return "null";
        }
        return byteValue.toString();
    }
}
