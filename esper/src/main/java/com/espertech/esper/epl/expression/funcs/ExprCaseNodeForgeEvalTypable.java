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
import com.espertech.esper.epl.expression.core.ExprTypableReturnEval;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprCaseNodeForgeEvalTypable implements ExprTypableReturnEval {

    private final ExprCaseNodeForge forge;
    private final ExprEvaluator evaluator;

    public ExprCaseNodeForgeEvalTypable(ExprCaseNodeForge forge) {
        this.forge = forge;
        this.evaluator = forge.getExprEvaluator();
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return evaluator.evaluate(eventsPerStream, isNewData, context);
    }

    public Object[] evaluateTypableSingle(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Map<String, Object> map = (Map<String, Object>) evaluator.evaluate(eventsPerStream, isNewData, context);
        Object[] row = new Object[map.size()];
        int index = -1;
        for (Map.Entry<String, Object> entry : forge.mapResultType.entrySet()) {
            index++;
            row[index] = map.get(entry.getKey());
        }
        return row;
    }

    public static CodegenExpression codegenTypeableSingle(ExprCaseNodeForge forge, CodegenParamSetExprPremade params, CodegenContext context) {
        CodegenBlock block = context.addMethod(Object[].class, ExprCaseNodeForgeEvalTypable.class).add(params).begin()
                .declareVar(Map.class, "map", cast(Map.class, forge.evaluateCodegen(params, context)))
                .declareVar(Object[].class, "row", newArray(Object.class, exprDotMethod(ref("map"), "size")));
        int index = -1;
        for (Map.Entry<String, Object> entry : forge.mapResultType.entrySet()) {
            index++;
            block.assignArrayElement(ref("row"), constant(index), exprDotMethod(ref("map"), "get", constant(entry.getKey())));
        }
        return localMethodBuild(block.methodReturn(ref("row"))).passAll(params).call();
    }

    public Object[][] evaluateTypableMulti(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;    // always single-row
    }
}
