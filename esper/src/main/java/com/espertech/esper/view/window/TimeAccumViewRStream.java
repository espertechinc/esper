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
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * A data window view that holds events in a stream and only removes events from a stream (rstream) if
 * no more events arrive for a given time interval, also handling the remove stream
 * by keeping set-like semantics. See {@link TimeAccumView} for the same behavior without
 * remove stream handling.
 */
public class TimeAccumViewRStream extends ViewSupport implements DataWindowView, StoppableView, StopCallback {
    // View parameters
    private final TimeAccumViewFactory factory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    protected final long scheduleSlot;

    // Current running parameters
    protected LinkedHashMap<EventBean, Long> currentBatch = new LinkedHashMap<EventBean, Long>();
    protected EventBean lastEvent;
    protected long callbackScheduledTime;
    protected EPStatementHandleCallback handle;

    /**
     * Constructor.
     *
     * @param timeBatchViewFactory for copying this view in a group-by
     * @param agentInstanceContext context
     * @param timeDeltaComputation time delta eval
     */
    public TimeAccumViewRStream(TimeAccumViewFactory timeBatchViewFactory,
                                AgentInstanceViewFactoryChainContext agentInstanceContext,
                                ExprTimePeriodEvalDeltaConst timeDeltaComputation) {
        this.agentInstanceContext = agentInstanceContext;
        this.factory = timeBatchViewFactory;
        this.timeDeltaComputation = timeDeltaComputation;

        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(TimeAccumViewRStream.this, TimeAccumViewRStream.this.factory.getViewName());
                }
                TimeAccumViewRStream.this.sendRemoveStream();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewScheduledEval();
                }
            }
        };
        handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.addTerminationCallback(this);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, factory.getViewName(), newData, oldData);
        }

        if ((newData != null) && (newData.length > 0)) {
            // If we have an empty window about to be filled for the first time, add a callback
            boolean removeSchedule = false;
            boolean addSchedule = false;
            long timestamp = agentInstanceContext.getStatementContext().getSchedulingService().getTime();

            // if the window is already filled, then we may need to reschedule
            if (!currentBatch.isEmpty()) {
                // check if we need to reschedule
                long callbackTime = timestamp + timeDeltaComputation.deltaAdd(timestamp);
                if (callbackTime != callbackScheduledTime) {
                    removeSchedule = true;
                    addSchedule = true;
                }
            } else {
                addSchedule = true;
            }

            if (removeSchedule) {
                agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                callbackScheduledTime = -1;
            }
            if (addSchedule) {
                long timeIntervalSize = timeDeltaComputation.deltaAdd(timestamp);
                agentInstanceContext.getStatementContext().getSchedulingService().add(timeIntervalSize, handle, scheduleSlot);
                callbackScheduledTime = timeIntervalSize + timestamp;
            }

            // add data points to the window
            for (int i = 0; i < newData.length; i++) {
                currentBatch.put(newData[i], timestamp);
                internalHandleAdded(newData[i], timestamp);
                lastEvent = newData[i];
            }
        }

        if ((oldData != null) && (oldData.length > 0)) {
            boolean removedLastEvent = false;
            for (EventBean anOldData : oldData) {
                currentBatch.remove(anOldData);
                internalHandleRemoved(anOldData);
                if (anOldData == lastEvent) {
                    removedLastEvent = true;
                }
            }

            // we may need to reschedule as the newest event may have been deleted
            if (currentBatch.size() == 0) {
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
                    long callbackTime = lastTimestamp + timeDeltaComputation.deltaAdd(lastTimestamp);
                    long deltaFromNow = callbackTime - timestamp;
                    if (callbackTime != callbackScheduledTime) {
                        agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                        agentInstanceContext.getStatementContext().getSchedulingService().add(deltaFromNow, handle, scheduleSlot);
                        callbackScheduledTime = callbackTime;
                    }
                }
            }
        }

        // update child views
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), newData, oldData);
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

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, factory.getViewName(), currentBatch.size(), null);
    }

    /**
     * This method sends the remove stream for all accumulated events.
     */
    protected void sendRemoveStream() {
        callbackScheduledTime = -1;

        // If there are child views and the batch was filled, fireStatementStopped update method
        if (this.hasViews()) {
            // Convert to object arrays
            EventBean[] oldData = null;
            if (!currentBatch.isEmpty()) {
                oldData = currentBatch.keySet().toArray(new EventBean[currentBatch.size()]);
            }

            if (oldData != null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), null, oldData);
                }
                updateChildren(null, oldData);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
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
    }

    public void internalHandleRemoved(EventBean anOldData) {
        // no action required
    }

    public void internalHandleAdded(EventBean eventBean, long timestamp) {
        // no action required
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
