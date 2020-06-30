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
import com.espertech.esper.common.client.type.EPTypeClass;
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
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsCalendarCodegen;

class DTLocalDateOpsIntervalEval extends DTLocalEvaluatorCalOpsIntervalBase {

    private final TimeZone timeZone;

    public DTLocalDateOpsIntervalEval(List<CalendarOp> calendarOps, IntervalOp intervalOp, TimeZone timeZone) {
        super(calendarOps, intervalOp);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(((Date) target).getTime());
        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
        long time = cal.getTimeInMillis();
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenPointInTime(DTLocalDateOpsIntervalForge forge, CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalDateOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.DATE.getEPType(), "target");

        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.CALENDAR.getEPType(), "cal", staticMethod(Calendar.class, "getInstance", timeZoneField))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", exprDotMethod(ref("target"), "getTime")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", exprDotMethod(ref("cal"), "getTimeInMillis"))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long startLong = ((Date) startTimestamp).getTime();
        long endLong = ((Date) endTimestamp).getTime();
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(startLong);
        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
        long startTime = cal.getTimeInMillis();
        long endTime = startTime + (endLong - startLong);
        return intervalOp.evaluate(startTime, endTime, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegenStartEnd(DTLocalDateOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalDateOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.DATE.getEPType(), "startTimestamp").addParam(EPTypePremade.DATE.getEPType(), "endTimestamp");

        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startLong", exprDotMethod(ref("startTimestamp"), "getTime"))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endLong", exprDotMethod(ref("endTimestamp"), "getTime"))
                .declareVar(EPTypePremade.CALENDAR.getEPType(), "cal", staticMethod(Calendar.class, "getInstance", timeZoneField))
                .expression(exprDotMethod(ref("cal"), "setTimeInMillis", ref("startLong")));
        evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startTime", exprDotMethod(ref("cal"), "getTimeInMillis"))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endTime", op(ref("startTime"), "+", op(ref("endLong"), "-", ref("startLong"))))
                .methodReturn(forge.intervalForge.codegen(ref("startTime"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
