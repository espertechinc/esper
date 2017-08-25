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
package com.espertech.esper.epl.core.select;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.*;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprTypableReturnEval;
import com.espertech.esper.event.EventBeanManufacturer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class SelectExprProcessorTypableSingleEvalSingleRow implements ExprEvaluator {
    private final SelectExprProcessorTypableSingleForge forge;
    private final ExprTypableReturnEval typable;

    public SelectExprProcessorTypableSingleEvalSingleRow(SelectExprProcessorTypableSingleForge forge, ExprTypableReturnEval typable) {
        this.forge = forge;
        this.typable = typable;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] row = typable.evaluateTypableSingle(eventsPerStream, isNewData, exprEvaluatorContext);
        if (row == null) {
            return null;
        }
        if (forge.hasWideners) {
            SelectExprProcessorHelper.applyWideners(row, forge.wideners);
        }
        return forge.factory.make(row);
    }

    public static CodegenExpression codegen(SelectExprProcessorTypableSingleForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember factory = codegenClassScope.makeAddMember(EventBeanManufacturer.class, forge.factory);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, SelectExprProcessorTypableSingleEvalSingleRow.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "row", forge.typable.evaluateTypableSingleCodegen(methodNode, exprSymbol, codegenClassScope))
                .ifRefNullReturnNull("row");
        if (forge.hasWideners) {
            block.expression(SelectExprProcessorHelper.applyWidenersCodegen(ref("row"), forge.wideners, methodNode, codegenClassScope));
        }
        block.methodReturn(exprDotMethod(CodegenExpressionBuilder.member(factory.getMemberId()), "make", ref("row")));
        return localMethod(methodNode);
    }

}
