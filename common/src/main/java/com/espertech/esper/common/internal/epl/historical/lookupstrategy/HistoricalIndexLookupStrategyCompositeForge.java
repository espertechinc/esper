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
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategyCompositeForge implements HistoricalIndexLookupStrategyForge {

    private final int lookupStream;
    private final ExprForge[] evaluators;
    private final MultiKeyClassRef multiKeyClasses;
    private final QueryGraphValueEntryRangeForge[] ranges;

    public HistoricalIndexLookupStrategyCompositeForge(int lookupStream, ExprForge[] evaluators, MultiKeyClassRef multiKeyClasses, QueryGraphValueEntryRangeForge[] ranges) {
        this.lookupStream = lookupStream;
        this.evaluators = evaluators;
        this.multiKeyClasses = multiKeyClasses;
        this.ranges = ranges;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategyComposite.class, this.getClass(), classScope);

        method.getBlock().declareVar(QueryGraphValueEntryRange[].class, "rangeGetters", newArrayByLength(QueryGraphValueEntryRange.class, constant(ranges.length)));
        for (int i = 0; i < ranges.length; i++) {
            method.getBlock().assignArrayElement(ref("rangeGetters"), constant(i), ranges[i].make(null, method, symbols, classScope));
        }

        CodegenExpression hashGetter = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(evaluators, null, multiKeyClasses, method, classScope);
        method.getBlock()
            .declareVar(HistoricalIndexLookupStrategyComposite.class, "strat", newInstance(HistoricalIndexLookupStrategyComposite.class))
            .exprDotMethod(ref("strat"), "setLookupStream", constant(lookupStream))
            .exprDotMethod(ref("strat"), "setHashGetter", hashGetter)
            .exprDotMethod(ref("strat"), "setRangeProps", ref("rangeGetters"))
            .exprDotMethod(ref("strat"), "init")
            .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
