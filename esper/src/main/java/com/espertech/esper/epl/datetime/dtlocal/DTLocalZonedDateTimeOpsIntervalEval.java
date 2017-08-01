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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.ZonedDateTime;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDT;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDTCodegen;

public class DTLocalZonedDateTimeOpsIntervalEval extends DTLocalEvaluatorCalOpsIntervalBase {

    public DTLocalZonedDateTimeOpsIntervalEval(List<CalendarOp> calendarOps, IntervalOp intervalOp) {
        super(calendarOps, intervalOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        ZonedDateTime zdt = (ZonedDateTime) target;
        zdt = evaluateCalOpsZDT(calendarOps, zdt, eventsPerStream, isNewData, exprEvaluatorContext);
        long time = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(zdt);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZonedDateTimeOpsIntervalForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Boolean.class, DTLocalZonedDateTimeOpsIntervalEval.class).add(ZonedDateTime.class, "target").add(params).begin();
        evaluateCalOpsZDTCodegen(block, "target", forge.calendarForges, params, context);
        block.declareVar(long.class, "time", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("target")));
        String method = block.methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        ZonedDateTime start = (ZonedDateTime) startTimestamp;
        ZonedDateTime end = (ZonedDateTime) endTimestamp;
        long deltaMSec = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(end) - DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(start);
        start = evaluateCalOpsZDT(calendarOps, start, eventsPerStream, isNewData, exprEvaluatorContext);
        long startLong = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis(start);
        long endTime = startLong + deltaMSec;
        return intervalOp.evaluate(startLong, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZonedDateTimeOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Boolean.class, DTLocalZonedDateTimeOpsIntervalEval.class).add(ZonedDateTime.class, "start").add(ZonedDateTime.class, "end").add(params).begin()
                .declareVar(long.class, "startMs", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("start")))
                .declareVar(long.class, "endMs", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("end")))
                .declareVar(long.class, "deltaMSec", op(ref("endMs"), "-", ref("startMs")))
                .declareVar(ZonedDateTime.class, "result", start);
        evaluateCalOpsZDTCodegen(block, "result", forge.calendarForges, params, context);
        block.declareVar(long.class, "startLong", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("result")));
        block.declareVar(long.class, "endTime", op(ref("startLong"), "+", ref("deltaMSec")));
        String method = block.methodReturn(forge.intervalForge.codegen(ref("startLong"), ref("endTime"), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
