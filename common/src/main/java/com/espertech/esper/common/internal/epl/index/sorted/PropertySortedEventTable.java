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
package com.espertech.esper.common.internal.epl.index.sorted;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.join.exec.util.RangeIndexLookupValue;

import java.util.*;

/**
 * Index that organizes events by the event property values into a single TreeMap sortable non-nested index
 * with Object keys that store the property values.
 */
public abstract class PropertySortedEventTable implements EventTable {
    protected final PropertySortedEventTableFactory factory;

    public abstract Set<EventBean> lookupRange(Object keyStart, boolean includeStart, Object keyEnd, boolean includeEnd, boolean allowRangeReversal);

    public abstract Collection<EventBean> lookupRangeColl(Object keyStart, boolean includeStart, Object keyEnd, boolean includeEnd, boolean allowRangeReversal);

    public abstract Set<EventBean> lookupRangeInverted(Object keyStart, boolean includeStart, Object keyEnd, boolean includeEnd);

    public abstract Collection<EventBean> lookupRangeInvertedColl(Object keyStart, boolean includeStart, Object keyEnd, boolean includeEnd);

    public abstract Set<EventBean> lookupLess(Object keyStart);

    public abstract Collection<EventBean> lookupLessThenColl(Object keyStart);

    public abstract Set<EventBean> lookupLessEqual(Object keyStart);

    public abstract Collection<EventBean> lookupLessEqualColl(Object keyStart);

    public abstract Set<EventBean> lookupGreaterEqual(Object keyStart);

    public abstract Collection<EventBean> lookupGreaterEqualColl(Object keyStart);

    public abstract Set<EventBean> lookupGreater(Object keyStart);

    public abstract Collection<EventBean> lookupGreaterColl(Object keyStart);

    public abstract Set<EventBean> lookupConstants(RangeIndexLookupValue lookupValueBase);

    public PropertySortedEventTable(PropertySortedEventTableFactory factory) {
        this.factory = factory;
    }

    /**
     * Determine multikey for index access.
     *
     * @param theEvent to get properties from for key
     * @return multi key
     */
    protected Object getIndexedValue(EventBean theEvent) {
        return factory.propertyGetter.get(theEvent);
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        exprEvaluatorContext.getInstrumentationProvider().qIndexAddRemove(this, newData, oldData);

        if (newData != null) {
            for (EventBean theEvent : newData) {
                add(theEvent, exprEvaluatorContext);
            }
        }
        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                remove(theEvent, exprEvaluatorContext);
            }
        }

        exprEvaluatorContext.getInstrumentationProvider().aIndexAddRemove();
    }

    /**
     * Add an array of events. Same event instance is not added twice. Event properties should be immutable.
     * Allow null passed instead of an empty array.
     *
     * @param events               to add
     * @param exprEvaluatorContext evaluator context
     * @throws IllegalArgumentException if the event was already existed in the index
     */
    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {
            for (EventBean theEvent : events) {
                add(theEvent, exprEvaluatorContext);
            }
        }
    }

    /**
     * Remove events.
     *
     * @param events               to be removed, can be null instead of an empty array.
     * @param exprEvaluatorContext evaluator context
     * @throws IllegalArgumentException when the event could not be removed as its not in the index
     */
    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {
            for (EventBean theEvent : events) {
                remove(theEvent, exprEvaluatorContext);
            }
        }
    }


    public Integer getNumberOfEvents() {
        return null;
    }

    protected static Set<EventBean> normalize(SortedMap<Object, Set<EventBean>> submap) {
        if (submap.size() == 0) {
            return null;
        }
        if (submap.size() == 1) {
            return submap.get(submap.firstKey());
        }
        Set<EventBean> result = new LinkedHashSet<EventBean>();
        for (Map.Entry<Object, Set<EventBean>> entry : submap.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    protected static Collection<EventBean> normalizeCollection(SortedMap<Object, Set<EventBean>> submap) {
        if (submap.size() == 0) {
            return null;
        }
        if (submap.size() == 1) {
            return submap.get(submap.firstKey());
        }
        Deque<EventBean> result = new ArrayDeque<EventBean>();
        for (Map.Entry<Object, Set<EventBean>> entry : submap.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    protected static Collection<EventBean> normalizeCollection(SortedMap<Object, Set<EventBean>> submapOne, SortedMap<Object, Set<EventBean>> submapTwo) {
        if (submapOne.size() == 0) {
            return normalizeCollection(submapTwo);
        }
        if (submapTwo.size() == 0) {
            return normalizeCollection(submapOne);
        }
        ArrayDeque<EventBean> result = new ArrayDeque<EventBean>();
        for (Map.Entry<Object, Set<EventBean>> entry : submapOne.entrySet()) {
            result.addAll(entry.getValue());
        }
        for (Map.Entry<Object, Set<EventBean>> entry : submapTwo.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    protected static Set<EventBean> normalize(SortedMap<Object, Set<EventBean>> submapOne, SortedMap<Object, Set<EventBean>> submapTwo) {
        if (submapOne.size() == 0) {
            return normalize(submapTwo);
        }
        if (submapTwo.size() == 0) {
            return normalize(submapOne);
        }
        Set<EventBean> result = new LinkedHashSet<EventBean>();
        for (Map.Entry<Object, Set<EventBean>> entry : submapOne.entrySet()) {
            result.addAll(entry.getValue());
        }
        for (Map.Entry<Object, Set<EventBean>> entry : submapTwo.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + factory.getOrganization().getStreamNum();
    }

    public EventTableOrganization getOrganization() {
        return factory.getOrganization();
    }

    public Class getValueType() {
        return factory.valueType;
    }
}
