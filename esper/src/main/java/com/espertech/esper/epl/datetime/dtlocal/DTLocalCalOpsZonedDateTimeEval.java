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
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.ZonedDateTime;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDT;
import static com.espertech.esper.epl.datetime.dtlocal.DTLocalUtil.evaluateCalOpsZDTCodegen;

public class DTLocalCalOpsZonedDateTimeEval extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {
    public DTLocalCalOpsZonedDateTimeEval(List<CalendarOp> calendarOps) {
        super(calendarOps);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        ZonedDateTime zdt = (ZonedDateTime) target;
        return evaluateCalOpsZDT(calendarOps, zdt, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalCalOpsZonedDateTimeForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(ZonedDateTime.class, DTLocalCalOpsZonedDateTimeEval.class, codegenClassScope).addParam(ZonedDateTime.class, "zdt");

        CodegenBlock block = methodNode.getBlock();
        evaluateCalOpsZDTCodegen(block, "zdt", forge.calendarForges, methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(ref("zdt"));
        return localMethod(methodNode, inner);
    }
}
