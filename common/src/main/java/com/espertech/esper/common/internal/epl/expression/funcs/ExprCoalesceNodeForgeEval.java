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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprCoalesceNodeForgeEval implements ExprEvaluator {
    private final ExprCoalesceNodeForge forge;
    private final ExprEvaluator[] evaluators;

    ExprCoalesceNodeForgeEval(ExprCoalesceNodeForge forge, ExprEvaluator[] evaluators) {
        this.forge = forge;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value;

        // Look for the first non-null return value
        for (int i = 0; i < evaluators.length; i++) {
            value = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);

            if (value != null) {
                // Check if we need to coerce
                if (forge.getIsNumericCoercion()[i]) {
                    value = JavaClassHelper.coerceBoxed((Number) value, forge.getEvaluationType());
                }
                return value;
            }
        }

        return null;
    }

    public static CodegenExpression codegen(ExprCoalesceNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (forge.getEvaluationType() == null) {
            return constantNull();
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprCoalesceNodeForgeEval.class, codegenClassScope);


        CodegenBlock block = methodNode.getBlock();
        int num = 0;
        boolean doneWithReturn = false;
        for (ExprNode node : forge.getForgeRenderable().getChildNodes()) {
            Class reftype = node.getForge().getEvaluationType();
            if (reftype != null) {
                String refname = "r" + num;
                block.declareVar(reftype, refname, node.getForge().evaluateCodegen(reftype, methodNode, exprSymbol, codegenClassScope));

                if (reftype.isPrimitive()) {
                    if (!forge.getIsNumericCoercion()[num]) {
                        block.methodReturn(ref(refname));
                        doneWithReturn = true;
                    } else {
                        SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(reftype, forge.getEvaluationType());
                        block.methodReturn(coercer.coerceCodegen(ref(refname), reftype));
                        doneWithReturn = true;
                    }
                    break;
                }

                CodegenBlock blockIf = block.ifCondition(notEqualsNull(ref(refname)));
                if (!forge.getIsNumericCoercion()[num]) {
                    blockIf.blockReturn(ref(refname));
                } else {
                    blockIf.blockReturn(JavaClassHelper.coerceNumberBoxedToBoxedCodegen(ref(refname), reftype, forge.getEvaluationType()));
                }
            }
            num++;
        }

        if (!doneWithReturn) {
            block.methodReturn(constantNull());
        }
        return localMethod(methodNode);
    }
}
