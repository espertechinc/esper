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
package com.espertech.esper.common.internal.view.timewin;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.TimeWindow;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * This view is a moving timeWindow extending the specified amount of milliseconds into the past.
 * The view bases the timeWindow on the time obtained from the scheduling service.
 * All incoming events receive a timestamp and are placed in a sorted map by timestamp.
 * The view does not care about old data published by the parent view to this view.
 * <p>
 * Events leave or expire from the time timeWindow by means of a scheduled callback registered with the
 * scheduling service. Thus child views receive updates containing old data only asynchronously
 * as the system-time-based timeWindow moves on. However child views receive updates containing new data
 * as soon as the new data arrives.
 */
public class TimeWindowView extends ViewSupport implements DataWindowView, AgentInstanceStopCallback {
    private final TimeWindowViewFactory timeWindowViewFactory;
    private final TimeWindow timeWindow;
    private final ViewUpdatedCollection viewUpdatedCollection;
    private final AgentInstanceContext agentInstanceContext;
    private final EPStatementHandleCallbackSchedule handle;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    public TimeWindowView(AgentInstanceViewFactoryChainContext agentInstanceContext, TimeWindowViewFactory timeWindowViewFactory, ViewUpdatedCollection viewUpdatedCollection, TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.timeWindowViewFactory = timeWindowViewFactory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.timeWindow = new TimeWindow(agentInstanceContext.isRemoveStream());
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(TimeWindowView.this.agentInstanceContext, ScheduleObjectType.view, timeWindowViewFactory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(timeWindowViewFactory);
                TimeWindowView.this.expire();
                agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
            }
        };
        this.handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
    }

    /**
     * Returns the (optional) collection handling random access to window contents for prior or previous events.
     *
     * @return buffer for events
     */
    public ViewUpdatedCollection getViewUpdatedCollection() {
        return viewUpdatedCollection;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, timeWindowViewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(timeWindowViewFactory, newData, oldData);
        long timestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                timeWindow.remove(oldData[i]);
            }
        }

        // we don't care about removed data from a prior view
        if ((newData != null) && (newData.length > 0)) {
            // If we have an empty window about to be filled for the first time, schedule a callback
            // for now plus millisecondsBeforeExpiry
            if (timeWindow.isEmpty()) {
                long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                scheduleCallback(timePeriodProvide.deltaAdd(current, null, true, agentInstanceContext));
            }

            // add data points to the timeWindow
            for (int i = 0; i < newData.length; i++) {
                timeWindow.add(timestamp, newData[i]);
            }

            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(newData, null);
            }
        }

        // update child views
        agentInstanceContext.getInstrumentationProvider().qViewIndicate(timeWindowViewFactory, newData, oldData);
        child.update(newData, oldData);
        agentInstanceContext.getInstrumentationProvider().aViewIndicate();

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    /**
     * This method removes (expires) objects from the window and schedules a new callback for the
     * time when the next oldest message would expire from the window.
     */
    private final void expire() {
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long expireBeforeTimestamp = current - timePeriodProvide.deltaSubtract(current, null, true, agentInstanceContext) + 1;

        // Remove from the timeWindow any events that have an older or timestamp then the given timestamp
        // The window extends from X to (X - millisecondsBeforeExpiry + 1)
        ArrayDeque<EventBean> expired = timeWindow.expireEvents(expireBeforeTimestamp);

        // If there are child views, fireStatementStopped update method
        if (getChild() != null) {
            if ((expired != null) && (!expired.isEmpty())) {
                EventBean[] oldEvents = expired.toArray(new EventBean[expired.size()]);
                if (viewUpdatedCollection != null) {
                    viewUpdatedCollection.update(null, oldEvents);
                }
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(timeWindowViewFactory, null, oldEvents);
                getChild().update(null, oldEvents);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        scheduleExpiryCallback();
    }

    private void scheduleExpiryCallback() {
        // If we still have events in the window, schedule new callback
        if (timeWindow.isEmpty()) {
            return;
        }
        Long oldestTimestamp = timeWindow.getOldestTimestamp();
        long currentTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long scheduleTime = timePeriodProvide.deltaAdd(oldestTimestamp, null, true, agentInstanceContext) + oldestTimestamp - currentTimestamp;
        scheduleCallback(scheduleTime);
    }

    private void scheduleCallback(long timeAfterCurrentTime) {
        agentInstanceContext.getAuditProvider().scheduleAdd(timeAfterCurrentTime, agentInstanceContext, handle, ScheduleObjectType.view, timeWindowViewFactory.getViewName());
        agentInstanceContext.getStatementContext().getSchedulingService().add(timeAfterCurrentTime, handle, scheduleSlot);
    }

    public final Iterator<EventBean> iterator() {
        return timeWindow.iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return timeWindow.isEmpty();
    }

    public void stop(AgentInstanceStopServices services) {
        if (handle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, timeWindowViewFactory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        timeWindow.visitView(viewDataVisitor, timeWindowViewFactory);
    }

    public ViewFactory getViewFactory() {
        return timeWindowViewFactory;
    }
}
