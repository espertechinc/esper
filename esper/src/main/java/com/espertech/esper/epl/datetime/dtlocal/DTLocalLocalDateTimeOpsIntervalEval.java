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
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsLDTCodegen;

public class DTLocalLocalDateTimeOpsIntervalEval extends DTLocalEvaluatorCalOpsIntervalBase {

    private final TimeZone timeZone;

    public DTLocalLocalDateTimeOpsIntervalEval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
        super(calendarOps, intervalOp);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        LocalDateTime ldt = (LocalDateTime) target;
        ldt = DTLocalUtil.evaluateCalOpsLDT(calendarOps, ldt, eventsPerStream, isNewData, exprEvaluatorContext);
        long time = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(ldt, timeZone);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLocalDateTimeOpsIntervalForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenBlock block = context.addMethod(Boolean.class, DTLocalCalOpsIntervalEval.class).add(LocalDateTime.class, "target").add(params).begin();
        evaluateCalOpsLDTCodegen(block, "target", forge.calendarForges, params, context);
        block.declareVar(long.class, "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("target"), ref(tz.getMemberName())));
        String method = block.methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), params, context));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        LocalDateTime start = (LocalDateTime) startTimestamp;
        LocalDateTime end = (LocalDateTime) endTimestamp;
        long deltaMSec = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(end, timeZone) - DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(start, timeZone);
        LocalDateTime result = DTLocalUtil.evaluateCalOpsLDT(calendarOps, start, eventsPerStream, isNewData, exprEvaluatorContext);
        long startLong = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone(result, timeZone);
        long endTime = startLong + deltaMSec;
        return intervalOp.evaluate(startLong, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLocalDateTimeOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenMember tz = context.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenBlock block = context.addMethod(Boolean.class, DTLocalCalOpsIntervalEval.class).add(LocalDateTime.class, "start").add(LocalDateTime.class, "end").add(params).begin()
                .declareVar(long.class, "startMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("start"), ref(tz.getMemberName())))
                .declareVar(long.class, "endMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("end"), ref(tz.getMemberName())))
                .declareVar(long.class, "deltaMSec", op(ref("endMs"), "-", ref("startMs")))
                .declareVar(LocalDateTime.class, "result", start);
        evaluateCalOpsLDTCodegen(block, "result", forge.calendarForges, params, context);
        block.declareVar(long.class, "startLong", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("result"), ref(tz.getMemberName())));
        block.declareVar(long.class, "endTime", op(ref("startLong"), "+", ref("deltaMSec")));
        String method = block.methodReturn(forge.intervalForge.codegen(ref("startLong"), ref("endTime"), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
