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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.composite.PropertyCompositeEventTable;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexQuery;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexQueryFactory;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

import java.util.Arrays;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordCompositeTableLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    private final String[] expressions;
    protected final CompositeIndexQuery innerIndexQuery;

    public SubordCompositeTableLookupStrategyFactory(boolean isNWOnTrigger, int numStreams, String[] expressions, ExprEvaluator hashEval, QueryGraphValueEntryRange[] rangeEvals) {
        this.expressions = expressions;
        this.innerIndexQuery = CompositeIndexQueryFactory.makeSubordinate(isNWOnTrigger, numStreams, hashEval, rangeEvals);
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
        return new SubordCompositeTableLookupStrategy(this, (PropertyCompositeEventTable) eventTable[0]);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " ranges=" + Arrays.asList(expressions);
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.COMPOSITE, expressions);
    }
}
