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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CoercionException;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents the COALESCE(a,b,...) function is an expression tree.
 */
public class ExprCoalesceNode extends ExprNodeBase implements ExprEvaluator {
    private Class resultType;
    private boolean[] isNumericCoercion;

    private transient ExprEvaluator[] evaluators;

    private static final long serialVersionUID = -8276568753875819730L;

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length < 2) {
            throw new ExprValidationException("Coalesce node must have at least 2 parameters");
        }
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // get child expression types
        Class[] childTypes = new Class[getChildNodes().length];
        for (int i = 0; i < evaluators.length; i++) {
            childTypes[i] = evaluators[i].getType();
        }

        // determine coercion type
        try {
            resultType = JavaClassHelper.getCommonCoercionType(childTypes);
        } catch (CoercionException ex) {
            throw new ExprValidationException("Implicit conversion not allowed: " + ex.getMessage());
        }

        // determine which child nodes need numeric coercion
        isNumericCoercion = new boolean[getChildNodes().length];
        for (int i = 0; i < evaluators.length; i++) {
            if ((JavaClassHelper.getBoxedType(evaluators[i].getType()) != resultType) &&
                    (evaluators[i].getType() != null) && (resultType != null)) {
                if (!JavaClassHelper.isNumeric(resultType)) {
                    throw new ExprValidationException("Implicit conversion from datatype '" +
                            resultType.getSimpleName() +
                            "' to " + evaluators[i].getType() + " is not allowed");
                }
                isNumericCoercion[i] = true;
            }
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return resultType;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCoalesce(this);
        }
        Object value;

        // Look for the first non-null return value
        for (int i = 0; i < evaluators.length; i++) {
            value = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (value != null) {
                // Check if we need to coerce
                if (isNumericCoercion[i]) {
                    value = JavaClassHelper.coerceBoxed((Number) value, resultType);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprCoalesce(value);
                }
                return value;
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCoalesce(null);
        }
        return null;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprNodeUtility.toExpressionStringWFunctionName("coalesce", this.getChildNodes(), writer);
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprCoalesceNode)) {
            return false;
        }

        return true;
    }
}
