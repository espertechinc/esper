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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.interval.IntervalDeltaExprEvaluator;
import com.espertech.esper.epl.datetime.interval.IntervalDeltaExprForge;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.expression.time.TimeAbacus;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
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

    public CodegenExpression codegen(CodegenExpression reference, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(long.class, IntervalDeltaExprTimePeriodNonConstForge.class).add(long.class, "reference").add(params).begin()
                .declareVar(double.class, "sec", timePeriod.evaluateAsSecondsCodegen(params, context))
                .methodReturn(timeAbacus.deltaForSecondsDoubleCodegen(ref("sec"), context));
        return localMethodBuild(method).pass(reference).passAll(params).call();
    }
}
