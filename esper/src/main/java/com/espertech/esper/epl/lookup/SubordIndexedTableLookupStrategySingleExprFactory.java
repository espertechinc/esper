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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategySingleExprFactory implements SubordTableLookupStrategyFactory {
    protected final ExprEvaluator evaluator;
    protected boolean isNWOnTrigger;
    protected int streamCountOuter;
    protected final LookupStrategyDesc strategyDesc;

    public SubordIndexedTableLookupStrategySingleExprFactory(boolean isNWOnTrigger, int streamCountOuter, SubordPropHashKey hashKey) {
        this.streamCountOuter = streamCountOuter;
        this.evaluator = hashKey.getHashKey().getKeyExpr().getForge().getExprEvaluator();
        this.isNWOnTrigger = isNWOnTrigger;
        this.strategyDesc = new LookupStrategyDesc(LookupStrategyType.SINGLEEXPR, new String[]{ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(hashKey.getHashKey().getKeyExpr())});
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordIndexedTableLookupStrategySingleExprNW(evaluator, (PropertyIndexedEventTableSingle) eventTable[0], strategyDesc);
        } else {
            return new SubordIndexedTableLookupStrategySingleExpr(streamCountOuter, evaluator, (PropertyIndexedEventTableSingle) eventTable[0], strategyDesc);
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " evaluator " + evaluator.getClass().getSimpleName();
    }
}
