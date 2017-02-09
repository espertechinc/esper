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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * Represents an stream property identifier in a filter expressiun tree.
 */
public class ExprContextPropertyNode extends ExprNodeBase implements ExprEvaluator {
    private static final long serialVersionUID = 2816977190089087618L;
    private final String propertyName;
    private Class returnType;
    private transient EventPropertyGetter getter;

    public ExprContextPropertyNode(String propertyName) {
        this.propertyName = propertyName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (validationContext.getContextDescriptor() == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' cannot be used in the expression as provided");
        }
        EventType eventType = validationContext.getContextDescriptor().getContextPropertyRegistry().getContextEventType();
        if (eventType == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' cannot be used in the expression as provided");
        }
        getter = eventType.getGetter(propertyName);
        if (getter == null) {
            throw new ExprValidationException("Context property '" + propertyName + "' is not a known property, known properties are " + Arrays.toString(eventType.getPropertyNames()));
        }
        returnType = eventType.getPropertyType(propertyName);
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprContextProp(this);

            Object result = null;
            if (context.getContextProperties() != null) {
                result = getter.get(context.getContextProperties());
            }
            InstrumentationHelper.get().aExprContextProp(result);
            return result;
        }

        if (context.getContextProperties() != null) {
            return getter.get(context.getContextProperties());
        }
        return null;
    }

    public Class getType() {
        return returnType;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(propertyName);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public EventPropertyGetter getGetter() {
        return getter;
    }

    public boolean equalsNode(ExprNode node) {
        if (this == node) return true;
        if (node == null || getClass() != node.getClass()) return false;

        ExprContextPropertyNode that = (ExprContextPropertyNode) node;
        return propertyName.equals(that.propertyName);
    }
}
