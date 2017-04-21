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
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Arrays;
import java.util.Set;

/**
 * Index that organizes events by the event property values into hash buckets. Based on a HashMap
 * with {@link com.espertech.esper.collection.MultiKeyUntyped} keys that store the property values.
 * <p>
 * Takes a list of property names as parameter. Doesn't care which event type the events have as long as the properties
 * exist. If the same event is added twice, the class throws an exception on add.
 */
public abstract class PropertyIndexedEventTable implements EventTable {
    protected final EventPropertyGetter[] propertyGetters;
    protected final EventTableOrganization organization;

    public abstract Set<EventBean> lookup(Object[] keys);

    public PropertyIndexedEventTable(EventPropertyGetter[] propertyGetters, EventTableOrganization organization) {
        this.propertyGetters = propertyGetters;
        this.organization = organization;
    }

    /**
     * Determine multikey for index access.
     *
     * @param theEvent to get properties from for key
     * @return multi key
     */
    protected MultiKeyUntyped getMultiKey(EventBean theEvent) {
        return EventBeanUtility.getMultiKey(theEvent, propertyGetters);
    }

    public void addRemove(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qIndexAddRemove(this, newData, oldData);
        }
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aIndexAddRemove();
        }
    }

    /**
     * Add an array of events. Same event instance is not added twice. Event properties should be immutable.
     * Allow null passed instead of an empty array.
     *
     * @param events to add
     * @param exprEvaluatorContext evaluator context
     * @throws IllegalArgumentException if the event was already existed in the index
     */
    public void add(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexAdd(this, events);
                for (EventBean theEvent : events) {
                    add(theEvent, exprEvaluatorContext);
                }
                InstrumentationHelper.get().aIndexAdd();
                return;
            }

            for (EventBean theEvent : events) {
                add(theEvent, exprEvaluatorContext);
            }
        }
    }

    /**
     * Remove events.
     *
     * @param events to be removed, can be null instead of an empty array.
     * @param exprEvaluatorContext evaluator context
     * @throws IllegalArgumentException when the event could not be removed as its not in the index
     */
    public void remove(EventBean[] events, ExprEvaluatorContext exprEvaluatorContext) {
        if (events != null) {

            if (InstrumentationHelper.ENABLED && events.length > 0) {
                InstrumentationHelper.get().qIndexRemove(this, events);
                for (EventBean theEvent : events) {
                    remove(theEvent, exprEvaluatorContext);
                }
                InstrumentationHelper.get().aIndexRemove();
                return;
            }

            for (EventBean theEvent : events) {
                remove(theEvent, exprEvaluatorContext);
            }
        }
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " streamNum=" + organization.getStreamNum() +
                " propertyGetters=" + Arrays.toString(propertyGetters);
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }
}
