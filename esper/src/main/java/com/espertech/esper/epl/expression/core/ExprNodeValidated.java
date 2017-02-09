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
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;

import java.io.StringWriter;

/**
 * A placeholder for another expression node that has been validated already.
 */
public class ExprNodeValidated extends ExprNodeBase implements ExprEvaluator {
    private final ExprNode inner;
    private static final long serialVersionUID = 301058622892268624L;

    /**
     * Ctor.
     *
     * @param inner nested expression node
     */
    public ExprNodeValidated(ExprNode inner) {
        this.inner = inner;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprPrecedenceEnum getPrecedence() {
        return inner.getPrecedence();
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        inner.toEPL(writer, parentPrecedence);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        inner.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    public boolean isConstantResult() {
        return inner.isConstantResult();
    }

    public boolean equalsNode(ExprNode node) {
        if (node instanceof ExprNodeValidated) {
            return inner.equalsNode(((ExprNodeValidated) node).inner);
        }
        return inner.equalsNode(node);
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public void accept(ExprNodeVisitor visitor) {
        if (visitor.isVisit(this)) {
            visitor.visit(this);
            inner.accept(visitor);
        }
    }

    public Class getType() {
        return inner.getExprEvaluator().getType();
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return inner.getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
