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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordInKeywordMultiTableLookupStrategyFactory implements SubordTableLookupStrategyFactory {
    protected final ExprEvaluator evaluator;
    protected boolean isNWOnTrigger;
    protected int streamCountOuter;
    protected final LookupStrategyDesc strategyDesc;

    public SubordInKeywordMultiTableLookupStrategyFactory(boolean isNWOnTrigger, int streamCountOuter, ExprNode exprNode) {
        this.streamCountOuter = streamCountOuter;
        this.evaluator = exprNode.getForge().getExprEvaluator();
        this.isNWOnTrigger = isNWOnTrigger;
        this.strategyDesc = new LookupStrategyDesc(LookupStrategyType.INKEYWORDMULTIIDX, new String[]{ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(exprNode)});
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordInKeywordMultiTableLookupStrategyNW(evaluator, eventTable, strategyDesc);
        } else {
            return new SubordInKeywordMultiTableLookupStrategy(streamCountOuter, evaluator, eventTable, strategyDesc);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }
}
