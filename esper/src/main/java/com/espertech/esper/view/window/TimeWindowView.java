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
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleAdjustmentCallback;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;

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
public class TimeWindowView extends ViewSupport implements DataWindowView, ScheduleAdjustmentCallback, StoppableView, StopCallback {
    private final TimeWindowViewFactory timeWindowViewFactory;
    private final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    protected final TimeWindow timeWindow;
    private final ViewUpdatedCollection viewUpdatedCollection;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private final long scheduleSlot;
    private final EPStatementHandleCallback handle;

    /**
     * Constructor.
     *
     * @param timeDeltaComputation  is the computation for the number of milliseconds before events gets pushed
     *                              out of the timeWindow as oldData in the update method.
     * @param viewUpdatedCollection is a collection the view must update when receiving events
     * @param timeWindowViewFactory for copying the view in a group-by
     * @param agentInstanceContext  context
     */
    public TimeWindowView(AgentInstanceViewFactoryChainContext agentInstanceContext, TimeWindowViewFactory timeWindowViewFactory, ExprTimePeriodEvalDeltaConst timeDeltaComputation, ViewUpdatedCollection viewUpdatedCollection) {
        this.agentInstanceContext = agentInstanceContext;
        this.timeWindowViewFactory = timeWindowViewFactory;
        this.timeDeltaComputation = timeDeltaComputation;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timeWindow = new TimeWindow(agentInstanceContext.isRemoveStream());

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(TimeWindowView.this, TimeWindowView.this.timeWindowViewFactory.getViewName());
                }
                TimeWindowView.this.expire();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewScheduledEval();
                }
            }
        };
        this.handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);

        if (agentInstanceContext.getStatementContext().getScheduleAdjustmentService() != null) {
            agentInstanceContext.getStatementContext().getScheduleAdjustmentService().addCallback(this);
        }
        agentInstanceContext.addTerminationCallback(this);
    }

    public void adjust(long delta) {
        timeWindow.adjust(delta);
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, timeWindowViewFactory.getViewName(), newData, oldData);
        }
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
                scheduleCallback(timeDeltaComputation.deltaAdd(current));
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
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, timeWindowViewFactory.getViewName(), newData, oldData);
            }
            updateChildren(newData, oldData);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    /**
     * This method removes (expires) objects from the window and schedules a new callback for the
     * time when the next oldest message would expire from the window.
     */
    protected final void expire() {
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long expireBeforeTimestamp = current - timeDeltaComputation.deltaSubtract(current) + 1;

        // Remove from the timeWindow any events that have an older or timestamp then the given timestamp
        // The window extends from X to (X - millisecondsBeforeExpiry + 1)
        ArrayDeque<EventBean> expired = timeWindow.expireEvents(expireBeforeTimestamp);

        // If there are child views, fireStatementStopped update method
        if (this.hasViews()) {
            if ((expired != null) && (!expired.isEmpty())) {
                EventBean[] oldEvents = expired.toArray(new EventBean[expired.size()]);
                if (viewUpdatedCollection != null) {
                    viewUpdatedCollection.update(null, oldEvents);
                }
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, timeWindowViewFactory.getViewName(), null, oldEvents);
                }
                updateChildren(null, oldEvents);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }

        scheduleExpiryCallback();
    }

    protected void scheduleExpiryCallback() {
        // If we still have events in the window, schedule new callback
        if (timeWindow.isEmpty()) {
            return;
        }
        Long oldestTimestamp = timeWindow.getOldestTimestamp();
        long currentTimestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long scheduleTime = timeDeltaComputation.deltaAdd(oldestTimestamp) + oldestTimestamp - currentTimestamp;
        scheduleCallback(scheduleTime);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    private void scheduleCallback(long timeAfterCurrentTime) {
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

    public void stopView() {
        stopSchedule();
        agentInstanceContext.removeTerminationCallback(this);
    }

    public void stop() {
        stopSchedule();
    }

    public void stopSchedule() {
        if (handle != null) {
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
        if (agentInstanceContext.getStatementContext().getScheduleAdjustmentService() != null) {
            agentInstanceContext.getStatementContext().getScheduleAdjustmentService().removeCallback(this);
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        timeWindow.visitView(viewDataVisitor, timeWindowViewFactory);
    }

    public ViewFactory getViewFactory() {
        return timeWindowViewFactory;
    }
}
