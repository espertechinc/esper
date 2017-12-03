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
 * and also keeps a count of the number of matches so far, checking both count and timer,
 * letting all {@link MatchedEventMap} instances pass until then.
 */
public class TimerWithinOrMaxCountGuard implements Guard, ScheduleHandleCallback {
    private final long deltaTime;
    private final int numCountTo;
    private final Quitable quitable;
    private final long scheduleSlot;

    private int counter;
    private boolean isTimerActive;
    private EPStatementHandleCallback scheduleHandle;

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
        this.scheduleSlot = quitable.getContext().getPatternContext().getScheduleBucket().allocateSlot();
    }

    public void startGuard() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        scheduleHandle = new EPStatementHandleCallback(quitable.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        quitable.getContext().getPatternContext().getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
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

    public void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext) {
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
        visitor.visitGuard(20, scheduleSlot);
    }

    private void deactivateTimer() {
        if (scheduleHandle != null) {
            quitable.getContext().getPatternContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        scheduleHandle = null;
        isTimerActive = false;
    }
}