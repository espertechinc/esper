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
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * A data window view that holds events in a stream and only removes events from a stream (rstream) if
 * no more events arrive for a given time interval, also handling the remove stream
 * by keeping set-like semantics. See {@link TimeAccumView} for the same behavior without
 * remove stream handling.
 */
public class TimeAccumViewRStream extends ViewSupport implements DataWindowView, AgentInstanceStopCallback {
    private final TimeAccumViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    private LinkedHashMap<EventBean, Long> currentBatch = new LinkedHashMap<EventBean, Long>();
    private EventBean lastEvent;
    private long callbackScheduledTime;
    private EPStatementHandleCallbackSchedule handle;

    public TimeAccumViewRStream(TimeAccumViewFactory timeBatchViewFactory,
                                AgentInstanceViewFactoryChainContext agentInstanceContext,
                                TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = timeBatchViewFactory;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext.getAgentInstanceContext(), ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeAccumViewRStream.this.sendRemoveStream();
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

        if ((newData != null) && (newData.length > 0)) {
            // If we have an empty window about to be filled for the first time, add a callback
            boolean removeSchedule = false;
            boolean addSchedule = false;
            long timestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();

            // if the window is already filled, then we may need to reschedule
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
                callbackScheduledTime = -1;
            }
            if (addSchedule) {
                long timeIntervalSize = timePeriodProvide.deltaAdd(timestamp, null, true, agentInstanceContext);
                agentInstanceContext.getAuditProvider().scheduleAdd(timeIntervalSize, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getStatementContext().getSchedulingService().add(timeIntervalSize, handle, scheduleSlot);
                callbackScheduledTime = timeIntervalSize + timestamp;
            }

            // add data points to the window
            for (int i = 0; i < newData.length; i++) {
                currentBatch.put(newData[i], timestamp);
                lastEvent = newData[i];
            }
        }

        if ((oldData != null) && (oldData.length > 0)) {
            boolean removedLastEvent = false;
            for (EventBean anOldData : oldData) {
                currentBatch.remove(anOldData);
                if (anOldData == lastEvent) {
                    removedLastEvent = true;
                }
            }

            // we may need to reschedule as the newest event may have been deleted
            if (currentBatch.size() == 0) {
                agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                callbackScheduledTime = -1;
                lastEvent = null;
            } else {
                // reschedule if the last event was removed
                if (removedLastEvent) {
                    Set<EventBean> keyset = currentBatch.keySet();
                    EventBean[] events = keyset.toArray(new EventBean[keyset.size()]);
                    lastEvent = events[events.length - 1];
                    long lastTimestamp = currentBatch.get(lastEvent);

                    // reschedule, newest event deleted
                    long timestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                    long callbackTime = lastTimestamp + timePeriodProvide.deltaAdd(lastTimestamp, null, true, agentInstanceContext);
                    long deltaFromNow = callbackTime - timestamp;
                    if (callbackTime != callbackScheduledTime) {
                        agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                        agentInstanceContext.getAuditProvider().scheduleAdd(deltaFromNow, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
                        agentInstanceContext.getStatementContext().getSchedulingService().add(deltaFromNow, handle, scheduleSlot);
                        callbackScheduledTime = callbackTime;
                    }
                }
            }
        }

        // update child views
        if (child != null) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldData);
            child.update(newData, oldData);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), currentBatch.size(), null);
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
                oldData = currentBatch.keySet().toArray(new EventBean[currentBatch.size()]);
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
        return currentBatch.keySet().iterator();
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

    public ViewFactory getViewFactory() {
        return factory;
    }
}
