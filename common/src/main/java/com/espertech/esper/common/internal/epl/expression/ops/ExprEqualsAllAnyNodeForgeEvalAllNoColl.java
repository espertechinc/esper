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

public class ExprEqualsAllAnyNodeForgeEvalAllNoColl implements ExprEvaluator {
    private final ExprEqualsAllAnyNodeForge forge;
    private final ExprEvaluator[] evaluators;

    public ExprEqualsAllAnyNodeForgeEvalAllNoColl(ExprEqualsAllAnyNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object result = evaluateInternal(eventsPerStream, isNewData, exprEvaluatorContext);
        return result;
    }

    private Object evaluateInternal(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {

        Object leftResult = evaluators[0].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

        // coerce early if testing without collections
        if (forge.isMustCoerce() && (leftResult != null)) {
            leftResult = forge.getCoercer().coerceBoxed((Number) leftResult);
        }

        return compareAll(forge.getForgeRenderable().isNot(), leftResult, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    private Object compareAll(boolean isNot, Object leftResult, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        boolean equalsMeansContinue = !isNot;
        int len = evaluators.length - 1;
        if ((len > 0) && (leftResult == null)) {
            return null;
        }
        boolean hasNonNullRow = false;
        boolean hasNullRow = false;
        for (int i = 1; i <= len; i++) {
            Object rightResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (rightResult != null) {
                hasNonNullRow = true;
                if (!forge.isMustCoerce()) {
                    if (equalsMeansContinue ^ leftResult.equals(rightResult)) {
                        return false;
                    }
                } else {
                    Number right = forge.getCoercer().coerceBoxed((Number) rightResult);
                    if (equalsMeansContinue ^ leftResult.equals(right)) {
                        return false;
                    }
                }
            } else {
                hasNullRow = true;
            }
        }

        if (!hasNonNullRow || hasNullRow) {
            return null;
        }
        return true;
    }

}
