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
import com.espertech.esper.collection.OneEventCollection;
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

import java.util.Iterator;
import java.util.LinkedHashSet;

public class FirstTimeView extends ViewSupport implements StoppableView, DataWindowView, StopCallback {
    private final FirstTimeViewFactory timeFirstViewFactory;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    protected final ExprTimePeriodEvalDeltaConst timeDeltaComputation;
    protected final long scheduleSlot;
    protected EPStatementHandleCallback handle;

    // Current running parameters
    protected LinkedHashSet<EventBean> events = new LinkedHashSet<EventBean>();
    protected boolean isClosed;

    /**
     * Constructor.
     *
     * @param timeFirstViewFactory fr copying this view in a group-by
     * @param agentInstanceContext context
     * @param timeDeltaComputation delta eval
     */
    public FirstTimeView(FirstTimeViewFactory timeFirstViewFactory,
                         AgentInstanceViewFactoryChainContext agentInstanceContext,
                         ExprTimePeriodEvalDeltaConst timeDeltaComputation) {
        this.agentInstanceContext = agentInstanceContext;
        this.timeFirstViewFactory = timeFirstViewFactory;
        this.timeDeltaComputation = timeDeltaComputation;

        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();

        scheduleCallback();

        agentInstanceContext.addTerminationCallback(this);
    }

    public ExprTimePeriodEvalDeltaConst getTimeDeltaComputation() {
        return timeDeltaComputation;
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, timeFirstViewFactory.getViewName(), newData, oldData);
        }

        OneEventCollection oldDataToPost = null;
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                boolean removed = events.remove(anOldData);
                if (removed) {
                    if (oldDataToPost == null) {
                        oldDataToPost = new OneEventCollection();
                    }
                    oldDataToPost.add(anOldData);
                    internalHandleRemoved(anOldData);
                }
            }
        }

        // add data points to the timeWindow
        OneEventCollection newDataToPost = null;
        if ((!isClosed) && (newData != null)) {
            for (EventBean aNewData : newData) {
                events.add(aNewData);
                if (newDataToPost == null) {
                    newDataToPost = new OneEventCollection();
                }
                newDataToPost.add(aNewData);
                internalHandleAdded(aNewData);
            }
        }

        // If there are child views, call update method
        if ((this.hasViews()) && ((newDataToPost != null) || (oldDataToPost != null))) {
            EventBean[] nd = (newDataToPost != null) ? newDataToPost.toArray() : null;
            EventBean[] od = (oldDataToPost != null) ? oldDataToPost.toArray() : null;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, timeFirstViewFactory.getViewName(), nd, od);
            }
            updateChildren(nd, od);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public void internalHandleAdded(EventBean newEvent) {
        // no action
    }

    public void internalHandleRemoved(EventBean oldEvent) {
        // no action
    }

    public void internalHandleClosed() {
        // no action
    }


    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    public final Iterator<EventBean> iterator() {
        return events.iterator();
    }

    public final String toString() {
        return this.getClass().getName();
    }

    private void scheduleCallback() {
        long afterTime = timeDeltaComputation.deltaAdd(agentInstanceContext.getStatementContext().getSchedulingService().getTime());

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger(EngineLevelExtensionServicesContext extensionServicesContext) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().qViewScheduledEval(FirstTimeView.this, timeFirstViewFactory.getViewName());
                }
                FirstTimeView.this.isClosed = true;
                internalHandleClosed();
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

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public LinkedHashSet<EventBean> getEvents() {
        return events;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(events, true, timeFirstViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return timeFirstViewFactory;
    }

    private static final Logger log = LoggerFactory.getLogger(TimeBatchViewRStream.class);
}
