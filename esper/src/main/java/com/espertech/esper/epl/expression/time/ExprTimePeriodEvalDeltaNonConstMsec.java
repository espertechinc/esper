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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
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

    public CodegenExpression deltaAddCodegen(CodegenExpression reference, CodegenParamSetExprPremade params, CodegenContext context) {
        String method = context.addMethod(long.class, ExprTimePeriodEvalDeltaNonConstMsec.class).add(long.class, "currentTime").add(params).begin()
                .declareVar(double.class, "d", forge.evaluateAsSecondsCodegen(params, context))
                .methodReturn(forge.getTimeAbacus().deltaForSecondsDoubleCodegen(ref("d"), context));
        return localMethodBuild(method).pass(reference).passAll(params).call();
    }

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return deltaAdd(currentTime, eventsPerStream, isNewData, context);
    }

    public long deltaUseEngineTime(EventBean[] eventsPerStream, AgentInstanceContext agentInstanceContext) {
        return deltaAdd(0, eventsPerStream, true, agentInstanceContext);
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long msec = deltaAdd(current, eventsPerStream, isNewData, context);
        return new ExprTimePeriodEvalDeltaResult(ExprTimePeriodEvalDeltaConstGivenDelta.deltaAddWReference(current, reference, msec), reference);
    }
}
