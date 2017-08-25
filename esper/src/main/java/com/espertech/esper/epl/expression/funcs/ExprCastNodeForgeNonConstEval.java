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

public class ExprCastNodeForgeNonConstEval implements ExprEvaluator {
    private final ExprCastNodeForge forge;
    private final ExprEvaluator evaluator;
    private final ExprCastNode.CasterParserComputer casterParserComputer;

    public ExprCastNodeForgeNonConstEval(ExprCastNodeForge forge, ExprEvaluator evaluator, ExprCastNode.CasterParserComputer casterParserComputer) {
        this.forge = forge;
        this.evaluator = evaluator;
        this.casterParserComputer = casterParserComputer;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprCast(forge.getForgeRenderable());
        }

        Object result = evaluator.evaluate(eventsPerStream, isNewData, context);
        if (result != null) {
            result = casterParserComputer.compute(result, eventsPerStream, isNewData, context);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprCast(result);
        }
        return result;
    }

    public static CodegenExpression codegen(ExprCastNodeForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        if (forge.getEvaluationType() == null) {
            return constantNull();
        }
        ExprNode child = forge.getForgeRenderable().getChildNodes()[0];
        Class childType = child.getForge().getEvaluationType();
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprCastNodeForgeNonConstEval.class, codegenClassScope);


        CodegenBlock block = methodNode.getBlock()
                .declareVar(childType, "result", child.getForge().evaluateCodegen(childType, methodNode, exprSymbol, codegenClassScope));
        if (!childType.isPrimitive()) {
            block.ifRefNullReturnNull("result");
        }
        CodegenExpression cast = forge.getCasterParserComputerForge().codegenPremade(forge.getEvaluationType(), ref("result"), childType, methodNode, exprSymbol, codegenClassScope);
        block.methodReturn(cast);
        return localMethod(methodNode);
    }

}
