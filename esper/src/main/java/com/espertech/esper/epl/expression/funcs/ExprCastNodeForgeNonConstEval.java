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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
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

    public static CodegenExpression codegen(ExprCastNodeForge forge, CodegenContext context, CodegenParamSetExprPremade params) {
        if (forge.getEvaluationType() == null) {
            return constantNull();
        }
        ExprNode child = forge.getForgeRenderable().getChildNodes()[0];
        Class childType = child.getForge().getEvaluationType();
        CodegenBlock block = context.addMethod(forge.getEvaluationType(), ExprCastNodeForgeNonConstEval.class).add(params).begin()
                .declareVar(childType, "result", child.getForge().evaluateCodegen(params, context));
        if (!childType.isPrimitive()) {
            block.ifRefNullReturnNull("result");
        }
        CodegenExpression cast = forge.getCasterParserComputerForge().codegenPremade(forge.getEvaluationType(), ref("result"), childType, context, params);
        CodegenMethodId method = block.methodReturn(cast);
        return localMethodBuild(method).passAll(params).call();
    }

}
