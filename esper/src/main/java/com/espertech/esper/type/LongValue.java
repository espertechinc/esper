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
 * Placeholder for a long-typed value in an event expression.
 */
public final class LongValue extends PrimitiveValueBase {
    private Long longValue;

    public PrimitiveValueType getType() {
        return PrimitiveValueType.LONG;
    }

    public final void parse(String value) {
        longValue = parseString(value);
    }

    /**
     * Parse the string containing a long value.
     *
     * @param value is the textual long value
     * @return long value
     */
    public static long parseString(String value) {
        if ((value.endsWith("L")) || ((value.endsWith("l")))) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        return Long.parseLong(value);
    }

    /**
     * Parse the string array returning a long array.
     *
     * @param values - string array
     * @return typed array
     */
    public static long[] parseString(String[] values) {
        long[] result = new long[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseString(values[i]);
        }
        return result;
    }

    public final Object getValueObject() {
        return longValue;
    }

    public final void setLong(long x) {
        this.longValue = x;
    }

    /**
     * Returns the long value.
     *
     * @return long value
     */
    public final long getLong() {
        if (longValue == null) {
            throw new IllegalStateException();
        }
        return longValue;
    }

    public final String toString() {
        if (longValue == null) {
            return "null";
        }
        return longValue.toString();
    }
}
