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
package com.espertech.esper.epl.datetime.dtlocal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Date;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

class DTLocalDateIntervalEval extends DTLocalEvaluatorIntervalBase {
    public DTLocalDateIntervalEval(IntervalOp intervalOp) {
        super(intervalOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = ((Date) target).getTime();
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalDateIntervalForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, DTLocalDateIntervalEval.class).add(Date.class, "target").add(params).begin()
                .declareVar(long.class, "time", exprDotMethod(ref("target"), "getTime"))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = ((Date) startTimestamp).getTime();
        long end = ((Date) endTimestamp).getTime();
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalDateIntervalForge forge, CodegenExpression start, CodegenExpression end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, DTLocalDateIntervalEval.class).add(Date.class, "start").add(Date.class, "end").add(params).begin()
                .methodReturn(forge.intervalForge.codegen(exprDotMethod(ref("start"), "getTime"), exprDotMethod(ref("end"), "getTime"), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
