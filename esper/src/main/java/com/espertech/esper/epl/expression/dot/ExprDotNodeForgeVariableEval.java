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
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.variable.VariableMetaData;
import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeVariableEval implements ExprEvaluator {
    private final ExprDotNodeForgeVariable forge;
    private final ExprDotEval[] chainEval;

    public ExprDotNodeForgeVariableEval(ExprDotNodeForgeVariable forge, ExprDotEval[] chainEval) {
        this.forge = forge;
        this.chainEval = chainEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDot(forge.getForgeRenderable());
        }

        Object result = forge.getVariableReader().getValue();
        result = ExprDotNodeUtility.evaluateChainWithWrap(forge.getResultWrapLambda(), result, forge.getVariableReader().getVariableMetaData().getEventType(), forge.getVariableReader().getVariableMetaData().getType(), chainEval, forge.getChainForge(), eventsPerStream, isNewData, exprEvaluatorContext);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDot(result);
        }
        return result;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeVariable forge, CodegenParamSetExprPremade params, CodegenContext context) {

        CodegenMember variableReader = context.makeAddMember(VariableReader.class, forge.getVariableReader());
        Class variableType;
        VariableMetaData metaData = forge.getVariableReader().getVariableMetaData();
        if (metaData.getEventType() != null) {
            variableType = EventBean.class;
        } else {
            variableType = metaData.getType();
        }
        CodegenBlock block = context.addMethod(forge.getEvaluationType(), ExprDotNodeForgeVariableEval.class).add(params).begin()
                .declareVar(variableType, "result", cast(variableType, exprDotMethod(member(variableReader.getMemberId()), "getValue")));
        CodegenExpression chain = ExprDotNodeUtility.evaluateChainCodegen(context, params, ref("result"), variableType, forge.getChainForge(), forge.getResultWrapLambda());
        return localMethodBuild(block.methodReturn(chain)).passAll(params).call();
    }
}
