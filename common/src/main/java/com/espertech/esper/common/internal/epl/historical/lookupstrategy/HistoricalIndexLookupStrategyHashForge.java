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
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategyHashForge implements HistoricalIndexLookupStrategyForge {

    private final int lookupStream;
    private final ExprForge[] evaluators;
    private final Class[] coercionTypes;

    public HistoricalIndexLookupStrategyHashForge(int lookupStream, ExprForge[] evaluators, Class[] coercionTypes) {
        this.lookupStream = lookupStream;
        this.evaluators = evaluators;
        this.coercionTypes = coercionTypes;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategyHash.class, this.getClass(), classScope);

        method.getBlock()
                .declareVar(HistoricalIndexLookupStrategyHash.class, "strat", newInstance(HistoricalIndexLookupStrategyHash.class))
                .exprDotMethod(ref("strat"), "setLookupStream", constant(lookupStream))
                .exprDotMethod(ref("strat"), "setEvaluator", ExprNodeUtilityCodegen.codegenEvaluatorMayMultiKeyWCoerce(evaluators, coercionTypes, method, this.getClass(), classScope))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
