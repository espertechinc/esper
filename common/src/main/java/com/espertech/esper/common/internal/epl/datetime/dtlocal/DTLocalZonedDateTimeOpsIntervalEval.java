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
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.time.ZonedDateTime;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDT;
import static com.espertech.esper.common.internal.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDTCodegen;

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

    public static CodegenExpression codegen(DTLocalZonedDateTimeOpsIntervalForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalZonedDateTimeOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "target");


        CodegenBlock block = methodNode.getBlock();
        evaluateCalOpsZDTCodegen(block, "target", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("target")));
        block.methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
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

    public static CodegenExpression codegen(DTLocalZonedDateTimeOpsIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalZonedDateTimeOpsIntervalEval.class, codegenClassScope).addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "start").addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "end");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startMs", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("start")))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endMs", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("end")))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "deltaMSec", op(ref("endMs"), "-", ref("startMs")))
                .declareVar(EPTypePremade.ZONEDDATETIME.getEPType(), "result", start);
        evaluateCalOpsZDTCodegen(block, "result", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "startLong", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("result")));
        block.declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "endTime", op(ref("startLong"), "+", ref("deltaMSec")));
        block.methodReturn(forge.intervalForge.codegen(ref("startLong"), ref("endTime"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
