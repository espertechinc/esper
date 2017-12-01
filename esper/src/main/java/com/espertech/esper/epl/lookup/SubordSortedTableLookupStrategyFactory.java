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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.join.exec.sorted.SortedAccessStrategy;
import com.espertech.esper.epl.join.exec.sorted.SortedAccessStrategyFactory;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertySortedEventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordSortedTableLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    private final SubordPropRangeKey rangeKey;

    protected final SortedAccessStrategy strategy;

    protected final LookupStrategyDesc strategyDesc;

    public SubordSortedTableLookupStrategyFactory(boolean isNWOnTrigger, int numStreams, SubordPropRangeKey rangeKey) {
        this.rangeKey = rangeKey;
        this.strategy = SortedAccessStrategyFactory.make(isNWOnTrigger, -1, numStreams, rangeKey);
        this.strategyDesc = new LookupStrategyDesc(LookupStrategyType.RANGE, ExprNodeUtilityCore.toExpressionStringsMinPrecedence(rangeKey.getRangeInfo().getExpressions()));
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        return new SubordSortedTableLookupStrategy(strategy, (PropertySortedEventTable) eventTable[0], strategyDesc);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " range " + rangeKey.toQueryPlan();
    }
}
