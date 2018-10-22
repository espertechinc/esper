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

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.sorted.PropertySortedEventTable;
import com.espertech.esper.common.internal.epl.join.exec.sorted.SortedAccessStrategy;
import com.espertech.esper.common.internal.epl.join.exec.sorted.SortedAccessStrategyFactory;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordSortedTableLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    private final String expression;
    protected final SortedAccessStrategy strategy;

    public SubordSortedTableLookupStrategyFactory(boolean isNWOnTrigger, int numStreams, String expression, QueryGraphValueEntryRange range) {
        this.expression = expression;
        this.strategy = SortedAccessStrategyFactory.make(isNWOnTrigger, -1, numStreams, range);
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
        return new SubordSortedTableLookupStrategy(this, (PropertySortedEventTable) eventTable[0]);
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.RANGE, new String[]{expression});
    }
}
