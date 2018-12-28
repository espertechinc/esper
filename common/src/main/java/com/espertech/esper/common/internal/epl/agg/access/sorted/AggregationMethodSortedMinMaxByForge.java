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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationMethodSortedMinMaxByForge implements AggregationMethodForge {
    private final Class resultType;
    private final boolean max;

    public AggregationMethodSortedMinMaxByForge(Class resultType, boolean max) {
        this.resultType = resultType;
        this.max = max;
    }

    public Class getResultType() {
        return resultType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationMethodSortedMinMaxBy.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationMethodSortedMinMaxBy.class, "strat", newInstance(AggregationMethodSortedMinMaxBy.class))
            .exprDotMethod(ref("strat"), "setMax", constant(max))
            .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
