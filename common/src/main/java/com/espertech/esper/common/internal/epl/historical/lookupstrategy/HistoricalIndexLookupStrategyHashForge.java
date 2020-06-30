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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class HistoricalIndexLookupStrategyHashForge implements HistoricalIndexLookupStrategyForge {

    private final int lookupStream;
    private final ExprForge[] evaluators;
    private final EPTypeClass[] coercionTypes;
    private final MultiKeyClassRef multiKeyClassRef;

    public HistoricalIndexLookupStrategyHashForge(int lookupStream, ExprForge[] evaluators, EPTypeClass[] coercionTypes, MultiKeyClassRef multiKeyClassRef) {
        this.lookupStream = lookupStream;
        this.evaluators = evaluators;
        this.coercionTypes = coercionTypes;
        this.multiKeyClassRef = multiKeyClassRef;
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(HistoricalIndexLookupStrategyHash.EPTYPE, this.getClass(), classScope);

        CodegenExpression evaluator = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(evaluators, coercionTypes, multiKeyClassRef, method, classScope);
        method.getBlock()
            .declareVarNewInstance(HistoricalIndexLookupStrategyHash.EPTYPE, "strat")
            .exprDotMethod(ref("strat"), "setLookupStream", constant(lookupStream))
            .exprDotMethod(ref("strat"), "setEvaluator", evaluator)
            .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
