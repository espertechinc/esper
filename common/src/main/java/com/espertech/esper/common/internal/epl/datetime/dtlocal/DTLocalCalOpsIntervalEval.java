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
package com.espertech.esper.common.internal.epl.datetime.dtlocal;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarOp;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsCalendarCodegen;

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
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.CALENDAR.getEPType(), "target");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.CALENDAR.getEPType(), "cal", cast(EPTypePremade.CALENDAR.getEPType(), exprDotMethod(ref("target"), "clone")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", exprDotMethod(ref("cal"), "getTimeInMillis"))
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
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.CALENDAR.getEPType(), "startTimestamp").addParam(EPTypePremade.CALENDAR.getEPType(), "endTimestamp");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startLong", exprDotMethod(ref("startTimestamp"), "getTimeInMillis"))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endLong", exprDotMethod(ref("endTimestamp"), "getTimeInMillis"))
                .declareVar(EPTypePremade.CALENDAR.getEPType(), "cal", staticMethod(Calendar.class, "getInstance", timeZoneField))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", ref("startLong")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startTime", exprDotMethod(ref("cal"), "getTimeInMillis"))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endTime", op(ref("startTime"), "+", op(ref("endLong"), "-", ref("startLong"))))
                .methodReturn(forge.intervalForge.codegen(ref("startTime"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
