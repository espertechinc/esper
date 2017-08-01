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
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.datetime.calop.CalendarOp;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class DTLocalCalOpsLocalDateTimeEval extends DTLocalEvaluatorCalOpsCalBase implements DTLocalEvaluator {
    public DTLocalCalOpsLocalDateTimeEval(List<CalendarOp> calendarOps) {
        super(calendarOps);
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        LocalDateTime ldt = (LocalDateTime) target;
        return DTLocalUtil.evaluateCalOpsLDT(calendarOps, ldt, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    public static CodegenExpression codegen(DTLocalCalOpsLocalDateTimeForge forge, CodegenExpression inner, Class innerType, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(LocalDateTime.class, DTLocalCalOpsLocalDateTimeEval.class).add(LocalDateTime.class, "target").add(params).begin();
        DTLocalUtil.evaluateCalOpsLDTCodegen(block, "target", forge.calendarForges, params, context);
        return localMethodBuild(block.methodReturn(ref("target"))).pass(inner).passAll(params).call();
    }
}
