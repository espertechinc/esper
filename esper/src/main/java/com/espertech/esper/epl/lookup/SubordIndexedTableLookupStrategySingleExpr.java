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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategySingleExpr implements SubordTableLookupStrategy {
    /**
     * Stream numbers to get key values from.
     */
    protected final ExprEvaluator evaluator;

    private final EventBean[] events;

    private final LookupStrategyDesc strategyDesc;

    /**
     * Index to look up in.
     */
    protected final PropertyIndexedEventTableSingle index;

    public SubordIndexedTableLookupStrategySingleExpr(int streamCountOuter, ExprEvaluator evaluator, PropertyIndexedEventTableSingle index, LookupStrategyDesc strategyDesc) {
        this.evaluator = evaluator;
        this.index = index;
        this.events = new EventBean[streamCountOuter + 1];
        this.strategyDesc = strategyDesc;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyIndexedEventTableSingle getIndex() {
        return index;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexSubordLookup(this, index, null);
        }

        Object key = getKey(eventsPerStream, context);

        if (InstrumentationHelper.ENABLED) {
            Set<EventBean> result = index.lookup(key);
            InstrumentationHelper.get().aIndexSubordLookup(result, key);
            return result;
        }
        return index.lookup(key);
    }

    /**
     * Get the index lookup keys.
     *
     * @param eventsPerStream is the events for each stream
     * @param context         context
     * @return key object
     */
    protected Object getKey(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        System.arraycopy(eventsPerStream, 0, events, 1, eventsPerStream.length);
        return evaluator.evaluate(events, true, context);
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " evaluator " + evaluator.getClass().getSimpleName();
    }
}
