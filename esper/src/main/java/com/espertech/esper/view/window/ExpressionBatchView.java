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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.ViewDataVisitor;
import com.espertech.esper.view.ViewFactory;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This view is a moving window extending the into the past until the expression passed to it returns false.
 */
public class ExpressionBatchView extends ExpressionViewBase {

    private final ExpressionBatchViewFactory dataWindowViewFactory;
    protected final Set<EventBean> window = new LinkedHashSet<EventBean>();

    protected EventBean[] lastBatch;
    protected long newestEventTimestamp;
    protected long oldestEventTimestamp;
    protected EventBean oldestEvent;
    protected EventBean newestEvent;

    public ExpressionBatchView(ExpressionBatchViewFactory dataWindowViewFactory,
                               ViewUpdatedCollection viewUpdatedCollection,
                               ExprEvaluator expiryExpression,
                               AggregationServiceFactoryDesc aggregationServiceFactoryDesc,
                               ObjectArrayEventBean builtinEventProps,
                               Set<String> variableNames,
                               AgentInstanceViewFactoryChainContext agentInstanceContext) {
        super(viewUpdatedCollection, expiryExpression, aggregationServiceFactoryDesc, builtinEventProps, variableNames, agentInstanceContext);
        this.dataWindowViewFactory = dataWindowViewFactory;
    }

    public String getViewName() {
        return dataWindowViewFactory.getViewName();
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, dataWindowViewFactory.getViewName(), newData, oldData);
        }

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
                    if (fireBatch && !dataWindowViewFactory.isIncludeTriggeringEvent()) {
                        numEventsInBatch = window.size() - 1;
                    }
                }
            }
        }

        // may fire the batch
        if (fireBatch) {
            expire(numEventsInBatch);
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
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
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, dataWindowViewFactory.getViewName(), batchNewData, lastBatch);
                }
                updateChildren(batchNewData, lastBatch);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
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
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, dataWindowViewFactory.getViewName(), batchNewData, lastBatch);
                }
                updateChildren(batchNewData, lastBatch);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
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
        viewDataVisitor.visitPrimary(window, true, dataWindowViewFactory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, dataWindowViewFactory.getViewName());
    }

    private boolean evaluateExpression(EventBean arriving, int windowSize) {

        ExpressionViewOAFieldEnum.populate(builtinEventProps.getProperties(), windowSize, oldestEventTimestamp, newestEventTimestamp, this, 0, oldestEvent, newestEvent);
        eventsPerStream[0] = arriving;
        for (AggregationServiceAggExpressionDesc aggregateNode : aggregateNodes) {
            aggregateNode.assignFuture(aggregationService);
        }

        Boolean result = (Boolean) expiryExpression.evaluate(eventsPerStream, true, agentInstanceContext);
        if (result == null) {
            return false;
        }
        return result;
    }

    public final Iterator<EventBean> iterator() {
        return window.iterator();
    }

    // Handle variable updates by scheduling a re-evaluation with timers
    public void update(Object newValue, Object oldValue) {
        if (!agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
            agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        }
    }

    public ViewFactory getViewFactory() {
        return dataWindowViewFactory;
    }
}
