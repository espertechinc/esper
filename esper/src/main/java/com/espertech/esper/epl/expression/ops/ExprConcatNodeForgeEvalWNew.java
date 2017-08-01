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
import com.espertech.esper.codegen.model.expression.CodegenExpressionExprDotMethodChain;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprConcatNodeForgeEvalWNew implements ExprEvaluator {
    private final ExprConcatNodeForge forge;
    private final ExprEvaluator[] evaluators;

    ExprConcatNodeForgeEvalWNew(ExprConcatNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        StringBuffer buffer = new StringBuffer();
        return evaluate(eventsPerStream, isNewData, context, buffer, evaluators, forge);
    }

    protected static String evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context, StringBuffer buffer, ExprEvaluator[] evaluators, ExprConcatNodeForge form) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprConcat(form.getForgeRenderable());
        }
        for (ExprEvaluator child : evaluators) {
            String result = (String) child.evaluate(eventsPerStream, isNewData, context);
            if (result == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprConcat(null);
                }
                return null;
            }
            buffer.append(result);
        }
        String result = buffer.toString();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprConcat(result);
        }
        return result;
    }

    public static CodegenExpression codegen(ExprConcatNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        CodegenBlock block = context.addMethod(String.class, ExprConcatNodeForgeEvalWNew.class).add(params).begin();
        block.declareVar(StringBuffer.class, "buf", newInstance(StringBuffer.class));
        block.declareVarNoInit(String.class, "value");
        CodegenExpressionExprDotMethodChain chain = exprDotMethodChain(ref("buf"));
        for (ExprNode expr : forge.getForgeRenderable().getChildNodes()) {
            block.assignRef("value", expr.getForge().evaluateCodegen(params, context))
                    .ifRefNullReturnNull("value")
                    .exprDotMethod(ref("buf"), "append", ref("value"));
        }
        String method = block.methodReturn(exprDotMethod(chain, "toString"));
        return localMethodBuild(method).passAll(params).call();
    }

}
