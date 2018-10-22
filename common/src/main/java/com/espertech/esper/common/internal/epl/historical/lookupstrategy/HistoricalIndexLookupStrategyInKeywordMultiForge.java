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
package com.espertech.esper.common.internal.epl.historical.lookupstrategy;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategyInKeywordMultiForge implements HistoricalIndexLookupStrategyForge {

    private final int lookupStream;
    private final ExprNode evaluator;

    public HistoricalIndexLookupStrategyInKeywordMultiForge(int lookupStream, ExprNode evaluator) {
        this.lookupStream = lookupStream;
        this.evaluator = evaluator;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategyInKeywordMulti.class, this.getClass(), classScope);

        method.getBlock()
                .declareVar(HistoricalIndexLookupStrategyInKeywordMulti.class, "strat", newInstance(HistoricalIndexLookupStrategyInKeywordMulti.class))
                .exprDotMethod(ref("strat"), "setLookupStream", constant(lookupStream))
                .exprDotMethod(ref("strat"), "setEvaluator", ExprNodeUtilityCodegen.codegenEvaluator(evaluator.getForge(), method, this.getClass(), classScope))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
