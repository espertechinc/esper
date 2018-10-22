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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategyMultiForge implements HistoricalIndexLookupStrategyForge {

    private final int indexUsed;
    private final HistoricalIndexLookupStrategyForge innerLookupStrategy;

    public HistoricalIndexLookupStrategyMultiForge(int indexUsed, HistoricalIndexLookupStrategyForge innerLookupStrategy) {
        this.indexUsed = indexUsed;
        this.innerLookupStrategy = innerLookupStrategy;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategyMulti.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(HistoricalIndexLookupStrategyMulti.class, "strat", newInstance(HistoricalIndexLookupStrategyMulti.class))
                .exprDotMethod(ref("strat"), "setIndexUsed", constant(indexUsed))
                .exprDotMethod(ref("strat"), "setInnerLookupStrategy", innerLookupStrategy.make(method, symbols, classScope))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " inner: " + innerLookupStrategy.toQueryPlan();
    }
}
