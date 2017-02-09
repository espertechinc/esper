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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Set;

/**
 * Index that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 */
public abstract class PropertyIndexedEventTableSingle implements EventTable {
    protected final EventPropertyGetter propertyGetter;
    protected final EventTableOrganization organization;

    public abstract Set<EventBean> lookup(Object key);

    public PropertyIndexedEventTableSingle(EventPropertyGetter propertyGetter, EventTableOrganization organization) {
        this.propertyGetter = propertyGetter;
        this.organization = organization;
    }

    /**
     * Determine multikey for index access.
     *
     * @param theEvent to get properties from for key
     * @return multi key
     */
    protected Object getKey(EventBean theEvent) {
        return propertyGetter.get(theEvent);
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);
        }
        if (newData != null) {
            for (EventBean theEvent : newData) {
                add(theEvent);
            }
        }
        if (oldData != null) {
            for (EventBean theEvent : oldData) {
                remove(theEvent);
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexAddRemove();
        }
    }

    /**
     * Add an array of events. Same event instance is not added twice. Event properties should be immutable.
     * Allow null passed instead of an empty array.
     *
     * @param events to add
     * @throws IllegalArgumentException if the event was already existed in the index
     */
    public void add(EventBean[] events) {
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

    /**
     * Remove events.
     *
     * @param events to be removed, can be null instead of an empty array.
     * @throws IllegalArgumentException when the event could not be removed as its not in the index
     */
    public void remove(EventBean[] events) {
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

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + organization.getStreamNum() +
                " propertyGetter=" + propertyGetter;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }
}
