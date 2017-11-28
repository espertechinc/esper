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
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaResult;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Batch window based on timestamp of arriving events.
 */
public class ExternallyTimedBatchView extends ViewSupport implements DataWindowView {
    private final ExternallyTimedBatchViewFactory factory;
    private final ExprNode timestampExpression;
    private final ExprEvaluator timestampExpressionEval;
    private final ExprTimePeriodEvalDeltaConst timeDeltaComputation;

    private final EventBean[] eventsPerStream = new EventBean[1];
    protected EventBean[] lastBatch;

    private Long oldestTimestamp;
    protected final Set<EventBean> window = new LinkedHashSet<EventBean>();
    protected Long referenceTimestamp;

    protected ViewUpdatedCollection viewUpdatedCollection;
    protected AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    /**
     * Constructor.
     *
     * @param timestampExpression             is the field name containing a long timestamp value
     *                                        that should be in ascending order for the natural order of events and is intended to reflect
     *                                        System.currentTimeInMillis but does not necessarily have to.
     *                                        out of the window as oldData in the update method. The view compares
     *                                        each events timestamp against the newest event timestamp and those with a delta
     *                                        greater then secondsBeforeExpiry are pushed out of the window.
     * @param viewUpdatedCollection           is a collection that the view must update when receiving events
     * @param factory                         for copying this view in a group-by
     * @param agentInstanceViewFactoryContext context for expression evalauation
     * @param optionalReferencePoint          ref point
     * @param timeDeltaComputation            time delta
     * @param timestampExpressionEval         timestamp expr eval
     */
    public ExternallyTimedBatchView(ExternallyTimedBatchViewFactory factory,
                                    ExprNode timestampExpression,
                                    ExprEvaluator timestampExpressionEval,
                                    ExprTimePeriodEvalDeltaConst timeDeltaComputation,
                                    Long optionalReferencePoint,
                                    ViewUpdatedCollection viewUpdatedCollection,
                                    AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.factory = factory;
        this.timestampExpression = timestampExpression;
        this.timestampExpressionEval = timestampExpressionEval;
        this.timeDeltaComputation = timeDeltaComputation;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.referenceTimestamp = optionalReferencePoint;
    }

    /**
     * Returns the field name to get timestamp values from.
     *
     * @return field name for timestamp values
     */
    public final ExprNode getTimestampExpression() {
        return timestampExpression;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, factory.getViewName(), newData, oldData);
        }

        // remove points from data window
        if (oldData != null && oldData.length != 0) {
            for (EventBean anOldData : oldData) {
                window.remove(anOldData);
                handleInternalRemovedEvent(anOldData);
            }
            determineOldestTimestamp();
        }

        // add data points to the window
        EventBean[] batchNewData = null;
        if (newData != null) {
            for (EventBean newEvent : newData) {

                long timestamp = getLongValue(newEvent);
                if (referenceTimestamp == null) {
                    referenceTimestamp = timestamp;
                }

                if (oldestTimestamp == null) {
                    oldestTimestamp = timestamp;
                } else {
                    ExprTimePeriodEvalDeltaResult delta = timeDeltaComputation.deltaAddWReference(oldestTimestamp, referenceTimestamp);
                    this.referenceTimestamp = delta.getLastReference();
                    if (timestamp - oldestTimestamp >= delta.getDelta()) {
                        if (batchNewData == null) {
                            batchNewData = window.toArray(new EventBean[window.size()]);
                        } else {
                            batchNewData = EventBeanUtility.addToArray(batchNewData, window);
                        }
                        window.clear();
                        oldestTimestamp = null;
                    }
                }

                window.add(newEvent);
                handleInternalAddEvent(newEvent, batchNewData != null);
            }
        }

        if (batchNewData != null) {
            handleInternalPostBatch(window, batchNewData);
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(batchNewData, lastBatch);
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), newData, lastBatch);
            }
            updateChildren(batchNewData, lastBatch);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
            lastBatch = batchNewData;
            determineOldestTimestamp();
        }
        if (oldData != null && oldData.length > 0) {
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(null, oldData);
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), null, oldData);
            }
            updateChildren(null, oldData);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        return window.iterator();
    }

    public final String toString() {
        return this.getClass().getName() +
                " timestampExpression=" + timestampExpression;
    }

    /**
     * Returns true to indicate the window is empty, or false if the view is not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return window.isEmpty();
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(window, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }

    protected void determineOldestTimestamp() {
        if (window.isEmpty()) {
            oldestTimestamp = null;
        } else {
            oldestTimestamp = getLongValue(window.iterator().next());
        }
    }

    protected void handleInternalPostBatch(Set<EventBean> window, EventBean[] batchNewData) {
        // no action require
    }

    protected void handleInternalRemovedEvent(EventBean anOldData) {
        // no action require
    }

    protected void handleInternalAddEvent(EventBean anNewData, boolean isNextBatch) {
        // no action require
    }

    private long getLongValue(EventBean obj) {
        eventsPerStream[0] = obj;
        Number num = (Number) timestampExpressionEval.evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        return num.longValue();
    }
}
