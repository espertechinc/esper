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
import com.espertech.esper.epl.join.table.PropertyIndexedEventTableSingle;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Collection;
import java.util.Set;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordIndexedTableLookupStrategySingleProp implements SubordTableLookupStrategy {
    /**
     * Stream numbers to get key values from.
     */
    protected final int keyStreamNum;

    /**
     * Getters to use to get key values.
     */
    protected final EventPropertyGetter propertyGetter;

    /**
     * Index to look up in.
     */
    protected final PropertyIndexedEventTableSingle index;

    protected final LookupStrategyDesc strategyDesc;

    public SubordIndexedTableLookupStrategySingleProp(int keyStreamNum, EventPropertyGetter propertyGetter, PropertyIndexedEventTableSingle index, LookupStrategyDesc strategyDesc) {
        this.keyStreamNum = keyStreamNum;
        this.propertyGetter = propertyGetter;
        this.index = index;
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
            InstrumentationHelper.get().qIndexSubordLookup(this, index, new int[]{keyStreamNum});
        }

        Object key = getKey(eventsPerStream);

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
     * @return key object
     */
    protected Object getKey(EventBean[] eventsPerStream) {
        EventBean theEvent = eventsPerStream[keyStreamNum];
        return propertyGetter.get(theEvent);
    }

    public String toString() {
        return toQueryPlan();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return strategyDesc;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() + " stream=" + keyStreamNum;
    }
}
