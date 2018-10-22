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
package com.espertech.esper.common.internal.view.exttimedwin;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.TimeWindow;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.view.core.*;

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
    private final ExternallyTimedWindowViewFactory factory;

    private final EventBean[] eventsPerStream = new EventBean[1];
    protected final TimeWindow timeWindow;
    private ViewUpdatedCollection viewUpdatedCollection;
    protected AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final TimePeriodProvide timePeriodProvide;

    public ExternallyTimedWindowView(ExternallyTimedWindowViewFactory factory,
                                     ViewUpdatedCollection viewUpdatedCollection,
                                     AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext,
                                     TimePeriodProvide timePeriodProvide) {
        this.factory = factory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.timeWindow = new TimeWindow(agentInstanceViewFactoryContext.isRemoveStream());
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.timePeriodProvide = timePeriodProvide;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        AgentInstanceContext agentInstanceContext = agentInstanceViewFactoryContext.getAgentInstanceContext();
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);
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
            expired = timeWindow.expireEvents(timestamp - timePeriodProvide.deltaSubtract(timestamp, null, true, agentInstanceViewFactoryContext) + 1);
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
        if (child != null) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldDataUpdate);
            child.update(newData, oldDataUpdate);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return timeWindow.iterator();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        timeWindow.visitView(viewDataVisitor, factory);
    }

    private long getLongValue(EventBean obj) {
        eventsPerStream[0] = obj;
        Number num = (Number) factory.timestampEval.evaluate(eventsPerStream, true, agentInstanceViewFactoryContext);
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
        return factory;
    }
}
