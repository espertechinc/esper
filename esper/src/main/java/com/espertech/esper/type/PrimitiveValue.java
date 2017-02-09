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
 * Classes implementing this interface are responsible for parsing, setting and getting the value of
 * the different basic Java data types that occur in an event expression.
 * Placeholders represent all literal values in event expressions and set values in prepared event expressions.
 */
public interface PrimitiveValue {
    /**
     * Returns a value object.
     *
     * @return value object
     */
    public Object getValueObject();

    /**
     * Parse the string literal value into the specific data type.
     *
     * @param value is the textual value to parse
     */
    public void parse(String value);

    /**
     * Parse the string literal values supplied in the array into the specific data type.
     *
     * @param values are the textual values to parse
     */
    public void parse(String[] values);

    /**
     * Returns the type of primitive value this instance represents.
     *
     * @return enum type of primitive
     */
    public PrimitiveValueType getType();

    /**
     * Set a boolean value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setBoolean(boolean x) throws UnsupportedOperationException;

    /**
     * Set a byte value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setByte(byte x) throws UnsupportedOperationException;

    /**
     * Set a double value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setDouble(double x) throws UnsupportedOperationException;

    /**
     * Set a float value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setFloat(float x) throws UnsupportedOperationException;

    /**
     * Set an int value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setInt(int x) throws UnsupportedOperationException;

    /**
     * Set a long value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setLong(long x) throws UnsupportedOperationException;

    /**
     * Set a short value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setShort(short x) throws UnsupportedOperationException;

    /**
     * Set a string value.
     *
     * @param x is the value to set
     * @throws UnsupportedOperationException to indicate that the value cannot convert from boolean
     */
    public void setString(String x) throws UnsupportedOperationException;
}
