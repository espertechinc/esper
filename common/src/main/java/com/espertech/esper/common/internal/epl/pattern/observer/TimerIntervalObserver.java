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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;

/**
 * Observer that will wait a certain interval before indicating true (raising an event).
 */
public class TimerIntervalObserver implements EventObserver, ScheduleHandleCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "timer-interval";

    private final long deltaTime;
    private final MatchedEventMap beginState;
    private final ObserverEventEvaluator observerEventEvaluator;
    private final long scheduleSlot;

    private boolean isTimerActive = false;
    private EPStatementHandleCallbackSchedule scheduleHandle;

    /**
     * Ctor.
     *
     * @param deltaTime              - the time deltaTime
     * @param beginState             - start state
     * @param observerEventEvaluator - receiver for events
     */
    public TimerIntervalObserver(long deltaTime, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator) {
        this.deltaTime = deltaTime;
        this.beginState = beginState;
        this.observerEventEvaluator = observerEventEvaluator;
        this.scheduleSlot = observerEventEvaluator.getContext().getAgentInstanceContext().getScheduleBucket().allocateSlot();
    }

    public final void scheduledTrigger() {
        AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverScheduledEval();
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        observerEventEvaluator.observerEvaluateTrue(beginState, true);
        isTimerActive = false;
        agentInstanceContext.getInstrumentationProvider().aPatternObserverScheduledEval();
    }

    public MatchedEventMap getBeginState() {
        return beginState;
    }

    public void startObserve() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        if (deltaTime <= 0) {
            observerEventEvaluator.observerEvaluateTrue(beginState, true);
        } else {
            AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
            scheduleHandle = new EPStatementHandleCallbackSchedule(agentInstanceContext.getEpStatementAgentInstanceHandle(), this);
            agentInstanceContext.getAuditProvider().scheduleAdd(deltaTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
            observerEventEvaluator.getContext().getStatementContext().getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
            isTimerActive = true;
        }
    }

    public void stopObserve() {
        if (isTimerActive) {
            AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
            isTimerActive = false;
            scheduleHandle = null;
        }
    }

    public void accept(EventObserverVisitor visitor) {
        visitor.visitObserver(beginState, 10, scheduleSlot);
    }
}
