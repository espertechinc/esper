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
import com.espertech.esper.epl.datetime.reformatop.ReformatOp;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Calendar;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class DTLocalCalOpsReformatEval extends DTLocalEvaluatorCalopReformatBase {
    public DTLocalCalOpsReformatEval(List<CalendarOp> calendarOps, ReformatOp reformatOp) {
        super(calendarOps, reformatOp);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Calendar cal = (Calendar) ((Calendar) target).clone();
        DTLocalUtil.evaluateCalOpsCalendar(calendarOps, cal, eventsPerStream, isNewData, exprEvaluatorContext);
        return reformatOp.evaluate(cal, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalCalOpsReformatForge forge, CodegenExpression inner, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.reformatForge.getReturnType(), DTLocalDateOpsReformatEval.class, codegenClassScope).addParam(Calendar.class, "target");

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Calendar.class, "cal", cast(Calendar.class, exprDotMethod(ref("target"), "clone")));
        DTLocalUtil.evaluateCalOpsCalendarCodegen(block, forge.calendarForges, ref("cal"), methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(forge.reformatForge.codegenCal(ref("cal"), methodNode, exprSymbol, codegenClassScope));
        return localMethod(methodNode, inner);
    }
}
