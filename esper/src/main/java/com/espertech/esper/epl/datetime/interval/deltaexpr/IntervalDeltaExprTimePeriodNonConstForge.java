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
package com.espertech.esper.epl.datetime.interval.deltaexpr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.interval.IntervalDeltaExprEvaluator;
import com.espertech.esper.epl.datetime.interval.IntervalDeltaExprForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class IntervalDeltaExprTimePeriodNonConstForge implements IntervalDeltaExprForge, IntervalDeltaExprEvaluator {

    private final ExprTimePeriod timePeriod;
    private final TimeAbacus timeAbacus;

    public IntervalDeltaExprTimePeriodNonConstForge(ExprTimePeriod timePeriod, TimeAbacus timeAbacus) {
        this.timePeriod = timePeriod;
        this.timeAbacus = timeAbacus;
    }

    public IntervalDeltaExprEvaluator makeEvaluator() {
        return this;
    }

    public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        double sec = timePeriod.evaluateAsSeconds(eventsPerStream, isNewData, context);
        return timeAbacus.deltaForSecondsDouble(sec);
    }

    public CodegenExpression codegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(long.class, IntervalDeltaExprTimePeriodNonConstForge.class, codegenClassScope).addParam(long.class, "reference");


        methodNode.getBlock().declareVar(double.class, "sec", timePeriod.evaluateAsSecondsCodegen(methodNode, exprSymbol, codegenClassScope))
                .methodReturn(timeAbacus.deltaForSecondsDoubleCodegen(ref("sec"), codegenClassScope));
        return localMethod(methodNode, reference);
    }
}
