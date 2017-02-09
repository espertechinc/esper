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
package com.espertech.esper.epl.join.exec.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.plan.InKeywordTableLookupUtil;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;

import java.util.Set;

/**
 * Lookup on an index using a set of expression results as key values.
 */
public class InKeywordMultiTableLookupStrategyExpr implements JoinExecTableLookupStrategy {
    private final PropertyIndexedEventTableSingle[] indexes;
    private final int streamNum;
    private final EventBean[] eventsPerStream;
    private final ExprEvaluator evaluator;
    private final LookupStrategyDesc lookupStrategyDesc;

    public InKeywordMultiTableLookupStrategyExpr(ExprEvaluator evaluator, int streamNum, PropertyIndexedEventTableSingle[] indexes, LookupStrategyDesc lookupStrategyDesc) {
        if (indexes == null) {
            throw new IllegalArgumentException("Unexpected null index received");
        }
        this.indexes = indexes;
        this.streamNum = streamNum;
        this.eventsPerStream = new EventBean[streamNum + 1];
        this.evaluator = evaluator;
        this.lookupStrategyDesc = lookupStrategyDesc;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyIndexedEventTableSingle[] getIndex() {
        return indexes;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        eventsPerStream[streamNum] = theEvent;
        return InKeywordTableLookupUtil.multiIndexLookup(evaluator, eventsPerStream, exprEvaluatorContext, indexes);
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + lookupStrategyDesc.toString();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return lookupStrategyDesc;
    }
}
