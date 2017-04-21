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
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQueryResultPostProcessor;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.util.Map;

public abstract class PropertyCompositeEventTable implements EventTable {
    private final Class[] optKeyCoercedTypes;
    private final Class[] optRangeCoercedTypes;
    private final EventTableOrganization organization;

    public abstract Map<Object, Object> getIndex();

    public abstract CompositeIndexQueryResultPostProcessor getPostProcessor();

    public PropertyCompositeEventTable(Class[] optKeyCoercedTypes, Class[] optRangeCoercedTypes, EventTableOrganization organization) {
        this.optKeyCoercedTypes = optKeyCoercedTypes;
        this.optRangeCoercedTypes = optRangeCoercedTypes;
        this.organization = organization;
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

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public Class[] getOptRangeCoercedTypes() {
        return optRangeCoercedTypes;
    }

    public Class[] getOptKeyCoercedTypes() {
        return optKeyCoercedTypes;
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public EventTableOrganization getOrganization() {
        return organization;
    }
}
