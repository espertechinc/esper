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
import com.espertech.esper.type.WildcardParameter;

import java.io.StringWriter;

/**
 * Expression for use within crontab to specify a wildcard.
 */
public class ExprWildcardImpl extends ExprNodeBase implements ExprEvaluator, ExprWildcard {
    private static final WildcardParameter WILDCARD_PARAMETER = new WildcardParameter();
    private static final long serialVersionUID = -6098833102154556698L;

    public ExprWildcardImpl() {
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("*");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public boolean isConstantResult() {
        return true;
    }

    public boolean equalsNode(ExprNode node) {
        return node instanceof ExprWildcardImpl;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public Class getType() {
        return WildcardParameter.class;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        return WILDCARD_PARAMETER;
    }
}
