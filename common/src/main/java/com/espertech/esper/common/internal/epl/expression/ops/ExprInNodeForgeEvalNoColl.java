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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

/**
 * Represents the in-clause (set check) function in an expression tree.
 */
public class ExprInNodeForgeEvalNoColl implements ExprEvaluator {
    private final ExprInNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprInNodeForgeEvalNoColl(ExprInNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Boolean result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        return result;
    }

    private Boolean evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object inPropResult = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        boolean isNotIn = forge.getForgeRenderable().isNotIn();
        if (forge.isMustCoerce() && (inPropResult != null)) {
            inPropResult = forge.getCoercer().coerceBoxed((Number) inPropResult);
        }

        int len = evaluators.length - 1;
        if ((len > 0) && (inPropResult == null)) {
            return null;
        }
        boolean hasNullRow = false;
        for (int i = 1; i <= len; i++) {
            Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (rightResult == null) {
                hasNullRow = true;
                continue;
            }

            if (!forge.isMustCoerce()) {
                if (rightResult.equals(inPropResult)) {
                    return !isNotIn;
                }
            } else {
                Number right = forge.getCoercer().coerceBoxed((Number) rightResult);
                if (right.equals(inPropResult)) {
                    return !isNotIn;
                }
            }
        }

        if (hasNullRow) {
            return null;
        }
        return isNotIn;
    }

}
