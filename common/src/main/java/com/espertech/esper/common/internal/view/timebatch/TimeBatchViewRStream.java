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
package com.espertech.esper.common.internal.view.timebatch;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodDeltaResult;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Same as the {@link TimeBatchView}, this view also supports fast-remove from the batch for remove stream events.
 */
public class TimeBatchViewRStream extends ViewSupport implements AgentInstanceStopCallback, DataWindowView {
    private final TimeBatchViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private EPStatementHandleCallbackSchedule handle;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    private Long currentReferencePoint;
    private LinkedHashSet<EventBean> lastBatch = null;
    private LinkedHashSet<EventBean> currentBatch = new LinkedHashSet<EventBean>();
    private boolean isCallbackScheduled;

    public TimeBatchViewRStream(TimeBatchViewFactory factory,
                                AgentInstanceViewFactoryChainContext agentInstanceContext,
                                TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        // schedule the first callback
        if (factory.isStartEager) {
            currentReferencePoint = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            scheduleCallback();
            isCallbackScheduled = true;
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
                currentBatch.remove(oldData[i]);
            }
        }

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
            return;
        }

        // If we have an empty window about to be filled for the first time, schedule a callback
        if (currentBatch.isEmpty()) {
            if (currentReferencePoint == null) {
                currentReferencePoint = factory.optionalReferencePoint;
                if (currentReferencePoint == null) {
                    currentReferencePoint = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
                }
            }

            // Schedule the next callback if there is none currently scheduled
            if (!isCallbackScheduled) {
                scheduleCallback();
                isCallbackScheduled = true;
            }
        }

        // add data points to the timeWindow
        for (EventBean newEvent : newData) {
            currentBatch.add(newEvent);
        }

        // We do not update child views, since we batch the events.
        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    /**
     * This method updates child views and clears the batch of events.
     * We schedule a new callback at this time if there were events in the batch.
     */
    protected void sendBatch() {
        isCallbackScheduled = false;

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

            if ((newData != null) || (oldData != null) || factory.isForceUpdate) {
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldData);
                child.update(newData, oldData);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        // Only if forceOutput is enabled or
        // there have been any events in this or the last interval do we schedule a callback,
        // such as to not waste resources when no events arrive.
        if ((!currentBatch.isEmpty()) || ((lastBatch != null) && (!lastBatch.isEmpty()))
                ||
                factory.isForceUpdate) {
            scheduleCallback();
            isCallbackScheduled = true;
        }

        lastBatch = currentBatch;
        currentBatch = new LinkedHashSet<EventBean>();
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
                " initialReferencePoint=" + factory.optionalReferencePoint;
    }

    protected void scheduleCallback() {
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        TimePeriodDeltaResult deltaWReference = timePeriodProvide.deltaAddWReference(current, currentReferencePoint, null, true, agentInstanceContext);
        long afterTime = deltaWReference.getDelta();
        currentReferencePoint = deltaWReference.getLastReference();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeBatchViewRStream.this.sendBatch();
                agentInstanceContext.getInstrumentationProvider().aViewScheduledEval();
            }
        };
        handle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.getAuditProvider().scheduleAdd(afterTime, agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
        agentInstanceContext.getStatementContext().getSchedulingService().add(afterTime, handle, scheduleSlot);
    }

    public void stop(AgentInstanceStopServices services) {
        if (handle != null) {
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, handle, ScheduleObjectType.view, factory.getViewName());
            agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
        }
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
