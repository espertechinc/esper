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
package com.espertech.esper.pattern.observer;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.schedule.ScheduleHandleCallback;

/**
 * Observer that will wait a certain interval before indicating true (raising an event).
 */
public class TimerIntervalObserver implements EventObserver, ScheduleHandleCallback {
    private final long deltaTime;
    private final MatchedEventMap beginState;
    private final ObserverEventEvaluator observerEventEvaluator;
    private final long scheduleSlot;

    private boolean isTimerActive = false;
    private EPStatementHandleCallback scheduleHandle;

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
        this.scheduleSlot = observerEventEvaluator.getContext().getPatternContext().getScheduleBucket().allocateSlot();
    }

    public final void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternObserverScheduledEval();
        }
        observerEventEvaluator.observerEvaluateTrue(beginState, true);
        isTimerActive = false;
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternObserverScheduledEval();
        }
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
            scheduleHandle = new EPStatementHandleCallback(observerEventEvaluator.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
            observerEventEvaluator.getContext().getPatternContext().getSchedulingService().add(deltaTime, scheduleHandle, scheduleSlot);
            isTimerActive = true;
        }
    }

    public void stopObserve() {
        if (isTimerActive) {
            observerEventEvaluator.getContext().getPatternContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
            isTimerActive = false;
            scheduleHandle = null;
        }
    }

    public void accept(EventObserverVisitor visitor) {
        visitor.visitObserver(beginState, 10, scheduleSlot);
    }
}
