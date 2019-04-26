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
package com.espertech.esper.common.internal.epl.index.composite;


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.join.exec.composite.CompositeIndexQueryResultPostProcessor;

import java.util.Map;

public abstract class PropertyCompositeEventTable implements EventTable {
    protected final PropertyCompositeEventTableFactory factory;

    public abstract Map<Object, Object> getIndex();

    public abstract CompositeIndexQueryResultPostProcessor getPostProcessor();

    public PropertyCompositeEventTable(PropertyCompositeEventTableFactory factory) {
        this.factory = factory;
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

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        return this.getClass().getName();
    }

    public Integer getNumberOfEvents() {
        return null;
    }

    public EventTableOrganization getOrganization() {
        return factory.getOrganization();
    }

    public Class[] getOptKeyCoercedTypes() {
        return factory.optKeyCoercedTypes;
    }

    public Class[] getOptRangeCoercedTypes() {
        return factory.optRangeCoercedTypes;
    }

    public MultiKeyFromObjectArray getMultiKeyTransform() {
        return factory.transformFireAndForget;
    }
}
