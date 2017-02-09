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
 * Represents an And-condition.
 */
public class ExprAndNodeImpl extends ExprNodeBase implements ExprEvaluator, ExprAndNode {
    private static final long serialVersionUID = 8105121208330622813L;

    private transient ExprEvaluator[] evaluators;

    public ExprAndNodeImpl() {
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        evaluators = ExprNodeUtility.getEvaluators(this.getChildNodes());

        // Sub-nodes must be returning boolean
        for (ExprEvaluator child : evaluators) {
            Class childType = child.getType();
            if (!JavaClassHelper.isBoolean(childType)) {
                throw new ExprValidationException("Incorrect use of AND clause, sub-expressions do not return boolean");
            }
        }

        if (this.getChildNodes().length <= 1) {
            throw new ExprValidationException("The AND operator requires at least 2 child expressions");
        }
        return null;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return Boolean.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprAnd(this);
        }
        Boolean result = true;
        for (ExprEvaluator child : evaluators) {
            Boolean evaluated = (Boolean) child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (evaluated == null) {
                result = null;
            } else {
                if (!evaluated) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aExprAnd(false);
                    }
                    return false;
                }
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprAnd(result);
        }
        return result;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        String appendStr = "";
        for (ExprNode child : this.getChildNodes()) {
            writer.append(appendStr);
            child.toEPL(writer, getPrecedence());
            appendStr = " and ";
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.AND;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprAndNodeImpl)) {
            return false;
        }

        return true;
    }
}
