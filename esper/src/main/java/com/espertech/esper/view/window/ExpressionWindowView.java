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
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.event.arr.ObjectArrayEventBean;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.ViewDataVisitor;
import com.espertech.esper.view.ViewFactory;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;

/**
 * This view is a moving window extending the into the past until the expression passed to it returns false.
 */
public class ExpressionWindowView extends ExpressionViewBase {

    private final ExpressionWindowViewFactory dataWindowViewFactory;
    protected final ArrayDeque<ExpressionWindowTimestampEventPair> window = new ArrayDeque<ExpressionWindowTimestampEventPair>();
    private final EventBean[] removedEvents = new EventBean[1];

    public ExpressionWindowView(ExpressionWindowViewFactory dataWindowViewFactory,
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
        expire(null, null);
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, dataWindowViewFactory.getViewName(), newData, oldData);
        }

        // add data points to the window
        if (newData != null) {
            for (EventBean newEvent : newData) {
                ExpressionWindowTimestampEventPair pair = new ExpressionWindowTimestampEventPair(agentInstanceContext.getTimeProvider().getTime(), newEvent);
                window.add(pair);
                internalHandleAdd(pair);
            }

            if (aggregationService != null) {
                aggregationService.applyEnter(newData, null, agentInstanceContext);
            }
        }

        if (oldData != null) {
            Iterator<ExpressionWindowTimestampEventPair> it = window.iterator();
            for (; it.hasNext(); ) {
                ExpressionWindowTimestampEventPair pair = it.next();
                for (EventBean anOldData : oldData) {
                    if (pair.getTheEvent() == anOldData) {
                        it.remove();
                        break;
                    }
                }
                internalHandleRemoved(pair);
            }
            if (aggregationService != null) {
                aggregationService.applyLeave(oldData, null, agentInstanceContext);
            }
        }

        // expire events
        expire(newData, oldData);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public void internalHandleRemoved(ExpressionWindowTimestampEventPair pair) {
        // no action required
    }

    public void internalHandleExpired(ExpressionWindowTimestampEventPair pair) {
        // no action required
    }

    public void internalHandleAdd(ExpressionWindowTimestampEventPair pair) {
        // no action required
    }

    // Called based on schedule evaluation registered when a variable changes (new data is null).
    // Called when new data arrives.
    private void expire(EventBean[] newData, EventBean[] oldData) {

        OneEventCollection expired = null;
        if (oldData != null) {
            expired = new OneEventCollection();
            expired.add(oldData);
        }
        int expiredCount = 0;
        if (!window.isEmpty()) {
            ExpressionWindowTimestampEventPair newest = window.getLast();

            while (true) {
                ExpressionWindowTimestampEventPair first = window.getFirst();

                boolean pass = checkEvent(first, newest, expiredCount);
                if (!pass) {
                    if (expired == null) {
                        expired = new OneEventCollection();
                    }
                    EventBean removed = window.removeFirst().getTheEvent();
                    expired.add(removed);
                    if (aggregationService != null) {
                        removedEvents[0] = removed;
                        aggregationService.applyLeave(removedEvents, null, agentInstanceContext);
                    }
                    expiredCount++;
                    internalHandleExpired(first);
                } else {
                    break;
                }

                if (window.isEmpty()) {
                    if (aggregationService != null) {
                        aggregationService.clearResults(agentInstanceContext);
                    }
                    break;
                }
            }
        }

        // Check for any events that get pushed out of the window
        EventBean[] expiredArr = null;
        if (expired != null) {
            expiredArr = expired.toArray();
        }

        // update event buffer for access by expressions, if any
        if (viewUpdatedCollection != null) {
            viewUpdatedCollection.update(newData, expiredArr);
        }

        // If there are child views, call update method
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, dataWindowViewFactory.getViewName(), newData, expiredArr);
            }
            updateChildren(newData, expiredArr);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }
    }

    private boolean checkEvent(ExpressionWindowTimestampEventPair first, ExpressionWindowTimestampEventPair newest, int numExpired) {

        ExpressionViewOAFieldEnum.populate(builtinEventProps.getProperties(), window.size(), first.getTimestamp(), newest.getTimestamp(),
                this, numExpired, first.getTheEvent(), newest.getTheEvent());
        eventsPerStream[0] = first.getTheEvent();

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
        return new ExpressionWindowTimestampEventPairIterator(window.iterator());
    }

    // Handle variable updates by scheduling a re-evaluation with timers
    public void update(Object newValue, Object oldValue) {
        if (!agentInstanceContext.getStatementContext().getSchedulingService().isScheduled(scheduleHandle)) {
            agentInstanceContext.getStatementContext().getSchedulingService().add(0, scheduleHandle, scheduleSlot);
        }
    }

    public ArrayDeque<ExpressionWindowTimestampEventPair> getWindow() {
        return window;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(window, true, dataWindowViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return dataWindowViewFactory;
    }
}
