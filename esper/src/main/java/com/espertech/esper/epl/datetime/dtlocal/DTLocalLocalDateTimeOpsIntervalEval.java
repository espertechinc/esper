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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
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

    public static CodegenExpression codegen(DTLocalLocalDateTimeOpsIntervalForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(LocalDateTime.class, "target");


        CodegenBlock block = methodNode.getBlock();
        evaluateCalOpsLDTCodegen(block, "target", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(long.class, "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("target"), member(tz.getMemberId())));
        block.methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
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

    public static CodegenExpression codegen(DTLocalLocalDateTimeOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(LocalDateTime.class, "start").addParam(LocalDateTime.class, "end");


        CodegenBlock block = methodNode.getBlock()
                .declareVar(long.class, "startMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("start"), member(tz.getMemberId())))
                .declareVar(long.class, "endMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("end"), member(tz.getMemberId())))
                .declareVar(long.class, "deltaMSec", op(ref("endMs"), "-", ref("startMs")))
                .declareVar(LocalDateTime.class, "result", start);
        evaluateCalOpsLDTCodegen(block, "result", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(long.class, "startLong", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("result"), member(tz.getMemberId())));
        block.declareVar(long.class, "endTime", op(ref("startLong"), "+", ref("deltaMSec")));
        block.methodReturn(forge.intervalForge.codegen(ref("startLong"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
