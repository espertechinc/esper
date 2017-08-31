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
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.datetime.interval.IntervalOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class DTLocalCalIntervalEval extends DTLocalEvaluatorIntervalBase {
    public DTLocalCalIntervalEval(IntervalOp intervalOp) {
        super(intervalOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long time = ((Calendar) target).getTimeInMillis();
        return intervalOp.evaluate(time, time, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalCalIntervalForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalIntervalEval.class, codegenClassScope).addParam(Calendar.class, "target");

        methodNode.getBlock()
                .declareVar(long.class, "time", exprDotMethod(ref("target"), "getTimeInMillis"))
                .methodReturn(forge.intervalForge.codegen(ref("time"), ref("time"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }

    public Object evaluate(Object startTimestamp, Object endTimestamp, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        long start = ((Calendar) startTimestamp).getTimeInMillis();
        long end = ((Calendar) endTimestamp).getTimeInMillis();
        return intervalOp.evaluate(start, end, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalCalIntervalForge forge, CodegenExpressionRef start, CodegenExpressionRef end, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, DTLocalCalIntervalEval.class, codegenClassScope).addParam(Calendar.class, "start").addParam(Calendar.class, "end");

        methodNode.getBlock().methodReturn(forge.intervalForge.codegen(exprDotMethod(ref("start"), "getTimeInMillis"), exprDotMethod(ref("end"), "getTimeInMillis"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, start, end);
    }
}
