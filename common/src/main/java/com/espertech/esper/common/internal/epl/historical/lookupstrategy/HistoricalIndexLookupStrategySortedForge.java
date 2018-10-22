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
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategySortedForge implements HistoricalIndexLookupStrategyForge {

    private final int lookupStream;
    private final QueryGraphValueEntryRangeForge range;
    private final Class coercionType;

    public HistoricalIndexLookupStrategySortedForge(int lookupStream, QueryGraphValueEntryRangeForge range, Class coercionType) {
        this.lookupStream = lookupStream;
        this.range = range;
        this.coercionType = coercionType;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategySorted.class, this.getClass(), classScope);

        method.getBlock()
                .declareVar(HistoricalIndexLookupStrategySorted.class, "strat", newInstance(HistoricalIndexLookupStrategySorted.class))
                .exprDotMethod(ref("strat"), "setLookupStream", constant(lookupStream))
                .exprDotMethod(ref("strat"), "setEvalRange", range.make(coercionType, method, symbols, classScope))
                .exprDotMethod(ref("strat"), "init")
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
