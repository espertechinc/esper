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
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsLDTCodegen;

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
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.LOCALDATETIME.getEPType(), "target");

        CodegenBlock block = methodNode.getBlock();
        evaluateCalOpsLDTCodegen(block, "target", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("target"), timeZoneField));
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
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalCalOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.LOCALDATETIME.getEPType(), "start").addParam(EPTypePremade.LOCALDATETIME.getEPType(), "end");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("start"), timeZoneField))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endMs", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("end"), timeZoneField))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "deltaMSec", op(ref("endMs"), "-", ref("startMs")))
                .declareVar(EPTypePremade.LOCALDATETIME.getEPType(), "result", start);
        evaluateCalOpsLDTCodegen(block, "result", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startLong", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("result"), timeZoneField));
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endTime", op(ref("startLong"), "+", ref("deltaMSec")));
        block.methodReturn(forge.intervalForge.codegen(ref("startLong"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
