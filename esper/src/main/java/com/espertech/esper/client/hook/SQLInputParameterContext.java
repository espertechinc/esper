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
package com.espertech.esper.client.hook;

/**
 * For use with {@link SQLColumnTypeConversion}, context of parameter conversion.
 */
public class SQLInputParameterContext {
    private int parameterNumber;
    private Object parameterValue;

    /**
     * Ctor.
     */
    public SQLInputParameterContext() {
    }

    /**
     * Set parameter value.
     *
     * @param parameterValue to set
     */
    public void setParameterValue(Object parameterValue) {
        this.parameterValue = parameterValue;
    }

    /**
     * Set parameter number
     *
     * @param parameterNumber to set
     */
    public void setParameterNumber(int parameterNumber) {
        this.parameterNumber = parameterNumber;
    }

    /**
     * Returns the parameter number.
     *
     * @return number of parameter
     */
    public int getParameterNumber() {
        return parameterNumber;
    }

    /**
     * Returns the parameter value.
     *
     * @return parameter value
     */
    public Object getParameterValue() {
        return parameterValue;
    }
}
