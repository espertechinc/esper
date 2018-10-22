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
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarOpPlusFastAddHelper;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarOpPlusFastAddResult;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarPlusMinusForgeOp;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallback;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.schedule.SchedulingService;

import java.util.Calendar;

/**
 * Observer implementation for indicating that a certain time arrived, similar to "crontab".
 */
public class TimerScheduleObserver implements EventObserver, ScheduleHandleCallback {
    public final static String NAME_AUDITPROVIDER_SCHEDULE = "timer-at";
    protected final long scheduleSlot;
    protected MatchedEventMap beginState;
    protected final ObserverEventEvaluator observerEventEvaluator;
    private final TimerScheduleSpec spec;
    private final boolean isFilterChildNonQuitting;

    // we always keep the anchor time, which could be runtime time or the spec time, and never changes in computations
    protected Calendar anchorTime;
    protected long anchorRemainder;

    // for fast computation, keep some last-value information around for the purpose of caching
    protected boolean isTimerActive = false;
    private Calendar cachedLastScheduled;
    private long cachedCountRepeated = 0;

    protected EPStatementHandleCallbackSchedule scheduleHandle;

    public TimerScheduleObserver(TimerScheduleSpec spec, MatchedEventMap beginState, ObserverEventEvaluator observerEventEvaluator, boolean isFilterChildNonQuitting) {
        this.beginState = beginState;
        this.observerEventEvaluator = observerEventEvaluator;
        this.scheduleSlot = observerEventEvaluator.getContext().getAgentInstanceContext().getScheduleBucket().allocateSlot();
        this.spec = spec;
        this.isFilterChildNonQuitting = isFilterChildNonQuitting;
    }

    public MatchedEventMap getBeginState() {
        return beginState;
    }

    public final void scheduledTrigger() {
        AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverScheduledEval();
        agentInstanceContext.getAuditProvider().scheduleFire(agentInstanceContext, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);

        // compute reschedule time
        isTimerActive = false;
        SchedulingService schedulingService = agentInstanceContext.getSchedulingService();
        long nextScheduledTime = computeNextSetLastScheduled(schedulingService.getTime(), agentInstanceContext.getClasspathImportServiceRuntime().getTimeAbacus());

        boolean quit = !isFilterChildNonQuitting || nextScheduledTime == -1;
        observerEventEvaluator.observerEvaluateTrue(beginState, quit);

        // handle no more invocations planned
        if (nextScheduledTime == -1) {
            stopObserve();
            observerEventEvaluator.observerEvaluateFalse(false);
            agentInstanceContext.getInstrumentationProvider().aPatternObserverScheduledEval();
            return;
        }

        agentInstanceContext.getAuditProvider().scheduleAdd(nextScheduledTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
        schedulingService.add(nextScheduledTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;

        agentInstanceContext.getInstrumentationProvider().aPatternObserverScheduledEval();
    }

    public void startObserve() {
        if (isTimerActive) {
            throw new IllegalStateException("Timer already active");
        }

        AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
        SchedulingService schedulingService = agentInstanceContext.getSchedulingService();
        TimeAbacus timeAbacus = agentInstanceContext.getClasspathImportServiceRuntime().getTimeAbacus();

        if (anchorTime == null) {
            if (spec.getOptionalDate() == null) {
                anchorTime = Calendar.getInstance(observerEventEvaluator.getContext().getStatementContext().getClasspathImportServiceRuntime().getTimeZone());
                anchorRemainder = timeAbacus.calendarSet(schedulingService.getTime(), anchorTime);
            } else {
                anchorTime = spec.getOptionalDate();
                anchorRemainder = spec.getOptionalRemainder() == null ? 0 : spec.getOptionalRemainder();
            }
        }

        long nextScheduledTime = computeNextSetLastScheduled(schedulingService.getTime(), timeAbacus);
        if (nextScheduledTime == -1) {
            stopObserve();
            observerEventEvaluator.observerEvaluateFalse(false);
            return;
        }

        scheduleHandle = new EPStatementHandleCallbackSchedule(observerEventEvaluator.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        agentInstanceContext.getAuditProvider().scheduleAdd(nextScheduledTime, agentInstanceContext, scheduleHandle, ScheduleObjectType.context, NAME_AUDITPROVIDER_SCHEDULE);
        schedulingService.add(nextScheduledTime, scheduleHandle, scheduleSlot);
        isTimerActive = true;
    }

    public void stopObserve() {
        if (isTimerActive) {
            AgentInstanceContext agentInstanceContext = observerEventEvaluator.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().scheduleRemove(agentInstanceContext, scheduleHandle, ScheduleObjectType.pattern, NAME_AUDITPROVIDER_SCHEDULE);
            agentInstanceContext.getSchedulingService().remove(scheduleHandle, scheduleSlot);
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

    private long computeNextSetLastScheduled(long currentTime, TimeAbacus timeAbacus) {

        // handle already-stopped
        if (cachedCountRepeated == Long.MAX_VALUE) {
            return -1;
        }

        // handle date-only-form: "<date>"
        if (spec.getOptionalRepeatCount() == null && spec.getOptionalDate() != null && spec.getOptionalTimePeriod() == null) {
            cachedCountRepeated = Long.MAX_VALUE;
            long computed = timeAbacus.calendarGet(anchorTime, anchorRemainder);
            if (computed > currentTime) {
                return computed - currentTime;
            }
            return -1;
        }

        // handle period-only-form: "P<period>"
        // handle partial-form-2: "<date>/<period>" (non-recurring)
        if (spec.getOptionalRepeatCount() == null && spec.getOptionalTimePeriod() != null) {
            cachedCountRepeated = Long.MAX_VALUE;
            cachedLastScheduled = (Calendar) anchorTime.clone();
            CalendarPlusMinusForgeOp.actionCalendarPlusMinusTimePeriod(cachedLastScheduled, 1, spec.getOptionalTimePeriod());
            long computed = timeAbacus.calendarGet(cachedLastScheduled, anchorRemainder);
            if (computed > currentTime) {
                return computed - currentTime;
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

        CalendarOpPlusFastAddResult nextDue = CalendarOpPlusFastAddHelper.computeNextDue(currentTime, spec.getOptionalTimePeriod(), cachedLastScheduled, timeAbacus, anchorRemainder);

        if (spec.getOptionalRepeatCount() == -1) {
            cachedLastScheduled = nextDue.getScheduled();
            long computed = timeAbacus.calendarGet(cachedLastScheduled, anchorRemainder);
            return computed - currentTime;
        }

        cachedCountRepeated += nextDue.getFactor();
        if (cachedCountRepeated <= spec.getOptionalRepeatCount()) {
            cachedLastScheduled = nextDue.getScheduled();
            long computed = timeAbacus.calendarGet(cachedLastScheduled, anchorRemainder);
            if (computed > currentTime) {
                return computed - currentTime;
            }
        }
        cachedCountRepeated = Long.MAX_VALUE;
        return -1;
    }
}
