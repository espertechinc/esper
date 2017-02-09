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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Set;

/**
 * Lookup on an index using a set of expression results as key values.
 */
public class InKeywordSingleTableLookupStrategyExpr implements JoinExecTableLookupStrategy {
    private final PropertyIndexedEventTableSingle index;
    private final int streamNum;
    private final EventBean[] eventsPerStream;
    private final ExprEvaluator[] evaluators;
    private final LookupStrategyDesc lookupStrategyDesc;

    public InKeywordSingleTableLookupStrategyExpr(ExprEvaluator[] evaluators, int streamNum, PropertyIndexedEventTableSingle index, LookupStrategyDesc lookupStrategyDesc) {
        if (index == null) {
            throw new IllegalArgumentException("Unexpected null index received");
        }
        this.index = index;
        this.streamNum = streamNum;
        this.eventsPerStream = new EventBean[streamNum + 1];
        this.evaluators = evaluators;
        this.lookupStrategyDesc = lookupStrategyDesc;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyIndexedEventTableSingle getIndex() {
        return index;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexJoinLookup(this, index);
        }

        eventsPerStream[streamNum] = theEvent;
        Set<EventBean> result = InKeywordTableLookupUtil.singleIndexLookup(evaluators, eventsPerStream, exprEvaluatorContext, index);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexJoinLookup(result, null);
        }
        return result;
    }

    public String toString() {
        return "IndexedTableLookupStrategyExpr expressions" +
                " index=(" + index + ')';
    }

    public LookupStrategyDesc getStrategyDesc() {
        return lookupStrategyDesc;
    }
}
