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
package com.espertech.esper.common.internal.epl.historical.indexingstrategy;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

/**
 * Strategy of indexing that simply builds an unindexed table of poll results.
 * <p>
 * For use when caching is disabled or when no proper index could be build because no where-clause or on-clause exists or
 * these clauses don't yield indexable columns on analysis.
 */
public class PollResultIndexingStrategyNoIndexForge implements PollResultIndexingStrategyForge {
    public final static PollResultIndexingStrategyNoIndexForge INSTANCE = new PollResultIndexingStrategyNoIndexForge();

    private PollResultIndexingStrategyNoIndexForge() {
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return publicConstValue(PollResultIndexingStrategyNoIndex.class, "INSTANCE");
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
