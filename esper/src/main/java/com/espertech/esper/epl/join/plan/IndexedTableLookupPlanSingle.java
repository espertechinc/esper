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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategySingle;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategySingleExpr;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;

import java.util.Collections;

/**
 * Plan to perform an indexed table lookup.
 */
public class IndexedTableLookupPlanSingle extends TableLookupPlan {
    private QueryGraphValueEntryHashKeyed hashKey;

    /**
     * Ctor.
     *
     * @param lookupStream  - stream that generates event to look up for
     * @param indexedStream - stream to index table lookup
     * @param indexNum      - index number for the table containing the full unindexed contents
     * @param hashKey       - properties to use in lookup event to access index
     */
    public IndexedTableLookupPlanSingle(int lookupStream, int indexedStream, TableLookupIndexReqKey indexNum, QueryGraphValueEntryHashKeyed hashKey) {
        super(lookupStream, indexedStream, new TableLookupIndexReqKey[]{indexNum});
        this.hashKey = hashKey;
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(Collections.singletonList(hashKey), Collections.<QueryGraphValueEntryRange>emptyList());
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        PropertyIndexedEventTableSingle index = (PropertyIndexedEventTableSingle) eventTable[0];
        if (hashKey instanceof QueryGraphValueEntryHashKeyedExpr) {
            QueryGraphValueEntryHashKeyedExpr expr = (QueryGraphValueEntryHashKeyedExpr) hashKey;
            return new IndexedTableLookupStrategySingleExpr(expr.getKeyExpr(), super.getLookupStream(), index,
                    new LookupStrategyDesc(LookupStrategyType.SINGLEEXPR, new String[]{ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(expr.getKeyExpr())}));
        } else if (hashKey instanceof QueryGraphValueEntryHashKeyedProp) {
            QueryGraphValueEntryHashKeyedProp prop = (QueryGraphValueEntryHashKeyedProp) hashKey;
            return new IndexedTableLookupStrategySingle(eventTypes[this.getLookupStream()], prop.getKeyProperty(), index);
        } else {
            throw new IllegalArgumentException("Invalid hashkey instance " + hashKey);
        }
    }

    public QueryGraphValueEntryHashKeyed getHashKey() {
        return hashKey;
    }

    public String toString() {
        return "IndexedTableLookupPlan " +
                super.toString() +
                " keyProperty=" + getKeyDescriptor();
    }
}
