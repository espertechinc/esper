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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;

public class ExprOptionalConstantForge {
    public final static IntervalDeltaExprMaxForge MAXFORGE = new IntervalDeltaExprMaxForge();
    public final static IntervalDeltaExprMaxEval MAXEVAL = new IntervalDeltaExprMaxEval();

    private final IntervalDeltaExprForge forge;
    private final Long optionalConstant;

    public ExprOptionalConstantForge(IntervalDeltaExprForge forge, Long optionalConstant) {
        this.forge = forge;
        this.optionalConstant = optionalConstant;
    }

    public Long getOptionalConstant() {
        return optionalConstant;
    }

    public IntervalDeltaExprForge getForge() {
        return forge;
    }

    public static ExprOptionalConstantForge make(long maxValue) {
        return new ExprOptionalConstantForge(MAXFORGE, maxValue);
    }

    public ExprOptionalConstantEval makeEval() {
        return new ExprOptionalConstantEval(forge.makeEvaluator(), optionalConstant);
    }

    public static class IntervalDeltaExprMaxForge implements IntervalDeltaExprForge {

        public IntervalDeltaExprEvaluator makeEvaluator() {
            return MAXEVAL;
        }

        public CodegenExpression codegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return constant(Long.MAX_VALUE);
        }
    }

    public static class IntervalDeltaExprMaxEval implements IntervalDeltaExprEvaluator {
        public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            return Long.MAX_VALUE;
        }
    }
}
