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
package com.espertech.esper.common.internal.view.expression;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventBean;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.ViewDataVisitor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This view is a moving window extending the into the past until the expression passed to it returns false.
 */
public class ExpressionBatchView extends ExpressionViewBase {

    protected final Set<EventBean> window = new LinkedHashSet<EventBean>();

    protected EventBean[] lastBatch;
    protected long newestEventTimestamp;
    protected long oldestEventTimestamp;
    protected EventBean oldestEvent;
    protected EventBean newestEvent;

    public ExpressionBatchView(ExpressionBatchViewFactory factory,
                               ViewUpdatedCollection viewUpdatedCollection,
                               ObjectArrayEventBean builtinEventProps,
                               AgentInstanceViewFactoryChainContext agentInstanceContext) {
        super(factory, viewUpdatedCollection, builtinEventProps, agentInstanceContext);
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return window.isEmpty();
    }

    public void scheduleCallback() {
        boolean fireBatch = evaluateExpression(null, window.size());
        if (fireBatch) {
            expire(window.size());
        }
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        boolean fireBatch = false;

        // remove points from data window
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                window.remove(anOldData);
            }
            if (aggregationService != null) {
                aggregationService.applyLeave(oldData, null, agentInstanceContext);
            }

            if (!window.isEmpty()) {
                oldestEvent = window.iterator().next();
            } else {
                oldestEvent = null;
            }

            fireBatch = evaluateExpression(null, window.size());
        }

        // add data points to the window
        int numEventsInBatch = -1;
        if (newData != null && newData.length > 0) {
            if (window.isEmpty()) {
                oldestEventTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            }
            newestEventTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            if (oldestEvent == null) {
                oldestEvent = newData[0];
            }

            for (EventBean newEvent : newData) {
                window.add(newEvent);
                if (aggregationService != null) {
                    aggregationService.applyEnter(new EventBean[]{newEvent}, null, agentInstanceContext);
                }
                newestEvent = newEvent;
                if (!fireBatch) {
                    fireBatch = evaluateExpression(newEvent, window.size());
                    if (fireBatch && !((ExpressionBatchViewFactory) factory).isIncludeTriggeringEvent()) {
                        numEventsInBatch = window.size() - 1;
                    }
                }
            }
        }

        // may fire the batch
        if (fireBatch) {
            expire(numEventsInBatch);
        }
        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    // Called based on schedule evaluation registered when a variable changes (new data is null).
    // Called when new data arrives.
    public void expire(int numEventsInBatch) {

        if (numEventsInBatch == window.size() || numEventsInBatch == -1) {
            EventBean[] batchNewData = window.toArray(new EventBean[window.size()]);
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(batchNewData, lastBatch);
            }

            // post
            if (batchNewData != null || lastBatch != null) {
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, batchNewData, lastBatch);
                child.update(batchNewData, lastBatch);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }

            // clear
            window.clear();
            lastBatch = batchNewData;
            if (aggregationService != null) {
                aggregationService.clearResults(agentInstanceContext);
            }
            oldestEvent = null;
            newestEvent = null;
        } else {
            EventBean[] batchNewData = new EventBean[numEventsInBatch];
            Iterator<EventBean> it = window.iterator();
            for (int i = 0; i < batchNewData.length; i++) {
                batchNewData[i] = it.next();
                it.remove();
            }

            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(batchNewData, lastBatch);
            }

            // post
            if (batchNewData != null || lastBatch != null) {
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, batchNewData, lastBatch);
                child.update(batchNewData, lastBatch);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }

            // clear
            lastBatch = batchNewData;
            if (aggregationService != null) {
                aggregationService.applyLeave(batchNewData, null, agentInstanceContext);
            }
            oldestEvent = window.iterator().next();
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(window, true, factory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, factory.getViewName());
    }

    private boolean evaluateExpression(EventBean arriving, int windowSize) {

        ExpressionViewOAFieldEnum.populate(builtinEventProps.getProperties(), windowSize, oldestEventTimestamp, newestEventTimestamp, this, 0, oldestEvent, newestEvent);
        eventsPerStream[0] = arriving;
        return ExpressionBatchViewUtil.evaluate(eventsPerStream, agentInstanceContext, factory, aggregationService);
    }

    public final Iterator<EventBean> iterator() {
        return window.iterator();
    }

    // Handle variable updates by scheduling a re-evaluation with timers
    public void update(Object newValue, Object oldValue) {
        if (!agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
            agentInstanceContext.getAuditProvider().scheduleAdd(0, agentInstanceContext, scheduleHandle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        }
    }
}
