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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeLongCoercerZonedDateTime;
import com.espertech.esper.common.internal.epl.datetime.interval.IntervalOp;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.time.ZonedDateTime;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

class DTLocalZDTIntervalEval extends DTLocalEvaluatorIntervalBase {

    public DTLocalZDTIntervalEval(IntervalOp intervalOp) {
        super(intervalOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) target);
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZDTIntervalForge forge, CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalZDTIntervalEval.class, codegenClassScope).addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "target");

        methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "time", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("target")))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) startTimestamp);
        long end = DatetimeLongCoercerZonedDateTime.coerceZDTToMillis((ZonedDateTime) endTimestamp);
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalZDTIntervalForge forge, CodegenExpression start, CodegenExpression end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethod methodNode = codegenMethodScope.makeChild(EPTypePremade.BOOLEANBOXED.getEPType(), DTLocalZDTIntervalEval.class, codegenClassScope).addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "startTimestamp").addParam(EPTypePremade.ZONEDDATETIME.getEPType(), "endTimestamp");

        methodNode.getBlock()
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "start", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("startTimestamp")))
                .declareVar(EPTypePremade.LONGPRIMITIVE.getEPType(), "end", staticMethod(DatetimeLongCoercerZonedDateTime.class, "coerceZDTToMillis", ref("endTimestamp")))
                .methodReturn(forge.intervalForge.codegen(ref("start"), ref("end"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
