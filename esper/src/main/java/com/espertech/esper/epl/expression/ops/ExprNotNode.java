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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;

/**
 * Represents a NOT expression in an expression tree.
 */
public class ExprNotNode extends ExprNodeBase implements ExprEvaluator {
    private transient ExprEvaluator evaluator;
    private static final long serialVersionUID = -5958420226808323787L;

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        // Must have a single child node
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("The NOT node requires exactly 1 child node");
        }

        evaluator = this.getChildNodes()[0].getExprEvaluator();
        Class childType = evaluator.getType();
        if (!JavaClassHelper.isBoolean(childType)) {
            throw new ExprValidationException("Incorrect use of NOT clause, sub-expressions do not return boolean");
        }
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getType() {
        return Boolean.class;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprNot(this);
        }
        Boolean evaluated = (Boolean) evaluator.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (evaluated == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprNot(null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprNot(!evaluated);
        }
        return !evaluated;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("not ");
        this.getChildNodes()[0].toEPL(writer, getPrecedence());
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.NEGATED;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprNotNode)) {
            return false;
        }

        return true;
    }
}
