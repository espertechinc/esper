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
package com.espertech.esper.common.internal.view.firsttime;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodProvide;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class FirstTimeView extends ViewSupport implements DataWindowView, AgentInstanceStopCallback {
    private final FirstTimeViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private EPStatementHandleCallbackSchedule handle;
    private final long scheduleSlot;
    private final TimePeriodProvide timePeriodProvide;

    // Current running parameters
    private LinkedHashSet<EventBean> events = new LinkedHashSet<EventBean>();
    private boolean isClosed;

    public FirstTimeView(FirstTimeViewFactory factory,
                         AgentInstanceViewFactoryChainContext agentInstanceContext,
                         TimePeriodProvide timePeriodProvide) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.scheduleSlot = agentInstanceContext.getStatementContext().getScheduleBucket().allocateSlot();
        this.timePeriodProvide = timePeriodProvide;

        scheduleCallback();
    }

    public final EventType getEventType() {
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        OneEventCollection oldDataToPost = null;
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                boolean removed = events.remove(anOldData);
                if (removed) {
                    if (oldDataToPost == null) {
                        oldDataToPost = new OneEventCollection();
                    }
                    oldDataToPost.add(anOldData);
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
            }
        }

        // If there are child views, call update method
        if ((child != null) && ((newDataToPost != null) || (oldDataToPost != null))) {
            EventBean[] nd = (newDataToPost != null) ? newDataToPost.toArray() : null;
            EventBean[] od = (oldDataToPost != null) ? oldDataToPost.toArray() : null;
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, nd, od);
            child.update(nd, od);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
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
        long afterTime = timePeriodProvide.deltaAdd(agentInstanceContext.getStatementContext().getSchedulingService().getTime(), null, true, agentInstanceContext);

        ScheduleHandleCallback callback = new ScheduleHandleCallback() {
            public void scheduledTrigger() {
                agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.view, factory.getViewName());
                agentInstanceContext.getInstrumentationProvider().qViewScheduledEval(factory);
                FirstTimeView.this.isClosed = true;
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

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public LinkedHashSet<EventBean> getEvents() {
        return events;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(events, true, factory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
