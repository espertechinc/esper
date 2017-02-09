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
 * Placeholder for a double value in an event expression.
 */
public class DoubleValue extends PrimitiveValueBase {
    private Double doubleValue;

    /**
     * Constructor.
     */
    public DoubleValue() {
    }

    /**
     * Constructor setting the value.
     *
     * @param doubleValue value to set.
     */
    public DoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public PrimitiveValueType getType() {
        return PrimitiveValueType.DOUBLE;
    }

    /**
     * Parse string value returning a double.
     *
     * @param value to parse
     * @return parsed value
     */
    public static double parseString(String value) {
        return Double.parseDouble(value);
    }

    public final void parse(String value) {
        doubleValue = parseString(value);
    }

    /**
     * Parse the string array returning a double array.
     *
     * @param values - string array
     * @return typed array
     */
    public static double[] parseString(String[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = parseString(values[i]);
        }
        return result;
    }

    public final Object getValueObject() {
        return doubleValue;
    }

    /**
     * Return the value as an unboxed.
     *
     * @return value
     */
    public final double getDouble() {
        if (doubleValue == null) {
            throw new IllegalStateException();
        }
        return doubleValue;
    }

    public final void setDouble(double x) {
        this.doubleValue = x;
    }

    public final String toString() {
        if (doubleValue == null) {
            return "null";
        }
        return doubleValue.toString();
    }
}
