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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

/**
 * Represents a constant in an expressiun tree.
 */
public class ExprConstantNodeImpl extends ExprNodeBase implements ExprConstantNode, ExprEvaluator {
    private Object value;
    private final Class clazz;
    private static final long serialVersionUID = 3154169410675962539L;

    /**
     * Ctor.
     *
     * @param value is the constant's value.
     */
    public ExprConstantNodeImpl(Object value) {
        this.value = value;
        if (value == null) {
            clazz = null;
        } else {
            clazz = value.getClass();
        }
    }

    public boolean isConstantValue() {
        return true;
    }

    /**
     * Ctor.
     *
     * @param value     is the constant's value.
     * @param valueType is the constant's value type.
     */
    public ExprConstantNodeImpl(Object value, Class valueType) {
        this.value = value;
        if (value == null) {
            clazz = valueType;
        } else {
            clazz = value.getClass();
        }
    }

    /**
     * Ctor - for use when the constant should return a given type and the actual value is always null.
     *
     * @param clazz the type of the constant null.
     */
    public ExprConstantNodeImpl(Class clazz) {
        this.clazz = clazz;
        this.value = null;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public boolean isConstantResult() {
        return true;
    }

    /**
     * Returns the constant's value.
     *
     * @return value of constant
     */
    public Object getConstantValue(ExprEvaluatorContext context) {
        return value;
    }

    /**
     * Sets the value of the constant.
     *
     * @param value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    public Class getConstantType() {
        return clazz;
    }

    public Class getType() {
        return clazz;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaExprConst(value);
        }
        return value;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (value instanceof String) {
            writer.append("\"" + value + '\"');
        } else if (value == null) {
            writer.append("null");
        } else {
            writer.append(value.toString());
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprConstantNodeImpl)) {
            return false;
        }

        ExprConstantNodeImpl other = (ExprConstantNodeImpl) node;

        if ((other.value == null) && (this.value != null)) {
            return false;
        }
        if ((other.value != null) && (this.value == null)) {
            return false;
        }
        if ((other.value == null) && (this.value == null)) {
            return true;
        }
        return other.value.equals(this.value);
    }
}
