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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.IndexedTableLookupStrategyExpr;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;

import java.util.Collections;
import java.util.List;

/**
 * Plan to perform an indexed table lookup.
 */
public class IndexedTableLookupPlanMulti extends TableLookupPlan {
    private List<QueryGraphValueEntryHashKeyed> keyProperties;

    /**
     * Ctor.
     *
     * @param lookupStream  - stream that generates event to look up for
     * @param indexedStream - stream to index table lookup
     * @param indexNum      - index number for the table containing the full unindexed contents
     * @param keyProperties - properties to use in lookup event to access index
     */
    public IndexedTableLookupPlanMulti(int lookupStream, int indexedStream, TableLookupIndexReqKey indexNum, List<QueryGraphValueEntryHashKeyed> keyProperties) {
        super(lookupStream, indexedStream, new TableLookupIndexReqKey[]{indexNum});
        this.keyProperties = keyProperties;
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(keyProperties, Collections.<QueryGraphValueEntryRange>emptyList());
    }

    public JoinExecTableLookupStrategy makeStrategyInternal(EventTable[] eventTable, EventType[] eventTypes) {
        PropertyIndexedEventTable index = (PropertyIndexedEventTable) eventTable[0];
        String[] keyProps = new String[keyProperties.size()];
        ExprEvaluator[] evaluators = new ExprEvaluator[keyProperties.size()];
        String[] expressions = new String[keyProperties.size()];
        boolean isStrictlyProps = true;
        for (int i = 0; i < keyProps.length; i++) {
            isStrictlyProps = isStrictlyProps && keyProperties.get(i) instanceof QueryGraphValueEntryHashKeyedProp;
            evaluators[i] = keyProperties.get(i).getKeyExpr().getForge().getExprEvaluator();
            expressions[i] = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(keyProperties.get(i).getKeyExpr());

            if (keyProperties.get(i) instanceof QueryGraphValueEntryHashKeyedProp) {
                keyProps[i] = ((QueryGraphValueEntryHashKeyedProp) keyProperties.get(i)).getKeyProperty();
            } else {
                isStrictlyProps = false;
            }
        }
        if (isStrictlyProps) {
            return new IndexedTableLookupStrategy(eventTypes[this.getLookupStream()], keyProps, index);
        } else {
            return new IndexedTableLookupStrategyExpr(evaluators, getLookupStream(), index, new LookupStrategyDesc(LookupStrategyType.MULTIEXPR, expressions));
        }
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " +
                super.toString() +
                " keyProperties=" + QueryGraphValueEntryHashKeyed.toQueryPlan(keyProperties);
    }
}
