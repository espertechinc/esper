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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;

public class IntervalDeltaExprTimePeriodConstForge implements IntervalDeltaExprForge, IntervalDeltaExprEvaluator {

    private final ExprTimePeriodEvalDeltaConst timerPeriodConst;

    public IntervalDeltaExprTimePeriodConstForge(ExprTimePeriodEvalDeltaConst timerPeriodConst) {
        this.timerPeriodConst = timerPeriodConst;
    }

    public IntervalDeltaExprEvaluator makeEvaluator() {
        return this;
    }

    public long evaluate(long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return timerPeriodConst.deltaAdd(reference);
    }

    public CodegenExpression codegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return timerPeriodConst.deltaAddCodegen(reference, codegenMethodScope, codegenClassScope);
    }
}
