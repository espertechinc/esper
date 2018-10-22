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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordSortedTableLookupStrategyFactoryForge implements SubordTableLookupStrategyFactoryForge {
    private final boolean isNWOnTrigger;
    private final int numStreamsOuter;
    private final SubordPropRangeKeyForge rangeKey;
    private final CoercionDesc coercionDesc;

    public SubordSortedTableLookupStrategyFactoryForge(boolean isNWOnTrigger, int numStreamsOuter, SubordPropRangeKeyForge rangeKey, CoercionDesc coercionDesc) {
        this.isNWOnTrigger = isNWOnTrigger;
        this.numStreamsOuter = numStreamsOuter;
        this.rangeKey = rangeKey;
        this.coercionDesc = coercionDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " range " + rangeKey.toQueryPlan();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        String[] expressions = ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(rangeKey.getRangeInfo().getExpressions());
        return newInstance(SubordSortedTableLookupStrategyFactory.class, constant(isNWOnTrigger), constant(numStreamsOuter),
                constant(expressions[0]),
                rangeKey.getRangeInfo().make(coercionDesc.getCoercionTypes()[0], parent, symbols, classScope));
    }
}
