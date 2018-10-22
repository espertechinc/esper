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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newArrayWithInit;

public class SubordinateQueryPlanDescForge {
    private final SubordTableLookupStrategyFactoryForge lookupStrategyFactory;
    private final SubordinateQueryIndexDescForge[] indexDescs;

    public SubordinateQueryPlanDescForge(SubordTableLookupStrategyFactoryForge lookupStrategyFactory, SubordinateQueryIndexDescForge[] indexDescs) {
        this.lookupStrategyFactory = lookupStrategyFactory;
        this.indexDescs = indexDescs;
    }

    public SubordTableLookupStrategyFactoryForge getLookupStrategyFactory() {
        return lookupStrategyFactory;
    }

    public SubordinateQueryIndexDescForge[] getIndexDescs() {
        return indexDescs;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(SubordinateQueryPlanDesc.class, this.getClass(), "strategy", parent, symbols, classScope);
        int numIndex = indexDescs == null ? 0 : indexDescs.length;
        CodegenExpression[] indexDescArray = new CodegenExpression[numIndex];
        for (int i = 0; i < numIndex; i++) {
            indexDescArray[i] = indexDescs[i].make(builder.getMethod(), symbols, classScope);
        }
        return builder.expression("lookupStrategyFactory", lookupStrategyFactory.make(builder.getMethod(), symbols, classScope))
                .expression("indexDescs", newArrayWithInit(SubordinateQueryIndexDesc.class, indexDescArray))
                .build();
    }
}
