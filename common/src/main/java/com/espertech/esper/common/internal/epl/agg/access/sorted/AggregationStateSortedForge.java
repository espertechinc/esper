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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregatorAccess;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

public class AggregationStateSortedForge implements AggregationStateFactoryForge {

    protected final AggregationForgeFactoryAccessSorted factory;
    private AggregatorAccessSorted aggregatorAccess;

    public AggregationStateSortedForge(AggregationForgeFactoryAccessSorted factory) {
        this.factory = factory;
    }

    public void initAccessForge(int col, boolean join, CodegenCtor ctor, CodegenMemberCol membersColumnized, CodegenClassScope classScope) {
        aggregatorAccess = new AggregatorAccessSortedImpl(join, this, col, ctor, membersColumnized, classScope, factory.getParent().getOptionalFilter());
    }

    public AggregatorAccess getAggregator() {
        return aggregatorAccess;
    }

    public SortedAggregationStateDesc getSpec() {
        return factory.getOptionalSortedStateDesc();
    }

    public CodegenExpression codegenGetAccessTableState(int column, CodegenMethodScope parent, CodegenClassScope classScope) {
        return AggregatorAccessSortedImpl.codegenGetAccessTableState(column, parent, classScope);
    }

    public ExprNode getExpression() {
        return factory.getAggregationExpression();
    }
}
