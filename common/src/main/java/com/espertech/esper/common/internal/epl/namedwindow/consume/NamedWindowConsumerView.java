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
package com.espertech.esper.common.internal.epl.namedwindow.consume;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.util.FilteredEventIterator;
import com.espertech.esper.common.internal.event.core.FlushedEventBuffer;
import com.espertech.esper.common.internal.filterspec.PropertyEvaluator;
import com.espertech.esper.common.internal.view.core.ViewSupport;

import java.util.Iterator;

/**
 * Represents a consumer of a named window that selects from a named window via a from-clause.
 * <p>
 * The view simply dispatches directly to child views, and keeps the last new event for iteration.
 */
public class NamedWindowConsumerView extends ViewSupport implements AgentInstanceStopCallback {
    private final int namedWindowConsumerId;
    private final ExprEvaluator filter;
    private final EventType eventType;
    private final NamedWindowConsumerCallback consumerCallback;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final PropertyEvaluator optPropertyEvaluator;
    private final FlushedEventBuffer optPropertyContainedBuffer;
    private final boolean audit;

    public NamedWindowConsumerView(int namedWindowConsumerId,
                                   ExprEvaluator filter,
                                   PropertyEvaluator optPropertyEvaluator,
                                   EventType eventType,
                                   NamedWindowConsumerCallback consumerCallback,
                                   ExprEvaluatorContext exprEvaluatorContext,
                                   boolean audit) {
        this.namedWindowConsumerId = namedWindowConsumerId;
        this.filter = filter;
        this.optPropertyEvaluator = optPropertyEvaluator;
        if (optPropertyEvaluator != null) {
            optPropertyContainedBuffer = new FlushedEventBuffer();
        } else {
            optPropertyContainedBuffer = null;
        }
        this.eventType = eventType;
        this.consumerCallback = consumerCallback;
        this.exprEvaluatorContext = exprEvaluatorContext;
        this.audit = audit;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (audit) {
            exprEvaluatorContext.getAuditProvider().stream(newData, oldData, exprEvaluatorContext, eventType.getName());
        }

        // if we have a filter for the named window,
        if (filter != null) {
            EventBean[] eventPerStream = new EventBean[1];
            newData = passFilter(newData, true, exprEvaluatorContext, eventPerStream);
            oldData = passFilter(oldData, false, exprEvaluatorContext, eventPerStream);
        }

        if (optPropertyEvaluator != null) {
            newData = getUnpacked(newData);
            oldData = getUnpacked(oldData);
        }

        if ((newData != null) || (oldData != null)) {
            if (child != null) {
                child.update(newData, oldData);
            }
        }
    }

    private EventBean[] getUnpacked(EventBean[] data) {
        if (data == null) {
            return null;
        }
        if (data.length == 0) {
            return data;
        }

        for (int i = 0; i < data.length; i++) {
            EventBean[] unpacked = optPropertyEvaluator.getProperty(data[i], exprEvaluatorContext);
            optPropertyContainedBuffer.add(unpacked);
        }
        return optPropertyContainedBuffer.getAndFlush();
    }

    private EventBean[] passFilter(EventBean[] eventData, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext, EventBean[] eventPerStream) {
        if ((eventData == null) || (eventData.length == 0)) {
            return null;
        }

        if (eventData.length == 1) {
            eventPerStream[0] = eventData[0];
            Boolean result = (Boolean) filter.evaluate(eventPerStream, isNewData, exprEvaluatorContext);
            return result != null && result ? eventData : null;
        }

        OneEventCollection filtered = null;
        for (EventBean theEvent : eventData) {
            eventPerStream[0] = theEvent;
            Boolean result = (Boolean) filter.evaluate(eventPerStream, isNewData, exprEvaluatorContext);
            if (result == null || !result) {
                continue;
            }

            if (filtered == null) {
                filtered = new OneEventCollection();
            }
            filtered.add(theEvent);
        }

        if (filtered == null) {
            return null;
        }
        return filtered.toArray();
    }

    public EventType getEventType() {
        if (optPropertyEvaluator != null) {
            return optPropertyEvaluator.getFragmentEventType();
        }
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        if (filter == null) {
            return consumerCallback.getIterator();
        }
        return new FilteredEventIterator(filter, consumerCallback.getIterator(), exprEvaluatorContext);
    }

    public void stop(AgentInstanceStopServices services) {
        consumerCallback.stopped(this);
    }

    public int getNamedWindowConsumerId() {
        return namedWindowConsumerId;
    }

    public NamedWindowConsumerCallback getConsumerCallback() {
        return consumerCallback;
    }

    public ExprEvaluator getFilter() {
        return filter;
    }
}
