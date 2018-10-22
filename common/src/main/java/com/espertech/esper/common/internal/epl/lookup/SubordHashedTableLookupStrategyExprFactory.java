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
public class SubordHashedTableLookupStrategyExprFactory implements SubordTableLookupStrategyFactory {
    protected final String[] expressionTexts;
    protected final ExprEvaluator evaluator;
    protected final boolean isNWOnTrigger;
    protected final int numStreamsOuter;

    public SubordHashedTableLookupStrategyExprFactory(String[] expressionTexts, ExprEvaluator evaluator, boolean isNWOnTrigger, int numStreamsOuter) {
        this.expressionTexts = expressionTexts;
        this.evaluator = evaluator;
        this.isNWOnTrigger = isNWOnTrigger;
        this.numStreamsOuter = numStreamsOuter;
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordHashedTableLookupStrategyExprNW(this, (PropertyHashedEventTable) eventTable[0]);
        } else {
            return new SubordHashedTableLookupStrategyExpr(this, (PropertyHashedEventTable) eventTable[0]);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(expressionTexts.length == 1 ? LookupStrategyType.SINGLEEXPR : LookupStrategyType.MULTIEXPR, expressionTexts);
    }
}
