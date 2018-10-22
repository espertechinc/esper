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
 * and also keeps a count of the number of matches so far, checking both count and timer,
 * letting all {@link MatchedEventMap} instances pass until then.
 */
public class TimerWithinOrMaxCountGuard implements Guard, ScheduleHandleCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "timer-within-max";
    private final long deltaTime;
    private final int numCountTo;
    private final Quitable quitable;
    private final long scheduleSlot;

    private int counter;
    private boolean isTimerActive;
    private EPStatementHandleCallbackSchedule scheduleHandle;

    /**
     * Ctor.
     *
     * @param deltaTime  - number of millisecond to guard expiration
     * @param numCountTo - max number of counts
     * @param quitable   - to use to indicate that the gaurd quitted
     */
    public TimerWithinOrMaxCountGuard(long deltaTime, int numCountTo, Quitable quitable) {
        this.deltaTime = deltaTime;
        this.numCountTo = numCountTo;
        this.quitable = quitable;
        this.scheduleSlot = quitable.getContext().getAgentInstanceContext().getScheduleBucket().allocateSlot();
    }

    public void startGuard() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        scheduleHandle = new EPStatementHandleCallbackSchedule(quitable.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
        agentInstanceContext.getAuditProvider().scheduleAdd(deltaTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        agentInstanceContext.getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
        counter = 0;
    }

    public boolean inspect(MatchedEventMap matchEvent) {
        counter++;
        if (counter > numCountTo) {
            quitable.guardQuit();
            deactivateTimer();
            return false;
        }
        return true;
    }

    public void stopGuard() {
        if (isTimerActive) {
            deactivateTimer();
        }
    }

    public void scheduledTrigger() {
        AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardScheduledEval();
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        // Timer callback is automatically removed when triggering
        isTimerActive = false;
        quitable.guardQuit();
        agentInstanceContext.getInstrumentationProvider().aPatternGuardScheduledEval();
    }

    public void accept(EventGuardVisitor visitor) {
        visitor.visitGuard(20, scheduleSlot);
    }

    private void deactivateTimer() {
        if (scheduleHandle != null) {
            AgentInstanceContext agentInstanceContext = quitable.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        scheduleHandle = null;
        isTimerActive = false;
    }
}