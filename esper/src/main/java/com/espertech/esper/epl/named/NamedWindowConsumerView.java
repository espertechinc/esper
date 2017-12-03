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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.filterspec.PropertyEvaluator;
import com.espertech.esper.epl.util.FilteredEventIterator;
import com.espertech.esper.event.FlushedEventBuffer;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.EventBeanSummarizer;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.ViewSupport;

import java.util.Iterator;

/**
 * Represents a consumer of a named window that selects from a named window via a from-clause.
 * <p>
 * The view simply dispatches directly to child views, and keeps the last new event for iteration.
 */
public class NamedWindowConsumerView extends ViewSupport implements StopCallback {
    private final ExprEvaluator[] filterList;
    private final EventType eventType;
    private final NamedWindowConsumerCallback consumerCallback;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final PropertyEvaluator optPropertyEvaluator;
    private final FlushedEventBuffer optPropertyContainedBuffer;
    private final boolean audit;

    public NamedWindowConsumerView(ExprEvaluator[] filterList,
                                   PropertyEvaluator optPropertyEvaluator,
                                   EventType eventType,
                                   NamedWindowConsumerCallback consumerCallback,
                                   ExprEvaluatorContext exprEvaluatorContext,
                                   boolean audit) {
        this.filterList = filterList;
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
            if (AuditPath.isInfoEnabled()) {
                AuditPath.auditLog(exprEvaluatorContext.getEngineURI(), exprEvaluatorContext.getStatementName(), AuditEnum.STREAM, eventType.getName() + " insert {" + EventBeanSummarizer.summarize(newData) + "} remove {" + EventBeanSummarizer.summarize(oldData) + "}");
            }
        }

        // if we have a filter for the named window,
        if (filterList.length != 0) {
            EventBean[] eventPerStream = new EventBean[1];
            newData = passFilter(newData, true, exprEvaluatorContext, eventPerStream);
            oldData = passFilter(oldData, false, exprEvaluatorContext, eventPerStream);
        }

        if (optPropertyEvaluator != null) {
            newData = getUnpacked(newData);
            oldData = getUnpacked(oldData);
        }

        if ((newData != null) || (oldData != null)) {
            updateChildren(newData, oldData);
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

        OneEventCollection filtered = null;
        for (EventBean theEvent : eventData) {
            eventPerStream[0] = theEvent;
            boolean pass = true;
            for (ExprEvaluator filter : filterList) {
                Boolean result = (Boolean) filter.evaluate(eventPerStream, isNewData, exprEvaluatorContext);
                if (result == null || !result) {
                    pass = false;
                    break;
                }
            }

            if (pass) {
                if (filtered == null) {
                    filtered = new OneEventCollection();
                }
                filtered.add(theEvent);
            }
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
        return new FilteredEventIterator(filterList, consumerCallback.getIterator(), exprEvaluatorContext);
    }

    public void stop() {
        consumerCallback.stopped(this);
    }
}
