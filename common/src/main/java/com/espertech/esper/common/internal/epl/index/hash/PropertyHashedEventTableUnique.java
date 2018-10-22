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
package com.espertech.esper.common.internal.epl.index.hash;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTableAsSet;

import java.util.*;

/**
 * Unique index.
 */
public class PropertyHashedEventTableUnique extends PropertyHashedEventTable implements EventTableAsSet {
    private final Map<Object, EventBean> propertyIndex;

    public PropertyHashedEventTableUnique(PropertyHashedEventTableFactory factory) {
        super(factory);
        propertyIndex = new HashMap<>();
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
        exprEvaluatorContext.getInstrumentationProvider().qIndexAddRemove(this, newData, oldData);

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

        exprEvaluatorContext.getInstrumentationProvider().aIndexAddRemove();
    }

    public void add(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        Object key = getKey(theEvent);

        EventBean existing = propertyIndex.put(key, theEvent);
        if (existing != null && !existing.equals(theEvent)) {
            throw handleUniqueIndexViolation(factory.getOrganization().getIndexName(), key);
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
        propertyIndex.clear();
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
        return PropertyHashedEventTableUnique.class;
    }

    public Map<Object, EventBean> getPropertyIndex() {
        return propertyIndex;
    }

    public static EPException handleUniqueIndexViolation(String indexName, Object key) {
        String indexNameDisplay = indexName == null ? "" : " '" + indexName + "'";
        throw new EPException("Unique index violation, index" + indexNameDisplay + " is a unique index and key '" + key + "' already exists");
    }
}
