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
package com.espertech.esper.common.internal.view.time_accum;

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
 * A data window view that holds events in a stream and only removes events from a stream (rstream) if
 * no more events arrive for a given time interval.
 * <p>
 * No batch version of the view exists as the batch version is simply the remove stream of this view, which removes
 * in batches.
 * <p>
 * The view is continuous, the insert stream consists of arriving events. The remove stream
 * only posts current window contents when no more events arrive for a given timer interval.
 */
public class TimeAccumView extends ViewSupport implements DataWindowView, AgentInstanceStopCallback {
    // View parameters
    private final TimeAccumViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final ViewUpdatedCollection viewUpdatedCollection;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    private ArrayList<EventBean> currentBatch = new ArrayList<EventBean>();
    private long callbackScheduledTime;
    private EPStatementHandleCallbackSchedule handle;

    public TimeAccumView(TimeAccumViewFactory timeBatchViewFactory,
                         AgentInstanceViewFactoryChainContext agentInstanceContext,
                         ViewUpdatedCollection viewUpdatedCollection,
                         TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = timeBatchViewFactory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext.getAgentInstanceContext(), ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeAccumView.this.sendRemoveStream();
                agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
            }
        };
        handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            return;
        }

        // If we have an empty window about to be filled for the first time, addSchedule a callback
        boolean removeSchedule = false;
        boolean addSchedule = false;
        long timestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();

        if (!currentBatch.isEmpty()) {
            // check if we need to reschedule
            long callbackTime = timestamp + timePeriodProvide.deltaAdd(timestamp, null, true, agentInstanceContext);
            if (callbackTime != callbackScheduledTime) {
                removeSchedule = true;
                addSchedule = true;
            }
        } else {
            addSchedule = true;
        }

        if (removeSchedule) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
        if (addSchedule) {
            long timeIntervalSize = timePeriodProvide.deltaAdd(timestamp, null, true, agentInstanceContext);
            agentInstanceContext.getAuditProvider().scheduleAdd(timeIntervalSize, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().add(timeIntervalSize, handle, scheduleSlot);
            callbackScheduledTime = timeIntervalSize + timestamp;
        }

        // add data points to the window
        for (EventBean newEvent : newData) {
            currentBatch.add(newEvent);
        }

        // forward insert stream to child views
        if (viewUpdatedCollection != null) {
            viewUpdatedCollection.update(newData, null);
        }

        // update child views
        if (child != null) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, null);
            child.update(newData, null);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    /**
     * This method sends the remove stream for all accumulated events.
     */
    private void sendRemoveStream() {
        callbackScheduledTime = -1;

        // If there are child views and the batch was filled, fireStatementStopped update method
        if (child != null) {
            // Convert to object arrays
            EventBean[] oldData = null;
            if (!currentBatch.isEmpty()) {
                oldData = currentBatch.toArray(new EventBean[currentBatch.size()]);
            }

            // Post old data
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(null, oldData);
            }

            if (oldData != null) {
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, oldData);
                child.update(null, oldData);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        currentBatch.clear();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return currentBatch.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return currentBatch.iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    public void stop(AgentInstanceStopServices services) {
        if (handle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
