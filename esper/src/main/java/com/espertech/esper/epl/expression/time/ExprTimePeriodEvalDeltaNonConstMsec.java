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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.schedule.TimeProvider;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class ExprTimePeriodEvalDeltaNonConstMsec implements ExprTimePeriodEvalDeltaNonConst {
    private final ExprTimePeriodForge forge;

    public ExprTimePeriodEvalDeltaNonConstMsec(ExprTimePeriodForge forge) {
        this.forge = forge;
    }

    public long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        double d = forge.evaluateAsSeconds(eventsPerStream, isNewData, context);
        return forge.getTimeAbacus().deltaForSecondsDouble(d);
    }

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(long.class, ExprTimePeriodEvalDeltaNonConstMsec.class, codegenClassScope).addParam(long.class, "currentTime");

        methodNode.getBlock()
                .declareVar(double.class, "d", forge.evaluateAsSecondsCodegen(methodNode, exprSymbol, codegenClassScope))
                .methodReturn(forge.getTimeAbacus().deltaForSecondsDoubleCodegen(ref("d"), codegenClassScope));
        return localMethod(methodNode, reference);
    }

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return deltaAdd(currentTime, eventsPerStream, isNewData, context);
    }

    public long deltaUseEngineTime(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, TimeProvider timeProvider) {
        return deltaAdd(0, eventsPerStream, true, exprEvaluatorContext);
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long msec = deltaAdd(current, eventsPerStream, isNewData, context);
        return new ExprTimePeriodEvalDeltaResult(ExprTimePeriodEvalDeltaConstGivenDelta.deltaAddWReference(current, reference, msec), reference);
    }
}
