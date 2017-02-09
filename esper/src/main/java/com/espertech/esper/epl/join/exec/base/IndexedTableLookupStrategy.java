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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.rep.Cursor;
import com.espertech.esper.epl.join.table.PropertyIndexedEventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.LookupStrategyType;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Arrays;
import java.util.Set;

/**
 * Lookup on an index using a set of properties as key values.
 */
public class IndexedTableLookupStrategy implements JoinExecTableLookupStrategy {
    private final EventType eventType;
    private final String[] properties;
    private final PropertyIndexedEventTable index;
    private final EventPropertyGetter[] propertyGetters;

    /**
     * Ctor.
     *
     * @param eventType  - event type to expect for lookup
     * @param properties - key properties
     * @param index      - index to look up in
     */
    public IndexedTableLookupStrategy(EventType eventType, String[] properties, PropertyIndexedEventTable index) {
        this.eventType = eventType;
        this.properties = properties;
        if (index == null) {
            throw new IllegalArgumentException("Unexpected null index received");
        }
        this.index = index;

        propertyGetters = new EventPropertyGetter[properties.length];
        for (int i = 0; i < properties.length; i++) {
            propertyGetters[i] = eventType.getGetter(properties[i]);

            if (propertyGetters[i] == null) {
                throw new IllegalArgumentException("Property named '" + properties[i] + "' is invalid for type " + eventType);
            }
        }
    }

    /**
     * Returns event type of the lookup event.
     *
     * @return event type of the lookup event
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns properties to use from lookup event to look up in index.
     *
     * @return properties to use from lookup event
     */
    public String[] getProperties() {
        return properties;
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

        Object[] keys = getKeys(theEvent);

        if (InstrumentationHelper.ENABLED) {
            Set<EventBean> result = index.lookup(keys);
            InstrumentationHelper.get().aIndexJoinLookup(result, keys);
            return result;
        }
        return index.lookup(keys);
    }

    private Object[] getKeys(EventBean theEvent) {
        return EventBeanUtility.getPropertyArray(theEvent, propertyGetters);
    }

    public String toString() {
        return "IndexedTableLookupStrategy indexProps=" + Arrays.toString(properties) +
                " index=(" + index + ')';
    }

    public LookupStrategyDesc getStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.MULTIPROP, properties);
    }
}
