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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubordinateWMatchExprQueryPlanForge {
    private final SubordWMatchExprLookupStrategyFactoryForge strategy;
    private final SubordinateQueryIndexDescForge[] indexes;

    public SubordinateWMatchExprQueryPlanForge(SubordWMatchExprLookupStrategyFactoryForge strategy, SubordinateQueryIndexDescForge[] indexes) {
        this.strategy = strategy;
        this.indexes = indexes;
    }

    public SubordWMatchExprLookupStrategyFactoryForge getStrategy() {
        return strategy;
    }

    public SubordinateQueryIndexDescForge[] getIndexes() {
        return indexes;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordinateWMatchExprQueryPlan.class, this.getClass(), classScope);

        method.getBlock()
                .declareVar(SubordWMatchExprLookupStrategyFactory.class, "strategy", strategy.make(parent, symbols, classScope))
                .declareVar(SubordinateQueryIndexDesc[].class, "indexes", indexes == null ? constantNull() : newArrayByLength(SubordinateQueryIndexDesc.class, constant(indexes.length)));

        if (indexes != null) {
            for (int i = 0; i < indexes.length; i++) {
                method.getBlock().assignArrayElement("indexes", constant(i), indexes[i].make(method, symbols, classScope));
            }
        }

        method.getBlock().methodReturn(newInstance(SubordinateWMatchExprQueryPlan.class, ref("strategy"), ref("indexes")));
        return localMethod(method);
    }
}
