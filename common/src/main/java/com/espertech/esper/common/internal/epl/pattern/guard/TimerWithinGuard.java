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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;

/**
 * Guard implementation that keeps a timer instance and quits when the timer expired,
 * letting all {@link MatchedEventMap} instances pass until then.
 */
public class TimerWithinGuard implements Guard, ScheduleHandleCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "timer-within";
    private final long deltaTime;
    private final Quitable quitable;
    private final long scheduleSlot;

    private boolean isTimerActive;
    private EPStatementHandleCallbackSchedule scheduleHandle;

    /**
     * Ctor.
     *
     * @param delta    - number of millisecond to guard expiration
     * @param quitable - to use to indicate that the gaurd quitted
     */
    public TimerWithinGuard(long delta, Quitable quitable) {
        this.deltaTime = delta;
        this.quitable = quitable;
        this.scheduleSlot = quitable.getContext().getAgentInstanceContext().getScheduleBucket().allocateSlot();
    }

    public void startGuard() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        // Start the stopwatch timer
        scheduleHandle = new EPStatementHandleCallbackSchedule(quitable.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
        agentInstanceContext.getAuditProvider().scheduleAdd(deltaTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
    }

    public void stopGuard() {
        if (isTimerActive) {
            AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
            scheduleHandle = null;
            isTimerActive = false;
        }
    }

    public boolean inspect(MatchedEventMap matchEvent) {
        // no need to test: for timing only, if the timer expired the guardQuit stops any events from coming here
        return true;
    }

    public final void scheduledTrigger() {
        // Timer callback is automatically removed when triggering
        AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardScheduledEval();
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        isTimerActive = false;
        quitable.guardQuit();
        agentInstanceContext.getInstrumentationProvider().aPatternGuardScheduledEval();
    }

    public void accept(EventGuardVisitor visitor) {
        visitor.visitGuard(10, scheduleSlot);
    }
}
