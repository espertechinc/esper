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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.SimpleNumberCoercer;
import com.espertech.esper.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprCoalesceNodeForgeEval implements ExprEvaluator {
    private final ExprCoalesceNodeForge forge;
    private final ExprEvaluator[] evaluators;

    ExprCoalesceNodeForgeEval(ExprCoalesceNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCoalesce(forge.getForgeRenderable());
        }
        Object value;

        // Look for the first non-null return value
        for (int i = 0; i < evaluators.length; i++) {
            value = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (value != null) {
                // Check if we need to coerce
                if (forge.getIsNumericCoercion()[i]) {
                    value = JavaClassHelper.coerceBoxed((Number) value, forge.getEvaluationType());
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprCoalesce(value);
                }
                return value;
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCoalesce(null);
        }
        return null;
    }

    public static CodegenExpression codegen(ExprCoalesceNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        if (forge.getEvaluationType() == null) {
            return constantNull();
        }
        CodegenBlock block = context.addMethod(forge.getEvaluationType(), ExprCoalesceNodeForgeEval.class).add(params).begin();
        int num = 0;
        String method = null;
        for (ExprNode node : forge.getForgeRenderable().getChildNodes()) {
            if (node.getForge().getEvaluationType() != null) {
                String refname = "v" + num;
                block.declareVar(node.getForge().getEvaluationType(), refname, node.getForge().evaluateCodegen(params, context));

                if (node.getForge().getEvaluationType().isPrimitive()) {
                    if (!forge.getIsNumericCoercion()[num]) {
                        method = block.methodReturn(ref(refname));
                    } else {
                        SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(node.getForge().getEvaluationType(), forge.getEvaluationType());
                        method = block.methodReturn(coercer.coerceCodegen(ref(refname), node.getForge().getEvaluationType()));
                    }
                    break;
                }

                CodegenBlock blockIf = block.ifCondition(notEqualsNull(ref(refname)));
                if (!forge.getIsNumericCoercion()[num]) {
                    blockIf.blockReturn(ref(refname));
                } else {
                    blockIf.blockReturn(JavaClassHelper.coerceNumberBoxedToBoxedCodegen(ref(refname), node.getForge().getEvaluationType(), forge.getEvaluationType()));
                }
            }
            num++;
        }

        if (method == null) {
            method = block.methodReturn(constantNull());
        }
        return localMethodBuild(method).passAll(params).call();
    }
}
