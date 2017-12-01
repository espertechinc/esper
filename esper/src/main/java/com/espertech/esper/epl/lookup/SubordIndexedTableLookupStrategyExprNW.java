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
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategyExprNW implements SubordTableLookupStrategy {
    /**
     * Index to look up in.
     */
    protected final PropertyIndexedEventTable index;

    protected final ExprEvaluator[] evaluators;

    protected final LookupStrategyDesc strategyDesc;

    public SubordIndexedTableLookupStrategyExprNW(ExprEvaluator[] evaluators, PropertyIndexedEventTable index, LookupStrategyDesc strategyDesc) {
        this.evaluators = evaluators;
        this.index = index;
        this.strategyDesc = strategyDesc;
    }

    /**
     * Returns index to look up in.
     *
     * @return index to use
     */
    public PropertyIndexedEventTable getIndex() {
        return index;
    }

    public Collection<EventBean> lookup(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexSubordLookup(this, index, null);
        }

        Object[] keys = getKeys(eventsPerStream, context);

        if (InstrumentationHelper.ENABLED) {
            Set<EventBean> result = index.lookup(keys);
            InstrumentationHelper.get().aIndexSubordLookup(result, keys);
            return result;
        }
        return index.lookup(keys);
    }

    /**
     * Get the index lookup keys.
     *
     * @param eventsPerStream is the events for each stream
     * @param context         context
     * @return key object
     */
    protected Object[] getKeys(EventBean[] eventsPerStream, ExprEvaluatorContext context) {
        Object[] keyValues = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            keyValues[i] = evaluators[i].evaluate(eventsPerStream, true, context);
        }
        return keyValues;
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " evaluators " + ExprNodeUtilityCore.printEvaluators(evaluators);
    }
}
