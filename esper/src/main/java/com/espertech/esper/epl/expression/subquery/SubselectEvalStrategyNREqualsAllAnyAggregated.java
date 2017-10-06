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
package com.espertech.esper.epl.expression.subquery;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.SimpleNumberCoercer;

import java.util.Collection;

/**
 * Strategy for subselects with "=/!=/&gt;&lt; ALL".
 */
public class SubselectEvalStrategyNREqualsAllAnyAggregated extends SubselectEvalStrategyNREqualsBase {
    private final ExprEvaluator havingEval;

    public SubselectEvalStrategyNREqualsAllAnyAggregated(ExprEvaluator valueEval, ExprEvaluator selectEval, boolean resultWhenNoMatchingEvents, boolean notIn, SimpleNumberCoercer coercer, ExprEvaluator havingEval) {
        super(valueEval, selectEval, resultWhenNoMatchingEvents, notIn, coercer);
        this.havingEval = havingEval;
    }

    protected Object evaluateInternal(Object leftResult, EventBean[] events, boolean isNewData, Collection<EventBean> matchingEvents, ExprEvaluatorContext exprEvaluatorContext, AggregationService aggregationService) {

        if (havingEval != null) {
            Boolean pass = (Boolean) havingEval.evaluate(events, true, exprEvaluatorContext);
            if ((pass == null) || (!pass)) {
                return null;
            }
        }

        Object rightResult = selectEval.evaluate(events, true, exprEvaluatorContext);
        if (rightResult == null) {
            return null;
        }

        if (coercer == null) {
            boolean eq = leftResult.equals(rightResult);
            if ((isNot && eq) || (!isNot && !eq)) {
                return false;
            }
        } else {
            Number left = coercer.coerceBoxed((Number) leftResult);
            Number right = coercer.coerceBoxed((Number) rightResult);
            boolean eq = left.equals(right);
            if ((isNot && eq) || (!isNot && !eq)) {
                return false;
            }
        }
        return true;
    }
}
