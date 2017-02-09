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
 * Placeholder for an integer value in an event expression.
 */
public final class IntValue extends PrimitiveValueBase {
    private Integer intValue;

    /**
     * Constructor.
     */
    public IntValue() {
    }

    /**
     * Constructor.
     *
     * @param intValue is the value to set to
     */
    public IntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public PrimitiveValueType getType() {
        return PrimitiveValueType.INTEGER;
    }

    /**
     * Parse the string array returning a int array.
     *
     * @param values - string array
     * @return typed array
     */
    public static int[] parseString(String[] values) {
        int[] result = new int[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseString(values[i]);
        }
        return result;
    }

    /**
     * Parse string value returning a int.
     *
     * @param value to parse
     * @return parsed value
     */
    public static int parseString(String value) {
        return Integer.parseInt(value);
    }

    public final void parse(String value) {
        intValue = Integer.parseInt(value);
    }

    public final Object getValueObject() {
        return intValue;
    }

    public final void setInt(int x) {
        this.intValue = x;
    }

    public final String toString() {
        if (intValue == null) {
            return "null";
        }
        return intValue.toString();
    }
}
