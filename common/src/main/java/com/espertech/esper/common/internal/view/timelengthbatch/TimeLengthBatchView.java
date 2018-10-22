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
package com.espertech.esper.common.internal.view.timelengthbatch;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A data view that aggregates events in a stream and releases them in one batch if either one of these
 * conditions is reached, whichever comes first: One, a time interval passes. Two, a given number of events collected.
 * <p>
 * The view releases the batched events after the interval or number of events as new data to child views. The prior batch if
 * not empty is released as old data to child view. The view DOES release intervals with no old or new data.
 * It does not collect old data published by a parent view.
 * If there are no events in the current and prior batch, the view WILL invoke the update method of child views.
 * <p>
 * The view starts the first interval when the view is created.
 */
public class TimeLengthBatchView extends ViewSupport implements AgentInstanceStopCallback, DataWindowView {
    private final TimeLengthBatchViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final ViewUpdatedCollection viewUpdatedCollection;
    private final int size;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    protected ArrayList<EventBean> lastBatch = null;
    protected ArrayList<EventBean> currentBatch = new ArrayList<>();
    protected Long callbackScheduledTime;
    protected EPStatementHandleCallbackSchedule handle;

    public TimeLengthBatchView(TimeLengthBatchViewFactory factory,
                               int size,
                               AgentInstanceViewFactoryChainContext agentInstanceContext,
                               ViewUpdatedCollection viewUpdatedCollection,
                               TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.size = size;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        // schedule the first callback
        if (factory.isStartEager()) {
            scheduleCallback(0);
        }
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                if (currentBatch.remove(oldData[i])) {
                    internalHandleRemoved(oldData[i]);
                }
            }
        }

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
            return;
        }

        // Add data points
        for (EventBean newEvent : newData) {
            currentBatch.add(newEvent);
            internalHandleAdded(newEvent);
        }

        // We are done unless we went over the boundary
        if (currentBatch.size() < size) {
            // Schedule a callback if there is none scheduled
            if (callbackScheduledTime == null) {
                scheduleCallback(0);
            }
            agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
            return;
        }

        // send a batch of events
        sendBatch(false);

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public void internalHandleAdded(EventBean newEvent) {
        // no action required
    }

    public void internalHandleRemoved(EventBean eventBean) {
        // no action required
    }

    /**
     * This method updates child views and clears the batch of events.
     * We cancel and old callback and schedule a new callback at this time if there were events in the batch.
     *
     * @param isFromSchedule true if invoked from a schedule, false if not
     */
    protected void sendBatch(boolean isFromSchedule) {
        // No more callbacks scheduled if called from a schedule
        if (isFromSchedule) {
            callbackScheduledTime = null;
        } else {
            // Remove schedule if called from on overflow due to number of events
            if (callbackScheduledTime != null) {
                agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                callbackScheduledTime = null;
            }
        }

        // If there are child views and the batch was filled, fireStatementStopped update method
        if (child != null) {
            // Convert to object arrays
            EventBean[] newData = null;
            EventBean[] oldData = null;
            if (!currentBatch.isEmpty()) {
                newData = currentBatch.toArray(new EventBean[currentBatch.size()]);
            }
            if ((lastBatch != null) && (!lastBatch.isEmpty())) {
                oldData = lastBatch.toArray(new EventBean[lastBatch.size()]);
            }

            // Post new data (current batch) and old data (prior batch)
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(newData, oldData);
            }
            if ((newData != null) || (oldData != null) || factory.isForceUpdate()) {
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldData);
                child.update(newData, oldData);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        // Only if there have been any events in this or the last interval do we schedule a callback,
        // such as to not waste resources when no events arrive.
        if (((!currentBatch.isEmpty()) || ((lastBatch != null) && (!lastBatch.isEmpty())))
                ||
                factory.isForceUpdate) {
            scheduleCallback(0);
        }

        // Flush and roll
        lastBatch = currentBatch;
        currentBatch = new ArrayList<EventBean>();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        if (lastBatch != null) {
            if (!lastBatch.isEmpty()) {
                return false;
            }
        }
        return currentBatch.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return currentBatch.iterator();
    }

    public final String toString() {
        return this.getClass().getName() +
                " numberOfEvents=" + size;
    }

    protected void scheduleCallback(long delta) {
        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeLengthBatchView.this.sendBatch(true);
                agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
            }
        };
        handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long scheduled = timePeriodProvide.deltaAdd(currentTime, null, true, agentInstanceContext) - delta;
        agentInstanceContext.getStatementContext().getSchedulingService().add(scheduled, handle, scheduleSlot);
        callbackScheduledTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime() + scheduled;
    }

    public void stop(AgentInstanceStopServices services) {
        if (handle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(lastBatch, true, factory.getViewName(), null);
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
