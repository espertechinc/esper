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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategyProp implements SubordTableLookupStrategy {
    /**
     * Stream numbers to get key values from.
     */
    protected final int[] keyStreamNums;

    /**
     * Getters to use to get key values.
     */
    protected final EventPropertyGetter[] propertyGetters;

    /**
     * Index to look up in.
     */
    protected final PropertyIndexedEventTable index;

    protected final LookupStrategyDesc strategyDesc;

    public SubordIndexedTableLookupStrategyProp(int[] keyStreamNums, EventPropertyGetter[] propertyGetters, PropertyIndexedEventTable index, LookupStrategyDesc strategyDesc) {
        this.keyStreamNums = keyStreamNums;
        this.propertyGetters = propertyGetters;
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
            InstrumentationHelper.get().qIndexSubordLookup(this, index, keyStreamNums);
        }

        Object[] keys = getKeys(eventsPerStream);

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
     * @return key object
     */
    protected Object[] getKeys(EventBean[] eventsPerStream) {
        Object[] keyValues = new Object[propertyGetters.length];
        for (int i = 0; i < propertyGetters.length; i++) {
            int streamNum = keyStreamNums[i];
            EventBean theEvent = eventsPerStream[streamNum];
            keyValues[i] = propertyGetters[i].get(theEvent);
        }
        return keyValues;
    }

    public String toString() {
        return toQueryPlan();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " keyStreamNums=" + Arrays.toString(keyStreamNums);
    }
}
