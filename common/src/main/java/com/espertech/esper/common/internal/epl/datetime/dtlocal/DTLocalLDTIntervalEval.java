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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerLocalDateTime;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

class DTLocalLDTIntervalEval extends DTLocalEvaluatorIntervalBase {

    private final TimeZone timeZone;

    public DTLocalLDTIntervalEval(IntervalOp intervalOp, TimeZone timeZone) {
        super(intervalOp);
        this.timeZone = timeZone;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) target, timeZone);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLDTIntervalForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalLDTIntervalEval.class, codegenClassScope).addParam(EPTypePremade.LOCALDATETIME.getEPType(), "target");

        methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("target"), timeZoneField))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) startTimestamp, timeZone);
        long end = DatetimeLongCoercerLocalDateTime.coerceLDTToMilliWTimezone((LocalDateTime) endTimestamp, timeZone);
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalLDTIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalLDTIntervalEval.class, codegenClassScope).addParam(EPTypePremade.LOCALDATETIME.getEPType(), "startTimestamp").addParam(EPTypePremade.LOCALDATETIME.getEPType(), "endTimestamp");

        methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "start", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("startTimestamp"), timeZoneField))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "end", staticMethod(DatetimeLongCoercerLocalDateTime.class, "coerceLDTToMilliWTimezone", ref("endTimestamp"), timeZoneField))
                .methodReturn(forge.intervalForge.codegen(ref("start"), ref("end"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
