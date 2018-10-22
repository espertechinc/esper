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
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordInKeywordMultiTableLookupStrategyFactoryForge implements SubordTableLookupStrategyFactoryForge {
    protected final boolean isNWOnTrigger;
    protected final int streamCountOuter;
    protected final ExprNode exprNode;

    public SubordInKeywordMultiTableLookupStrategyFactoryForge(boolean isNWOnTrigger, int streamCountOuter, ExprNode exprNode) {
        this.streamCountOuter = streamCountOuter;
        this.isNWOnTrigger = isNWOnTrigger;
        this.exprNode = exprNode;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordInKeywordMultiTableLookupStrategyFactory.class, this.getClass(), classScope);
        String expression = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(exprNode);
        CodegenExpression eval = ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(exprNode.getForge(), method, this.getClass(), classScope);
        method.getBlock().methodReturn(newInstance(SubordInKeywordMultiTableLookupStrategyFactory.class, constant(isNWOnTrigger),
                constant(streamCountOuter), eval, constant(expression)));
        return localMethod(method);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
