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
import com.espertech.esper.client.hook.EPLExpressionEvaluationContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

/**
 * Represents the "current_evaluation_context" function in an expression tree.
 */
public class ExprCurrentEvaluationContextNode extends ExprNodeBase implements ExprEvaluator {
    private static final long serialVersionUID = -7345152240852371730L;

    /**
     * Ctor.
     */
    public ExprCurrentEvaluationContextNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 0) {
            throw new ExprValidationException("current_evaluation_context function node cannot have a child node");
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return EPLExpressionEvaluationContext.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EPLExpressionEvaluationContext ctx = new EPLExpressionEvaluationContext(exprEvaluatorContext.getStatementName(), exprEvaluatorContext.getAgentInstanceId(), exprEvaluatorContext.getEngineURI(), exprEvaluatorContext.getStatementUserObject());
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qaExprConst(ctx);
        }
        return ctx;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("current_evaluation_context()");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprCurrentEvaluationContextNode)) {
            return false;
        }
        return true;
    }
}
