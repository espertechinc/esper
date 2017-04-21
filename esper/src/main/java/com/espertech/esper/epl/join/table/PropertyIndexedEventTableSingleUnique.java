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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Unique index.
 */
public class PropertyIndexedEventTableSingleUnique extends PropertyIndexedEventTableSingle implements EventTableAsSet {
    private final Map<Object, EventBean> propertyIndex;
    private final boolean canClear;

    public PropertyIndexedEventTableSingleUnique(EventPropertyGetter propertyGetter, EventTableOrganization organization) {
        super(propertyGetter, organization);
        propertyIndex = new HashMap<Object, EventBean>();
        canClear = true;
    }

    public PropertyIndexedEventTableSingleUnique(EventPropertyGetter propertyGetter, EventTableOrganization organization, Map<Object, EventBean> propertyIndex) {
        super(propertyGetter, organization);
        this.propertyIndex = propertyIndex;
        canClear = false;
    }

    public Set<EventBean> lookup(Object key) {
        EventBean event = propertyIndex.get(key);
        if (event != null) {
            return Collections.singleton(event);
        }
        return null;
    }

    public int getNumKeys() {
        return propertyIndex.size();
    }

    public Object getIndex() {
        return propertyIndex;
    }

    /**
     * Remove then add events.
     *
     * @param newData              to add
     * @param oldData              to remove
     * @param exprEvaluatorContext evaluator context
     */
    @Override
    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);
        }
        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                remove(theEvent, exprEvaluatorContext);
            }
        }
        if (newData != null) {
            for (EventBean theEvent : newData) {
                add(theEvent, exprEvaluatorContext);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexAddRemove();
        }
    }

    public void add(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        Object key = getKey(theEvent);

        EventBean existing = propertyIndex.put(key, theEvent);
        if (existing != null && !existing.equals(theEvent)) {
            throw PropertyIndexedEventTableUnique.handleUniqueIndexViolation(organization.getIndexName(), key);
        }
    }

    public void remove(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        Object key = getKey(theEvent);
        propertyIndex.remove(key);
    }

    public boolean isEmpty() {
        return propertyIndex.isEmpty();
    }

    public Iterator<EventBean> iterator() {
        return propertyIndex.values().iterator();
    }

    public void clear() {
        if (canClear) {
            propertyIndex.clear();
        }
    }

    public void destroy() {
        clear();
    }

    public String toString() {
        return toQueryPlan();
    }

    public Integer getNumberOfEvents() {
        return propertyIndex.size();
    }

    public Set<EventBean> allValues() {
        if (propertyIndex.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<EventBean>(propertyIndex.values());
    }

    public Class getProviderClass() {
        return PropertyIndexedEventTableSingleUnique.class;
    }
}
