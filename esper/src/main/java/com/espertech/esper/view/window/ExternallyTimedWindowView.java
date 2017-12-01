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
import com.espertech.esper.view.internal.TimeWindow;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.*;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * View for a moving window extending the specified amount of time into the past, driven entirely by external timing
 * supplied within long-type timestamp values in a field of the event beans that the view receives.
 * <p>
 * The view is completely driven by timestamp values that are supplied by the events it receives,
 * and does not use the schedule service time.
 * It requires a field name as parameter for a field that returns ascending long-type timestamp values.
 * It also requires a long-type parameter setting the time length in milliseconds of the time window.
 * Events are expected to provide long-type timestamp values in natural order. The view does
 * itself not use the current system time for keeping track of the time window, but just the
 * timestamp values supplied by the events sent in.
 * <p>
 * The arrival of new events with a newer timestamp then past events causes the window to be re-evaluated and the oldest
 * events pushed out of the window. Ie. Assume event X1 with timestamp T1 is in the window.
 * When event Xn with timestamp Tn arrives, and the window time length in milliseconds is t, then if
 * ((Tn - T1) &gt; t == true) then event X1 is pushed as oldData out of the window. It is assumed that
 * events are sent in in their natural order and the timestamp values are ascending.
 */
public class ExternallyTimedWindowView extends ViewSupport implements DataWindowView {
    private final ExternallyTimedWindowViewFactory externallyTimedWindowViewFactory;
    private final ExprNode timestampExpression;
    private final ExprEvaluator timestampExpressionEval;
    private final ExprTimePeriodEvalDeltaConst timeDeltaComputation;

    private final EventBean[] eventsPerStream = new EventBean[1];
    protected final TimeWindow timeWindow;
    private ViewUpdatedCollection viewUpdatedCollection;
    protected AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;

    /**
     * Constructor.
     *
     * @param timestampExpression              is the field name containing a long timestamp value
     *                                         that should be in ascending order for the natural order of events and is intended to reflect
     *                                         System.currentTimeInMillis but does not necessarily have to.
     *                                         out of the window as oldData in the update method. The view compares
     *                                         each events timestamp against the newest event timestamp and those with a delta
     *                                         greater then secondsBeforeExpiry are pushed out of the window.
     * @param viewUpdatedCollection            is a collection that the view must update when receiving events
     * @param externallyTimedWindowViewFactory for copying this view in a group-by
     * @param agentInstanceViewFactoryContext  context for expression evalauation
     * @param timeDeltaComputation             delta computation
     * @param timestampExpressionEval          timestamp expr eval
     */
    public ExternallyTimedWindowView(ExternallyTimedWindowViewFactory externallyTimedWindowViewFactory,
                                     ExprNode timestampExpression, ExprEvaluator timestampExpressionEval,
                                     ExprTimePeriodEvalDeltaConst timeDeltaComputation, ViewUpdatedCollection viewUpdatedCollection,
                                     AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        this.externallyTimedWindowViewFactory = externallyTimedWindowViewFactory;
        this.timestampExpression = timestampExpression;
        this.timestampExpressionEval = timestampExpressionEval;
        this.timeDeltaComputation = timeDeltaComputation;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.timeWindow = new TimeWindow(agentInstanceViewFactoryContext.isRemoveStream());
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
    }

    /**
     * Returns the field name to get timestamp values from.
     *
     * @return field name for timestamp values
     */
    public final ExprNode getTimestampExpression() {
        return timestampExpression;
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, externallyTimedWindowViewFactory.getViewName(), newData, oldData);
        }
        long timestamp = -1;

        // add data points to the window
        // we don't care about removed data from a prior view
        if (newData != null) {
            for (int i = 0; i < newData.length; i++) {
                timestamp = getLongValue(newData[i]);
                timeWindow.add(timestamp, newData[i]);
            }
        }

        // Remove from the window any events that have an older timestamp then the last event's timestamp
        ArrayDeque<EventBean> expired = null;
        if (timestamp != -1) {
            expired = timeWindow.expireEvents(timestamp - timeDeltaComputation.deltaSubtract(timestamp) + 1);
        }

        EventBean[] oldDataUpdate = null;
        if ((expired != null) && (!expired.isEmpty())) {
            oldDataUpdate = expired.toArray(new EventBean[expired.size()]);
        }

        if ((oldData != null) && (agentInstanceViewFactoryContext.isRemoveStream())) {
            for (EventBean anOldData : oldData) {
                timeWindow.remove(anOldData);
            }

            if (oldDataUpdate == null) {
                oldDataUpdate = oldData;
            } else {
                oldDataUpdate = CollectionUtil.addArrayWithSetSemantics(oldData, oldDataUpdate);
            }
        }

        if (viewUpdatedCollection != null) {
            viewUpdatedCollection.update(newData, oldDataUpdate);
        }

        // If there are child views, fireStatementStopped update method
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, externallyTimedWindowViewFactory.getViewName(), newData, oldDataUpdate);
            }
            updateChildren(newData, oldDataUpdate);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        return timeWindow.iterator();
    }

    public final String toString() {
        return this.getClass().getName() +
                " timestampExpression=" + timestampExpression;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        timeWindow.visitView(viewDataVisitor, externallyTimedWindowViewFactory);
    }

    private long getLongValue(EventBean obj) {
        eventsPerStream[0] = obj;
        Number num = (Number) timestampExpressionEval.evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
        return num.longValue();
    }

    /**
     * Returns true to indicate the window is empty, or false if the view is not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return timeWindow.isEmpty();
    }

    public ViewUpdatedCollection getViewUpdatedCollection() {
        return viewUpdatedCollection;
    }

    public ViewFactory getViewFactory() {
        return externallyTimedWindowViewFactory;
    }
}
