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
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

public class PropertyIndexedEventTableUnadorned extends PropertyIndexedEventTable {
    protected final Map<MultiKeyUntyped, Set<EventBean>> propertyIndex;

    public PropertyIndexedEventTableUnadorned(EventPropertyGetter[] propertyGetters, EventTableOrganization organization) {
        super(propertyGetters, organization);
        propertyIndex = new HashMap<MultiKeyUntyped, Set<EventBean>>();
    }

    /**
     * Returns the set of events that have the same property value as the given event.
     *
     * @param keys to compare against
     * @return set of events with property value, or null if none found (never returns zero-sized set)
     */
    public Set<EventBean> lookup(Object[] keys) {
        MultiKeyUntyped key = new MultiKeyUntyped(keys);
        return propertyIndex.get(key);
    }

    public void add(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        MultiKeyUntyped key = getMultiKey(theEvent);

        Set<EventBean> events = propertyIndex.get(key);
        if (events == null) {
            events = new LinkedHashSet<EventBean>();
            propertyIndex.put(key, events);
        }

        events.add(theEvent);
    }

    public void remove(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        MultiKeyUntyped key = getMultiKey(theEvent);

        Set<EventBean> events = propertyIndex.get(key);
        if (events == null) {
            return;
        }

        if (!events.remove(theEvent)) {
            // Not an error, its possible that an old-data event is artificial (such as for statistics) and
            // thus did not correspond to a new-data event raised earlier.
            return;
        }

        if (events.isEmpty()) {
            propertyIndex.remove(key);
        }
    }

    public boolean isEmpty() {
        return propertyIndex.isEmpty();
    }

    public Iterator<EventBean> iterator() {
        return new PropertyIndexedEventTableIterator<MultiKeyUntyped>(propertyIndex);
    }

    public void clear() {
        propertyIndex.clear();
    }

    public void destroy() {
        clear();
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public int getNumKeys() {
        return propertyIndex.size();
    }

    public Object getIndex() {
        return propertyIndex;
    }

    public Class getProviderClass() {
        return PropertyIndexedEventTable.class;
    }
}
