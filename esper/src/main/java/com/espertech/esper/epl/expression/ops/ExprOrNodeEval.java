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
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprOrNodeEval implements ExprEvaluator {
    private final ExprOrNode parent;
    private final ExprEvaluator[] evaluators;

    public ExprOrNodeEval(ExprOrNode parent, ExprEvaluator[] evaluators) {
        this.parent = parent;
        this.evaluators = evaluators;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprOr(parent);
        }
        Boolean result = false;
        // At least one child must evaluate to true
        for (ExprEvaluator child : evaluators) {
            Boolean evaluated = (Boolean) child.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (evaluated == null) {
                result = null;
            } else {
                if (evaluated) {
                    if (InstrumentationHelper.ENABLED) {
                        InstrumentationHelper.get().aExprOr(true);
                    }
                    return true;
                }
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprOr(result);
        }
        return result;
    }

    public static CodegenExpression codegen(ExprOrNode parent, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Boolean.class, ExprOrNodeEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Boolean.class, "result", constantFalse());

        int count = -1;
        for (ExprNode child : parent.getChildNodes()) {
            count++;
            Class childType = child.getForge().getEvaluationType();
            if (childType.isPrimitive()) {
                block.ifCondition(child.getForge().evaluateCodegen(Boolean.class, methodNode, exprSymbol, codegenClassScope)).blockReturn(constantTrue());
            } else {
                String refname = "r" + count;
                block.declareVar(Boolean.class, refname, child.getForge().evaluateCodegen(Boolean.class, methodNode, exprSymbol, codegenClassScope))
                        .ifCondition(equalsNull(ref(refname)))
                        .assignRef("result", constantNull())
                        .ifElse()
                        .ifCondition(ref(refname)).blockReturn(constantTrue());
            }
        }
        block.methodReturn(ref("result"));
        return localMethod(methodNode);
    }
}
