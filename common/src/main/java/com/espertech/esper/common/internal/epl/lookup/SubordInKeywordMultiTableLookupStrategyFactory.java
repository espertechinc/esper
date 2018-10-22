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
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordInKeywordMultiTableLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    protected final boolean isNWOnTrigger;
    protected final int streamCountOuter;
    protected final ExprEvaluator evaluator;
    protected final String expression;

    public SubordInKeywordMultiTableLookupStrategyFactory(boolean isNWOnTrigger, int streamCountOuter, ExprEvaluator evaluator, String expression) {
        this.isNWOnTrigger = isNWOnTrigger;
        this.streamCountOuter = streamCountOuter;
        this.evaluator = evaluator;
        this.expression = expression;
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
        PropertyHashedEventTable[] indexes = new PropertyHashedEventTable[eventTable.length];
        for (int i = 0; i < eventTable.length; i++) {
            indexes[i] = (PropertyHashedEventTable) eventTable[i];
        }

        if (isNWOnTrigger) {
            return new SubordInKeywordMultiTableLookupStrategyNW(this, indexes);
        } else {
            return new SubordInKeywordMultiTableLookupStrategy(this, indexes);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.INKEYWORDMULTIIDX, new String[]{expression});
    }
}
