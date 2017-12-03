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
package com.espertech.esper.pattern.guard;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.schedule.ScheduleHandleCallback;

/**
 * Guard implementation that keeps a timer instance and quits when the timer expired,
 * letting all {@link MatchedEventMap} instances pass until then.
 */
public class TimerWithinGuard implements Guard, ScheduleHandleCallback {
    private final long deltaTime;
    private final Quitable quitable;
    private final long scheduleSlot;

    private boolean isTimerActive;
    private EPStatementHandleCallback scheduleHandle;

    /**
     * Ctor.
     *
     * @param delta    - number of millisecond to guard expiration
     * @param quitable - to use to indicate that the gaurd quitted
     */
    public TimerWithinGuard(long delta, Quitable quitable) {
        this.deltaTime = delta;
        this.quitable = quitable;
        this.scheduleSlot = quitable.getContext().getPatternContext().getScheduleBucket().allocateSlot();
    }

    public void startGuard() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        // Start the stopwatch timer
        scheduleHandle = new EPStatementHandleCallback(quitable.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        quitable.getContext().getPatternContext().getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
    }

    public void stopGuard() {
        if (isTimerActive) {
            quitable.getContext().getPatternContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
            scheduleHandle = null;
            isTimerActive = false;
        }
    }

    public boolean inspect(MatchedEventMap matchEvent) {
        // no need to test: for timing only, if the timer expired the guardQuit stops any events from coming here
        return true;
    }

    public final void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternGuardScheduledEval();
        }
        // Timer callback is automatically removed when triggering
        isTimerActive = false;
        quitable.guardQuit();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternGuardScheduledEval();
        }
    }

    public void accept(EventGuardVisitor visitor) {
        visitor.visitGuard(10, scheduleSlot);
    }
}
