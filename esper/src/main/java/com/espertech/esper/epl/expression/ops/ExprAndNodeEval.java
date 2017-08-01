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
package com.espertech.esper.epl.expression.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprAndNodeEval implements ExprEvaluator {
    private final ExprAndNodeImpl parent;
    private final ExprEvaluator[] evaluators;

    public ExprAndNodeEval(ExprAndNodeImpl parent, ExprEvaluator[] evaluators) {
        this.parent = parent;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprAnd(parent);
        }
        Boolean result = true;
        for (ExprEvaluator child : evaluators) {
            Boolean evaluated = (Boolean) child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (evaluated == null) {
                result = null;
            } else {
                if (!evaluated) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aExprAnd(false);
                    }
                    return false;
                }
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprAnd(result);
        }
        return result;
    }

    public static CodegenExpression codegen(ExprAndNodeImpl parent, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenBlock block = context.addMethod(Boolean.class, ExprOrNodeEval.class).add(params).begin()
                .declareVar(Boolean.class, "result", constantTrue());

        int count = -1;
        for (ExprNode child : parent.getChildNodes()) {
            count++;
            Class childType = child.getForge().getEvaluationType();
            if (childType.isPrimitive()) {
                block.ifCondition(not(child.getForge().evaluateCodegen(params, context))).blockReturn(constantFalse());
            } else {
                String refname = "r" + count;
                block.declareVar(Boolean.class, refname, child.getForge().evaluateCodegen(params, context))
                        .ifCondition(equalsNull(ref(refname)))
                        .assignRef("result", constantNull())
                        .ifElse()
                        .ifCondition(not(ref(refname))).blockReturn(constantFalse());
            }
        }
        String method = block.methodReturn(ref("result"));
        return localMethodBuild(method).passAll(params).call();
    }

}
