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
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodDeltaResult;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * A data view that aggregates events in a stream and releases them in one batch at every specified time interval.
 * The view works similar to a time_window but in not continuous.
 * The view releases the batched events after the interval as new data to child views. The prior batch if
 * not empty is released as old data to child view. The view doesn't release intervals with no old or new data.
 * It also does not collect old data published by a parent view.
 * <p>
 * For example, we want to calculate the average of IBM stock every hour, for the last hour.
 * The view accepts 2 parameter combinations.
 * (1) A time interval is supplied with a reference point - based on this point the intervals are set.
 * (1) A time interval is supplied but no reference point - the reference point is set when the first event arrives.
 * <p>
 * If there are no events in the current and prior batch, the view will not invoke the update method of child views.
 * In that case also, no next callback is scheduled with the scheduling service until the next event arrives.
 */
public class TimeBatchView extends ViewSupport implements AgentInstanceStopCallback, DataWindowView {
    // View parameters
    private final TimeBatchViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final ViewUpdatedCollection viewUpdatedCollection;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    private EPStatementHandleCallbackSchedule handle;
    private Long currentReferencePoint;
    private ArrayDeque<EventBean> lastBatch = null;
    private ArrayDeque<EventBean> currentBatch = new ArrayDeque<EventBean>();
    private boolean isCallbackScheduled;

    public TimeBatchView(TimeBatchViewFactory factory,
                         AgentInstanceViewFactoryChainContext agentInstanceContext,
                         ViewUpdatedCollection viewUpdatedCollection,
                         TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        // schedule the first callback
        if (factory.isStartEager) {
            if (currentReferencePoint == null) {
                currentReferencePoint = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            }
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
    private void sendBatch() {
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

            // Post new data (current batch) and old data (prior batch)
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(newData, oldData);
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
        currentBatch = new ArrayDeque<EventBean>();
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

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, true, factory.getViewName(), null);
    }

    private void scheduleCallback() {
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        TimePeriodDeltaResult deltaWReference = timePeriodProvide.deltaAddWReference(current, currentReferencePoint, null, true, agentInstanceContext);
        long afterTime = deltaWReference.getDelta();
        currentReferencePoint = deltaWReference.getLastReference();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                TimeBatchView.this.sendBatch();
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

    public ViewFactory getViewFactory() {
        return factory;
    }

    private static final Logger log = LoggerFactory.getLogger(TimeBatchView.class);
}
