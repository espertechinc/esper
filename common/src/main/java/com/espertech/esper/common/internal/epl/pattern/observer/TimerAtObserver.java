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
import com.espertech.esper.common.internal.schedule.*;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

/**
 * Observer implementation for indicating that a certain time arrived, similar to "crontab".
 */
public class TimerAtObserver implements EventObserver, ScheduleHandleCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "timer-at";

    private final ScheduleSpec scheduleSpec;
    private final long scheduleSlot;
    private final MatchedEventMap beginState;
    private final ObserverEventEvaluator observerEventEvaluator;

    private boolean isTimerActive = false;
    private EPStatementHandleCallbackSchedule scheduleHandle;

    /**
     * Ctor.
     *
     * @param scheduleSpec           - specification containing the crontab schedule
     * @param beginState             - start state
     * @param observerEventEvaluator - receiver for events
     */
    public TimerAtObserver(ScheduleSpec scheduleSpec, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator) {
        this.scheduleSpec = scheduleSpec;
        this.beginState = beginState;
        this.observerEventEvaluator = observerEventEvaluator;
        this.scheduleSlot = observerEventEvaluator.getContext().getAgentInstanceContext().getScheduleBucket().allocateSlot();
    }

    public MatchedEventMap getBeginState() {
        return beginState;
    }

    public final void scheduledTrigger() {
        AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverScheduledEval();
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        observerEventEvaluator.observerEvaluateTrue(beginState, true);
        isTimerActive = false;
        agentInstanceContext.getInstrumentationProvider().aPatternObserverScheduledEval();
    }

    public void startObserve() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        scheduleHandle = new EPStatementHandleCallbackSchedule(observerEventEvaluator.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
        SchedulingService schedulingService = agentInstanceContext.getSchedulingService();
        ClasspathImportServiceRuntime classpathImportService = agentInstanceContext.getClasspathImportServiceRuntime();
        long nextScheduledTime = ScheduleComputeHelper.computeDeltaNextOccurance(scheduleSpec, schedulingService.getTime(), classpathImportService.getTimeZone(), classpathImportService.getTimeAbacus());
        agentInstanceContext.getAuditProvider().scheduleAdd(nextScheduledTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        schedulingService.add(nextScheduledTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
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
        visitor.visitObserver(beginState, 2, scheduleSlot, scheduleSpec);
    }
}
