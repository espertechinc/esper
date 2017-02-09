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
 * Abstract class for literal values supplied in an event expression string and prepared expression values supplied
 * by set methods.
 */
public abstract class PrimitiveValueBase implements PrimitiveValue {
    public void parse(String[] values) {
        parse(values[0]);
    }

    public void setBoolean(boolean x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setByte(byte x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setDouble(double x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setFloat(float x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setInt(int x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setLong(long x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setShort(short x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void setString(String x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
