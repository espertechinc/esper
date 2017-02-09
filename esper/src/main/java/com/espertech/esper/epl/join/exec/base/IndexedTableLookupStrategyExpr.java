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
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Set;

/**
 * Lookup on an index using a set of expression results as key values.
 */
public class IndexedTableLookupStrategyExpr implements JoinExecTableLookupStrategy {
    private final PropertyIndexedEventTable index;
    private final int streamNum;
    private final EventBean[] eventsPerStream;
    private final ExprEvaluator[] evaluators;
    private final LookupStrategyDesc lookupStrategyDesc;

    public IndexedTableLookupStrategyExpr(ExprEvaluator[] evaluators, int streamNum, PropertyIndexedEventTable index, LookupStrategyDesc lookupStrategyDesc) {
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
    public PropertyIndexedEventTable getIndex() {
        return index;
    }

    public Set<EventBean> lookup(EventBean theEvent, Cursor cursor, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexJoinLookup(this, index);
        }

        Object[] keys = new Object[evaluators.length];
        eventsPerStream[streamNum] = theEvent;
        for (int i = 0; i < evaluators.length; i++) {
            keys[i] = evaluators[i].evaluate(eventsPerStream, true, exprEvaluatorContext);
        }

        if (InstrumentationHelper.ENABLED) {
            Set<EventBean> result = index.lookup(keys);
            InstrumentationHelper.get().aIndexJoinLookup(result, keys);
            return result;
        }
        return index.lookup(keys);
    }

    public String toString() {
        return "IndexedTableLookupStrategyExpr expressions" +
                " index=(" + index + ')';
    }

    public LookupStrategyDesc getStrategyDesc() {
        return lookupStrategyDesc;
    }
}
