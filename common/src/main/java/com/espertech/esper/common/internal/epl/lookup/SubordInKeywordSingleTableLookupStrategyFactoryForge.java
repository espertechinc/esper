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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubordInKeywordSingleTableLookupStrategyFactoryForge implements SubordTableLookupStrategyFactoryForge {
    private final boolean isNWOnTrigger;
    private final int streamCountOuter;
    private final ExprNode[] exprNodes;

    public SubordInKeywordSingleTableLookupStrategyFactoryForge(boolean isNWOnTrigger, int streamCountOuter, ExprNode[] exprNodes) {
        this.streamCountOuter = streamCountOuter;
        this.isNWOnTrigger = isNWOnTrigger;
        this.exprNodes = exprNodes;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordInKeywordSingleTableLookupStrategyFactory.class, this.getClass(), classScope);

        String[] expressions = new String[exprNodes.length];
        method.getBlock().declareVar(ExprEvaluator[].class, "evals", newArrayByLength(ExprEvaluator.class, constant(exprNodes.length)));
        for (int i = 0; i < exprNodes.length; i++) {
            CodegenExpression eval = ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(exprNodes[i].getForge(), method, this.getClass(), classScope);
            method.getBlock().assignArrayElement(ref("evals"), constant(i), eval);
            expressions[i] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNodes[i]);
        }

        method.getBlock().methodReturn(newInstance(SubordInKeywordSingleTableLookupStrategyFactory.class,
                constant(isNWOnTrigger), constant(streamCountOuter), ref("evals"), constant(expressions)));
        return localMethod(method);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
