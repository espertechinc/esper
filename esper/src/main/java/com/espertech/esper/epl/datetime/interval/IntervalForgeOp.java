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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.localMethodBuild;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class IntervalForgeOp implements IntervalOp {

    private final ExprEvaluator evaluatorTimestamp;
    private final IntervalForgeImpl.IntervalOpEval intervalOpEval;

    public IntervalForgeOp(ExprEvaluator evaluatorTimestamp, IntervalForgeImpl.IntervalOpEval intervalOpEval) {
        this.evaluatorTimestamp = evaluatorTimestamp;
        this.intervalOpEval = intervalOpEval;
    }

    public Object evaluate(long startTs, long endTs, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object parameter = evaluatorTimestamp.evaluate(eventsPerStream, isNewData, context);
        if (parameter == null) {
            return parameter;
        }

        return intervalOpEval.evaluate(startTs, endTs, parameter, eventsPerStream, isNewData, context);
    }

    public static CodegenExpression codegen(IntervalForgeImpl forge, CodegenExpression start, CodegenExpression end, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Boolean.class, IntervalForgeOp.class).add(long.class, "startTs").add(long.class, "endTs").add(params).begin()
                .declareVar(forge.getForgeTimestamp().getEvaluationType(), "parameter", forge.getForgeTimestamp().evaluateCodegen(params, context));
        if (!forge.getForgeTimestamp().getEvaluationType().isPrimitive()) {
            block.ifRefNullReturnNull("parameter");
        }
        CodegenMethodId method = block.methodReturn(forge.getIntervalOpForge().codegen(ref("startTs"), ref("endTs"), ref("parameter"), forge.getForgeTimestamp().getEvaluationType(), params, context));
        return localMethodBuild(method).pass(start).pass(end).passAll(params).call();
    }
}
