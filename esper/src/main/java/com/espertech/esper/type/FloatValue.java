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
 * Placeholder for a float value in an event expression.
 */
public final class FloatValue extends PrimitiveValueBase {
    private Float floatValue;

    public PrimitiveValueType getType() {
        return PrimitiveValueType.FLOAT;
    }

    /**
     * Parse string value returning a float.
     *
     * @param value to parse
     * @return parsed value
     */
    public static float parseString(String value) {
        return Float.parseFloat(value);
    }

    /**
     * Parse the string array returning a float array.
     *
     * @param values - string array
     * @return typed array
     */
    public static float[] parseString(String[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseString(values[i]);
        }
        return result;
    }

    public final void parse(String value) {
        floatValue = parseString(value);
    }

    public final Object getValueObject() {
        return floatValue;
    }

    public final void setFloat(float x) {
        this.floatValue = x;
    }

    public final String toString() {
        if (floatValue == null) {
            return "null";
        }
        return floatValue.toString();
    }
}
