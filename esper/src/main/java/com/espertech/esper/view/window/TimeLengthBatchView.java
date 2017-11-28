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
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.expression.time.ExprTimePeriodEvalDeltaConst;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TimeLengthBatchView extends ViewSupport implements StoppableView, StopCallback, DataWindowView {
    private static final Logger log = LoggerFactory.getLogger(TimeLengthBatchView.class);

    // View parameters
    private final TimeLengthBatchViewFactory timeLengthBatchViewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    protected final long numberOfEvents;
    protected final boolean isForceOutput;
    protected final boolean isStartEager;
    protected final ViewUpdatedCollection viewUpdatedCollection;
    protected final long scheduleSlot;

    // Current running parameters
    protected ArrayList<EventBean> lastBatch = null;
    protected ArrayList<EventBean> currentBatch = new ArrayList<EventBean>();
    protected Long callbackScheduledTime;
    protected EPStatementHandleCallback handle;

    /**
     * Constructor.
     *
     * @param numberOfEvents        is the event count before the batch fires off
     * @param viewUpdatedCollection is a collection that the view must update when receiving events
     * @param timeBatchViewFactory  for copying this view in a group-by
     * @param forceOutput           is true if the batch should produce empty output if there is no value to output following time intervals
     * @param isStartEager          is true for start-eager
     * @param agentInstanceContext  context
     * @param timeDeltaComputation  time delta eval
     */
    public TimeLengthBatchView(TimeLengthBatchViewFactory timeBatchViewFactory,
                               AgentInstanceViewFactoryChainContext agentInstanceContext,
                               ExprTimePeriodEvalDeltaConst timeDeltaComputation,
                               long numberOfEvents,
                               boolean forceOutput,
                               boolean isStartEager,
                               ViewUpdatedCollection viewUpdatedCollection) {
        this.agentInstanceContext = agentInstanceContext;
        this.timeLengthBatchViewFactory = timeBatchViewFactory;
        this.timeDeltaComputation = timeDeltaComputation;
        this.numberOfEvents = numberOfEvents;
        this.isStartEager = isStartEager;
        this.viewUpdatedCollection = viewUpdatedCollection;
        this.isForceOutput = forceOutput;

        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();

        // schedule the first callback
        if (isStartEager) {
            scheduleCallback(0);
        }

        agentInstanceContext.addTerminationCallback(this);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
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
     * Returns the length of the batch.
     *
     * @return maximum number of events allowed before window gets flushed
     */
    public long getNumberOfEvents() {
        return numberOfEvents;
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
            InstrumentationHelper.get().qViewProcessIRStream(this, timeLengthBatchViewFactory.getViewName(), newData, oldData);
        }

        if (oldData != null) {
            for (int i = 0; i < oldData.length; i++) {
                if (currentBatch.remove(oldData[i])) {
                    internalHandleRemoved(oldData[i]);
                }
            }
        }

        // we don't care about removed data from a prior view
        if ((newData == null) || (newData.length == 0)) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewProcessIRStream();
            }
            return;
        }

        // Add data points
        for (EventBean newEvent : newData) {
            currentBatch.add(newEvent);
            internalHandleAdded(newEvent);
        }

        // We are done unless we went over the boundary
        if (currentBatch.size() < numberOfEvents) {
            // Schedule a callback if there is none scheduled
            if (callbackScheduledTime == null) {
                scheduleCallback(0);
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewProcessIRStream();
            }
            return;
        }

        // send a batch of events
        sendBatch(false);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
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
                agentInstanceContext.getStatementContext().getSchedulingService().remove(handle, scheduleSlot);
                callbackScheduledTime = null;
            }
        }

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

            // Post new data (current batch) and old data (prior batch)
            if (viewUpdatedCollection != null) {
                viewUpdatedCollection.update(newData, oldData);
            }
            if ((newData != null) || (oldData != null) || isForceOutput) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewIndicate(this, timeLengthBatchViewFactory.getViewName(), newData, oldData);
                }
                updateChildren(newData, oldData);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewIndicate();
                }
            }
        }

        // Only if there have been any events in this or the last interval do we schedule a callback,
        // such as to not waste resources when no events arrive.
        if (((!currentBatch.isEmpty()) || ((lastBatch != null) && (!lastBatch.isEmpty())))
                ||
                isForceOutput) {
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
                " numberOfEvents=" + numberOfEvents;
    }

    protected void scheduleCallback(long delta) {
        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(TimeLengthBatchView.this, timeLengthBatchViewFactory.getViewName());
                }
                TimeLengthBatchView.this.sendBatch(true);
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aViewScheduledEval();
                }
            }
        };
        handle = new EPStatementHandleCallback(agentInstanceContext.getEpStatementAgentInstanceHandle(), callback);
        long currentTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime();
        long scheduled = timeDeltaComputation.deltaAdd(currentTime) - delta;
        agentInstanceContext.getStatementContext().getSchedulingService().add(scheduled, handle, scheduleSlot);
        callbackScheduledTime = agentInstanceContext.getStatementContext().getSchedulingService().getTime() + scheduled;
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

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(lastBatch, true, timeLengthBatchViewFactory.getViewName(), null);
        viewDataVisitor.visitPrimary(currentBatch, true, timeLengthBatchViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return timeLengthBatchViewFactory;
    }
}
