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
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsCalendarCodegen;

public class DTLocalCalOpsIntervalEval extends DTLocalEvaluatorCalOpsIntervalBase {
    private final TimeZone timeZone;

    public DTLocalCalOpsIntervalEval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
        super(calendarOps, intervalOp);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = (Calendar) ((Calendar) target).clone();
        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
        long time = cal.getTimeInMillis();
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenPointInTime(DTLocalCalOpsIntervalForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(Calendar.class, "target");


        CodegenBlock block = methodNode.getBlock()
                .declareVar(Calendar.class, "cal", cast(Calendar.class, exprDotMethod(ref("target"), "clone")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(long.class, "time", exprDotMethod(ref("cal"), "getTimeInMillis"))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long startLong = ((Calendar) startTimestamp).getTimeInMillis();
        long endLong = ((Calendar) endTimestamp).getTimeInMillis();
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(startLong);
        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
        long startTime = cal.getTimeInMillis();
        long endTime = startTime + (endLong - startLong);
        return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenStartEnd(DTLocalCalOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember tz = codegenClassScope.makeAddMember(TimeZone.class, forge.timeZone);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(Calendar.class, "startTimestamp").addParam(Calendar.class, "endTimestamp");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(long.class, "startLong", exprDotMethod(ref("startTimestamp"), "getTimeInMillis"))
                .declareVar(long.class, "endLong", exprDotMethod(ref("endTimestamp"), "getTimeInMillis"))
                .declareVar(Calendar.class, "cal", staticMethod(Calendar.class, "getInstance", member(tz.getMemberId())))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", ref("startLong")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(long.class, "startTime", exprDotMethod(ref("cal"), "getTimeInMillis"))
                .declareVar(long.class, "endTime", op(ref("startTime"), "+", op(ref("endLong"), "-", ref("startLong"))))
                .methodReturn(forge.intervalForge.codegen(ref("startTime"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
