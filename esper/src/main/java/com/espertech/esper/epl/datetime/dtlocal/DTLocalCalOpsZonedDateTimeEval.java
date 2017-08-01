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

import java.time.ZonedDateTime;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
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

    public static CodegenExpression codegen(DTLocalCalOpsZonedDateTimeForge forge, CodegenExpression inner, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(ZonedDateTime.class, DTLocalCalOpsZonedDateTimeEval.class).add(ZonedDateTime.class, "zdt").add(params).begin();
        evaluateCalOpsZDTCodegen(block, "zdt", forge.calendarForges, params, context);
        String method = block.methodReturn(ref("zdt"));
        return localMethodBuild(method).pass(inner).passAll(params).call();
    }
}
