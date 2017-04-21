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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

public class PropertyIndexedEventTableUnique extends PropertyIndexedEventTable implements EventTableAsSet {
    protected final Map<MultiKeyUntyped, EventBean> propertyIndex;
    private final boolean canClear;

    public PropertyIndexedEventTableUnique(EventPropertyGetter[] propertyGetters, EventTableOrganization organization) {
        super(propertyGetters, organization);
        propertyIndex = new HashMap<MultiKeyUntyped, EventBean>();
        this.canClear = true;
    }

    public PropertyIndexedEventTableUnique(EventPropertyGetter[] propertyGetters, EventTableOrganization organization, Map<MultiKeyUntyped, EventBean> propertyIndex) {
        super(propertyGetters, organization);
        this.propertyIndex = propertyIndex;
        this.canClear = false;
    }

    /**
     * Remove then add events.
     *  @param newData to add
     * @param oldData to remove
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

    public Set<EventBean> lookup(Object[] keys) {
        MultiKeyUntyped key = new MultiKeyUntyped(keys);
        EventBean event = propertyIndex.get(key);
        if (event != null) {
            return Collections.singleton(event);
        }
        return null;
    }

    public void add(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        MultiKeyUntyped key = getMultiKey(theEvent);

        EventBean existing = propertyIndex.put(key, theEvent);
        if (existing != null && !existing.equals(theEvent)) {
            throw handleUniqueIndexViolation(organization.getIndexName(), key);
        }
    }

    public static EPException handleUniqueIndexViolation(String indexName, Object key) {
        String indexNameDisplay = indexName == null ? "" : " '" + indexName + "'";
        throw new EPException("Unique index violation, index" + indexNameDisplay + " is a unique index and key '" + key + "' already exists");
    }

    public void remove(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        MultiKeyUntyped key = getMultiKey(theEvent);
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

    public Integer getNumberOfEvents() {
        return propertyIndex.size();
    }

    public int getNumKeys() {
        return propertyIndex.size();
    }

    public Object getIndex() {
        return propertyIndex;
    }

    public Set<EventBean> allValues() {
        if (propertyIndex.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<EventBean>(propertyIndex.values());
    }

    public Class getProviderClass() {
        return PropertyIndexedEventTableUnique.class;
    }
}
