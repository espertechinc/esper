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
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.ZonedDateTime;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

class DTLocalZDTIntervalEval extends DTLocalEvaluatorIntervalBase {

    public DTLocalZDTIntervalEval(IntervalOp intervalOp) {
        super(intervalOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) target);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZDTIntervalForge forge, CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, DTLocalZDTIntervalEval.class).add(ZonedDateTime.class, "target").add(params).begin()
                .declareVar(long.class, "time", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("target")))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) startTimestamp);
        long end = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) endTimestamp);
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZDTIntervalForge forge, CodegenExpression start, CodegenExpression end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMethodId method = context.addMethod(Boolean.class, DTLocalZDTIntervalEval.class).add(ZonedDateTime.class, "startTimestamp").add(ZonedDateTime.class, "endTimestamp").add(params).begin()
                .declareVar(long.class, "start", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("startTimestamp")))
                .declareVar(long.class, "end", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("endTimestamp")))
                .methodReturn(forge.intervalForge.codegen(ref("start"), ref("end"), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
