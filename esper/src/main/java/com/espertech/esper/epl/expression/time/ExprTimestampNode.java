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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

/**
 * Represents the CURRENT_TIMESTAMP() function or reserved keyword in an expression tree.
 */
public class ExprTimestampNode extends ExprNodeBase implements ExprEvaluator {
    private static final long serialVersionUID = -6332243334897136751L;

    /**
     * Ctor.
     */
    public ExprTimestampNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 0) {
            throw new ExprValidationException("current_timestamp function node cannot have a child node");
        }
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return Long.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            long value = exprEvaluatorContext.getTimeProvider().getTime();
            InstrumentationHelper.get().qaExprTimestamp(this, value);
            return value;
        }
        return exprEvaluatorContext.getTimeProvider().getTime();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("current_timestamp()");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprTimestampNode)) {
            return false;
        }
        return true;
    }
}
