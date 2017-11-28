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
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaResult;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Same as the {@link TimeBatchView}, this view also supports fast-remove from the batch for remove stream events.
 */
public class TimeBatchViewRStream extends ViewSupport implements StoppableView, StopCallback, DataWindowView {
    // View parameters
    private final TimeBatchViewFactory timeBatchViewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    private final Long initialReferencePoint;
    protected final long scheduleSlot;
    private final boolean isForceOutput;
    private final boolean isStartEager;
    protected EPStatementHandleCallback handle;

    // Current running parameters
    protected Long currentReferencePoint;
    protected LinkedHashSet<EventBean> lastBatch = null;
    protected LinkedHashSet<EventBean> currentBatch = new LinkedHashSet<EventBean>();
    protected boolean isCallbackScheduled;

    /**
     * Constructor.
     *
     * @param timeDeltaComputation is the number of milliseconds to batch events for
     * @param referencePoint       is the reference point onto which to base intervals, or null if
     *                             there is no such reference point supplied
     * @param timeBatchViewFactory fr copying this view in a group-by
     * @param forceOutput          is true if the batch should produce empty output if there is no value to output following time intervals
     * @param isStartEager         is true for start-eager
     * @param agentInstanceContext context
     */
    public TimeBatchViewRStream(TimeBatchViewFactory timeBatchViewFactory,
                                AgentInstanceViewFactoryChainContext agentInstanceContext,
                                ExprTimePeriodEvalDeltaConst timeDeltaComputation,
                                Long referencePoint,
                                boolean forceOutput,
                                boolean isStartEager) {
        this.agentInstanceContext = agentInstanceContext;
        this.timeBatchViewFactory = timeBatchViewFactory;
        this.timeDeltaComputation = timeDeltaComputation;
        this.initialReferencePoint = referencePoint;
        this.isStartEager = isStartEager;
        this.isForceOutput = forceOutput;

        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();

        // schedule the first callback
        if (this.isStartEager) {
            currentReferencePoint = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
            scheduleCallback();
            isCallbackScheduled = true;
        }
        agentInstanceContext.addTerminationCallback(this);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    /**
     * Gets the reference point to use to anchor interval start and end dates to.
     *
     * @return is the millisecond reference point.
     */
    public final Long getInitialReferencePoint() {
        return initialReferencePoint;
    }

    /**
     * True for force-output.
     *
     * @return indicates force-output
     */
    public boolean isForceOutput() {
        return isForceOutput;
    }

    /**
     * True for start-eager.
     *
     * @return indicates start-eager
     */
    public boolean isStartEager() {
        return isStartEager;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, timeBatchViewFactory.getViewName(), newData, oldData);
        }

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                currentBatch.remove(oldData[i]);
                internalHandleRemoved(oldData[i]);
            }
        }

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewProcessIRStream();
            }
            return;
        }

        // If we have an empty window about to be filled for the first time, schedule a callback
        if (currentBatch.isEmpty()) {
            if (currentReferencePoint == null) {
                currentReferencePoint = initialReferencePoint;
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    /**
     * This method updates child views and clears the batch of events.
     * We schedule a new callback at this time if there were events in the batch.
     */
    protected void sendBatch() {
        isCallbackScheduled = false;

        // If there are child views and the batch was filled, fireStatementStopped update method
        if (this.hasViews()) {
            // Convert to object arrays
            EventBean[] newData = null;
            EventBean[] oldData = null;
            if (!currentBatch.isEmpty()) {
                newData = currentBatch.toArray(new EventBean[currentBatch.size()]);
            }
            if ((lastBatch != null) && (!lastBatch.isEmpty())) {
                oldData = lastBatch.toArray(new EventBean[lastBatch.size()]);
            }

            if ((newData != null) || (oldData != null) || isForceOutput) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, timeBatchViewFactory.getViewName(), newData, oldData);
                }
                updateChildren(newData, oldData);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }

        // Only if forceOutput is enabled or
        // there have been any events in this or the last interval do we schedule a callback,
        // such as to not waste resources when no events arrive.
        if ((!currentBatch.isEmpty()) || ((lastBatch != null) && (!lastBatch.isEmpty()))
                ||
                isForceOutput) {
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
                " initialReferencePoint=" + initialReferencePoint;
    }

    protected void scheduleCallback() {
        long current = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        ExprTimePeriodEvalDeltaResult deltaWReference = timeDeltaComputation.deltaAddWReference(current, currentReferencePoint);
        long afterTime = deltaWReference.getDelta();
        currentReferencePoint = deltaWReference.getLastReference();

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(TimeBatchViewRStream.this, timeBatchViewFactory.getViewName());
                }
                TimeBatchViewRStream.this.sendBatch();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewScheduledEval();
                }
            }
        };
        handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        agentInstanceContext.getStatementContext().getSchedulingService().add(afterTime, handle, scheduleSlot);
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

    public void internalHandleRemoved(EventBean eventBean) {
        // no action required
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(currentBatch, true, timeBatchViewFactory.getViewName(), null);
        viewDataVisitor.visitPrimary(lastBatch, true, timeBatchViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return timeBatchViewFactory;
    }
}
