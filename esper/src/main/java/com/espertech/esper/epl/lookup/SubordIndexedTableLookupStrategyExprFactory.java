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
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

import java.util.List;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategyExprFactory implements SubordTableLookupStrategyFactory {
    protected final ExprEvaluator[] evaluators;
    protected final boolean isNWOnTrigger;
    protected final int numStreamsOuter;
    protected final LookupStrategyDesc strategyDesc;

    public SubordIndexedTableLookupStrategyExprFactory(boolean isNWOnTrigger, int numStreamsOuter, List<SubordPropHashKey> hashKeys) {
        evaluators = new ExprEvaluator[hashKeys.size()];
        String[] expressions = new String[evaluators.length];
        for (int i = 0; i < hashKeys.size(); i++) {
            evaluators[i] = hashKeys.get(i).getHashKey().getKeyExpr().getForge().getExprEvaluator();
            expressions[i] = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(hashKeys.get(i).getHashKey().getKeyExpr());
        }
        this.isNWOnTrigger = isNWOnTrigger;
        this.numStreamsOuter = numStreamsOuter;
        this.strategyDesc = new LookupStrategyDesc(LookupStrategyType.MULTIEXPR, expressions);
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordIndexedTableLookupStrategyExprNW(evaluators, (PropertyIndexedEventTable) eventTable[0], strategyDesc);
        } else {
            return new SubordIndexedTableLookupStrategyExpr(numStreamsOuter, evaluators, (PropertyIndexedEventTable) eventTable[0], strategyDesc);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " evaluators " + ExprNodeUtilityCore.printEvaluators(evaluators);
    }
}
