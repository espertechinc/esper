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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprOptionalConstantEval {
    public final static IntervalDeltaExprEvaluatorMax MAXEVAL = new IntervalDeltaExprEvaluatorMax();

    private final IntervalDeltaExprEvaluator evaluator;
    private final Long optionalConstant;

    public ExprOptionalConstantEval(IntervalDeltaExprEvaluator evaluator, Long optionalConstant) {
        this.evaluator = evaluator;
        this.optionalConstant = optionalConstant;
    }

    public Long getOptionalConstant() {
        return optionalConstant;
    }

    public IntervalDeltaExprEvaluator getEvaluator() {
        return evaluator;
    }

    public static ExprOptionalConstantEval make(long maxValue) {
        return new ExprOptionalConstantEval(MAXEVAL, maxValue);
    }

    public static class IntervalDeltaExprEvaluatorMax implements IntervalDeltaExprEvaluator {
        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return Long.MAX_VALUE;
        }
    }
}
