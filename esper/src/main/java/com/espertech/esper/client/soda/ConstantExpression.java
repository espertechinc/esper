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

import com.espertech.esper.util.StringValue;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Constant value returns a fixed value for use in expressions.
 */
public class ConstantExpression extends ExpressionBase {
    private Object constant;
    private String constantType;
    private static final long serialVersionUID = 1787950621647511049L;

    /**
     * Ctor.
     */
    public ConstantExpression() {
    }

    /**
     * Returns the type of the constant.
     *
     * @return type
     */
    public String getConstantType() {
        return constantType;
    }

    /**
     * Sets the type of the constant.
     *
     * @param constantType type
     */
    public void setConstantType(String constantType) {
        this.constantType = constantType;
    }

    /**
     * Ctor.
     *
     * @param constant is the constant value, or null to represent the null value
     */
    public ConstantExpression(Object constant) {
        this.constant = constant;
    }

    /**
     * Ctor.
     *
     * @param constant     value
     * @param constantType type
     */
    public ConstantExpression(Object constant, String constantType) {
        this.constant = constant;
        this.constantType = constantType;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (constant instanceof Iterable) {
            Iterable iterable = (Iterable) constant;
            Iterator<Object> iterator = iterable.iterator();
            writer.append("[");
            String delimiter = "";
            for (; iterator.hasNext(); ) {
                Object next = iterator.next();
                writer.append(delimiter);
                DataFlowOperatorParameter.renderValue(writer, next);
                delimiter = ",";
            }
            writer.append("]");
        } else if (constant instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) constant;
            writer.append("{");
            String delimiter = "";
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writer.append(delimiter);
                writer.append(entry.getKey());
                writer.append(": ");
                DataFlowOperatorParameter.renderValue(writer, entry.getValue());
                delimiter = ",";
            }
            writer.append("}");
        } else {
            StringValue.renderConstantAsEPL(writer, constant);
        }
    }

    /**
     * Returns the constant value that the expression represents.
     *
     * @return value of constant
     */
    public Object getConstant() {
        return constant;
    }

    /**
     * Sets the constant value that the expression represents.
     *
     * @param constant is the value, or null to indicate the null value
     */
    public void setConstant(Object constant) {
        this.constant = constant;
    }
}
