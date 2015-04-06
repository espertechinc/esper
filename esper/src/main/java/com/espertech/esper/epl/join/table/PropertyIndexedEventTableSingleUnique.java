/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.*;

/**
 * Unique index.
 */
public class PropertyIndexedEventTableSingleUnique extends PropertyIndexedEventTableSingle implements EventTableAsSet
{
    private final Map<Object, EventBean> propertyIndex;
    private final boolean canClear;

    public PropertyIndexedEventTableSingleUnique(EventPropertyGetter propertyGetter, EventTableOrganization organization)
    {
        super(propertyGetter, organization, false);
        propertyIndex = new HashMap<Object, EventBean>();
        canClear = true;
    }

    public PropertyIndexedEventTableSingleUnique(EventPropertyGetter propertyGetter, EventTableOrganization organization, Map<Object, EventBean> propertyIndex) {
        super(propertyGetter, organization, false);
        this.propertyIndex = propertyIndex;
        canClear = false;
    }

    /**
     * Remove then add events.
     * @param newData to add
     * @param oldData to remove
     */
    @Override
    public void addRemove(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);}
        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                remove(theEvent);
            }
        }
        if (newData != null) {
            for (EventBean theEvent : newData) {
                add(theEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aIndexAddRemove();}
    }

    @Override
    public void add(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexAdd(this, events);
                for (EventBean theEvent : events) {
                    add(theEvent);
                }
                InstrumentationHelper.get().aIndexAdd();
                return;
            }

            for (EventBean theEvent : events) {
                add(theEvent);
            }
        }
    }

    @Override
    public void remove(EventBean[] events)
    {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexRemove(this, events);
                for (EventBean theEvent : events) {
                    remove(theEvent);
                }
                InstrumentationHelper.get().aIndexRemove();
                return;
            }

            for (EventBean theEvent : events) {
                remove(theEvent);
            }
        }
    }

    @Override
    public Set<EventBean> lookup(Object key)
    {
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

    public void add(EventBean theEvent)
    {
        Object key = getKey(theEvent);

        EventBean existing = propertyIndex.put(key, theEvent);
        if (existing != null && !existing.equals(theEvent)) {
            throw PropertyIndexedEventTableUnique.handleUniqueIndexViolation(organization.getIndexName(), key);
        }
    }

    public void remove(EventBean theEvent)
    {
        Object key = getKey(theEvent);
        propertyIndex.remove(key);
    }

    public boolean isEmpty()
    {
        return propertyIndex.isEmpty();
    }

    @Override
    public Iterator<EventBean> iterator()
    {
        return propertyIndex.values().iterator();
    }

    public void clear()
    {
        if (canClear) {
            propertyIndex.clear();
        }
    }

    public String toString()
    {
        return toQueryPlan();
    }

    @Override
    public Integer getNumberOfEvents() {
        return propertyIndex.size();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + organization.getStreamNum() +
                " propertyGetter=" + propertyGetter;
    }

    public Set<EventBean> allValues() {
        if (propertyIndex.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<EventBean>(propertyIndex.values());
    }
}
