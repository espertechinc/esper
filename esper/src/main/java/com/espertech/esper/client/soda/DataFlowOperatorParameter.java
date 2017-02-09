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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Object model of a data flow operator parameter.
 */
public class DataFlowOperatorParameter implements Serializable {

    private static final long serialVersionUID = 6902224639315413025L;

    private String parameterName;
    private Object parameterValue;

    /**
     * Ctor.
     *
     * @param parameterName  parameter name
     * @param parameterValue parameter value
     */
    public DataFlowOperatorParameter(String parameterName, Object parameterValue) {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    /**
     * Ctor.
     */
    public DataFlowOperatorParameter() {
    }

    /**
     * Get the parameter name.
     *
     * @return parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Set the parameter name.
     *
     * @param parameterName parameter name
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    /**
     * Get the parameter value, which can be either a constant, an {@link Expression} or a JSON object
     * or a {@link EPStatementObjectModel}.
     *
     * @return parameter value
     */
    public Object getParameterValue() {
        return parameterValue;
    }

    /**
     * Set the parameter value, which can be either a constant, an {@link Expression} or a JSON object
     * or a {@link EPStatementObjectModel}.
     *
     * @param parameterValue to set
     */
    public void setParameterValue(Object parameterValue) {
        this.parameterValue = parameterValue;
    }

    /**
     * Render parameter.
     *
     * @param writer to write to
     */
    public void toEpl(StringWriter writer) {
        writer.write(parameterName);
        writer.write(": ");
        renderValue(writer, parameterValue);
    }

    /**
     * Render prameter.
     *
     * @param writer         to render to
     * @param parameterValue value
     */
    public static void renderValue(StringWriter writer, Object parameterValue) {
        if (parameterValue instanceof EPStatementObjectModel) {
            writer.write("(");
            ((EPStatementObjectModel) parameterValue).toEPL(writer);
            writer.write(")");
        } else if (parameterValue instanceof Expression) {
            ((Expression) parameterValue).toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        } else if (parameterValue == null) {
            writer.write("null");
        } else if (parameterValue instanceof String) {
            writer.write("\"");
            writer.write(parameterValue.toString());
            writer.write("\"");
        } else {
            writer.write(parameterValue.toString());
        }
    }
}
