/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.pattern.observer;

import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.core.service.EngineLevelExtensionServicesContext;
import com.espertech.esper.epl.datetime.calop.CalendarOpPlusFastAddHelper;
import com.espertech.esper.epl.datetime.calop.CalendarOpPlusFastAddResult;
import com.espertech.esper.epl.datetime.calop.CalendarOpPlusMinus;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.pattern.MatchedEventMap;
import com.espertech.esper.schedule.ScheduleHandleCallback;
import com.espertech.esper.schedule.SchedulingService;

import java.util.Calendar;

/**
 * Observer implementation for indicating that a certain time arrived, similar to "crontab".
 */
public class TimerScheduleObserver implements EventObserver, ScheduleHandleCallback
{
    protected final long scheduleSlot;
    protected MatchedEventMap beginState;
    protected final ObserverEventEvaluator observerEventEvaluator;
    private final TimerScheduleSpec spec;
    private final boolean isFilterChildNonQuitting;

    // we always keep the anchor time, which could be engine time or the spec time, and never changes in computations
    protected Calendar anchorTime;

    // for fast computation, keep some last-value information around for the purpose of caching
    protected boolean isTimerActive = false;
    private Calendar cachedLastScheduled;
    private long cachedCountRepeated = 0;

    protected EPStatementHandleCallback scheduleHandle;

    /**
     * Ctor.
     * @param beginState - start state
     * @param observerEventEvaluator - receiver for events
     */
    public TimerScheduleObserver(TimerScheduleSpec spec, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, boolean isFilterChildNonQuitting)
    {
        this.beginState = beginState;
        this.observerEventEvaluator = observerEventEvaluator;
        this.scheduleSlot = observerEventEvaluator.getContext().getPatternContext().getScheduleBucket().allocateSlot();
        this.spec = spec;
        this.isFilterChildNonQuitting = isFilterChildNonQuitting;
    }

    public MatchedEventMap getBeginState() {
        return beginState;
    }

    public final void scheduledTrigger(EngineLevelExtensionServicesContext engineLevelExtensionServicesContext)
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qPatternObserverScheduledEval();}

        // compute reschedule time
        isTimerActive = false;
        SchedulingService schedulingService = observerEventEvaluator.getContext().getPatternContext().getSchedulingService();
        long nextScheduledTime = computeNextSetLastScheduled(schedulingService.getTime());

        boolean quit = !isFilterChildNonQuitting || nextScheduledTime == -1;
        observerEventEvaluator.observerEvaluateTrue(beginState, quit);

        // handle no more invocations planned
        if (nextScheduledTime == -1) {
            stopObserve();
            observerEventEvaluator.observerEvaluateFalse(false);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aPatternObserverScheduledEval();}
            return;
        }

        schedulingService.add(nextScheduledTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aPatternObserverScheduledEval();}
    }

    public void startObserve()
    {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        SchedulingService schedulingService = observerEventEvaluator.getContext().getPatternContext().getSchedulingService();
        if (anchorTime == null) {
            if (spec.getOptionalDate() == null) {
                anchorTime = Calendar.getInstance(observerEventEvaluator.getContext().getStatementContext().getEngineImportService().getTimeZone());
                anchorTime.setTimeInMillis(schedulingService.getTime());
            }
            else {
                anchorTime = spec.getOptionalDate();
            }
        }

        long nextScheduledTime = computeNextSetLastScheduled(schedulingService.getTime());
        if (nextScheduledTime == -1) {
            stopObserve();
            observerEventEvaluator.observerEvaluateFalse(false);
            return;
        }

        scheduleHandle = new EPStatementHandleCallback(observerEventEvaluator.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        schedulingService.add(nextScheduledTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
    }

    public void stopObserve()
    {
        if (isTimerActive) {
            observerEventEvaluator.getContext().getPatternContext().getSchedulingService().remove(scheduleHandle, scheduleSlot);
        }
        isTimerActive = false;
        scheduleHandle = null;
        cachedCountRepeated = Long.MAX_VALUE;
        cachedLastScheduled = null;
        anchorTime = null;
    }

    public void accept(EventObserverVisitor visitor) {
        visitor.visitObserver(beginState, 2, scheduleSlot, spec, anchorTime, cachedCountRepeated, cachedLastScheduled, isTimerActive);
    }

    private long computeNextSetLastScheduled(long currentTime) {

        // handle already-stopped
        if (cachedCountRepeated == Long.MAX_VALUE) {
            return -1;
        }

        // handle date-only-form: "<date>"
        if (spec.getOptionalRepeatCount() == null && spec.getOptionalDate() != null && spec.getOptionalTimePeriod() == null) {
            cachedCountRepeated = Long.MAX_VALUE;
            if (anchorTime.getTimeInMillis() > currentTime) {
                return anchorTime.getTimeInMillis() - currentTime;
            }
            return -1;
        }

        // handle period-only-form: "P<period>"
        // handle partial-form-2: "<date>/<period>" (non-recurring)
        if (spec.getOptionalRepeatCount() == null && spec.getOptionalTimePeriod() != null) {
            cachedCountRepeated = Long.MAX_VALUE;
            cachedLastScheduled = (Calendar) anchorTime.clone();
            CalendarOpPlusMinus.action(cachedLastScheduled, 1, spec.getOptionalTimePeriod());
            if (cachedLastScheduled.getTimeInMillis() > currentTime) {
                return cachedLastScheduled.getTimeInMillis() - currentTime;
            }
            return -1;
        }

        // handle partial-form-1: "R<?>/<period>"
        // handle full form
        if (cachedLastScheduled == null) {
            cachedLastScheduled = (Calendar) anchorTime.clone();
            if (spec.getOptionalDate() != null) {
                cachedCountRepeated = 1;
            }
        }

        if (spec.getOptionalRepeatCount() == -1) {
            CalendarOpPlusFastAddResult nextDue = CalendarOpPlusFastAddHelper.computeNextDue(currentTime, spec.getOptionalTimePeriod(), cachedLastScheduled);
            cachedLastScheduled = nextDue.getScheduled();
            return cachedLastScheduled.getTimeInMillis() - currentTime;
        }

        CalendarOpPlusFastAddResult nextDue = CalendarOpPlusFastAddHelper.computeNextDue(currentTime, spec.getOptionalTimePeriod(), cachedLastScheduled);
        cachedCountRepeated += nextDue.getFactor();
        if (cachedCountRepeated <= spec.getOptionalRepeatCount()) {
            cachedLastScheduled = nextDue.getScheduled();
            if (cachedLastScheduled.getTimeInMillis() > currentTime) {
                return cachedLastScheduled.getTimeInMillis() - currentTime;
            }
        }
        cachedCountRepeated = Long.MAX_VALUE;
        return -1;
    }
}
