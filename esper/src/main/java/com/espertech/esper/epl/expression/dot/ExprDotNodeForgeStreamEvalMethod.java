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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeStreamEvalMethod implements ExprEvaluator {
    private final ExprDotNodeForgeStream forge;
    private final ExprDotEval[] evaluators;

    public ExprDotNodeForgeStreamEvalMethod(ExprDotNodeForgeStream forge, ExprDotEval[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprStreamUndMethod(forge.getForgeRenderable());
        }

        // get underlying event
        EventBean event = eventsPerStream[forge.getStreamNumber()];
        if (event == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprStreamUndMethod(null);
            }
            return null;
        }
        Object inner = event.getUnderlying();

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDotChain(EPTypeHelper.singleValue(event.getEventType().getUnderlyingType()), inner, evaluators);
        }
        inner = ExprDotNodeUtility.evaluateChain(forge.getEvaluators(), evaluators, inner, eventsPerStream, isNewData, exprEvaluatorContext);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDotChain();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprStreamUndMethod(inner);
        }
        return inner;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeStream forge, CodegenParamSetExprPremade params, CodegenContext context) {
        Class evaluationType = forge.getEvaluationType();
        Class eventUndType = forge.getEventType().getUnderlyingType();
        CodegenBlock block = context.addMethod(evaluationType, ExprDotNodeForgeStreamEvalMethod.class).add(params).begin()
                .declareVar(EventBean.class, "event", arrayAtIndex(params.passEPS(), constant(forge.getStreamNumber())));
        if (evaluationType == void.class) {
            block.ifCondition(equalsNull(ref("event"))).blockReturnNoValue();
        } else {
            block.ifRefNullReturnNull("event");
        }
        block.declareVar(eventUndType, "inner", cast(eventUndType, exprDotMethod(ref("event"), "getUnderlying")));
        CodegenExpression invoke = ExprDotNodeUtility.evaluateChainCodegen(context, params, ref("inner"), eventUndType, forge.getEvaluators(), null);
        String method;
        if (evaluationType == void.class) {
            method = block.expression(invoke).methodEnd();
        } else {
            method = block.methodReturn(invoke);
        }
        return localMethodBuild(method).passAll(params).call();
    }
}
