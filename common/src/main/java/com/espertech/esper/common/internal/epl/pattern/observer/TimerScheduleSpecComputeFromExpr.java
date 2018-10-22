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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.TimePeriod;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodEval;
import com.espertech.esper.common.internal.epl.pattern.core.MatchedEventConvertor;
import com.espertech.esper.common.internal.epl.pattern.core.PatternExpressionUtil;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static com.espertech.esper.common.internal.epl.pattern.observer.TimerScheduleObserverForge.NAME_OBSERVER;

public class TimerScheduleSpecComputeFromExpr implements TimerScheduleSpecCompute {
    private ExprEvaluator date;
    private ExprEvaluator repetitions;
    private TimePeriodEval timePeriod;

    public void setDate(ExprEvaluator date) {
        this.date = date;
    }

    public void setRepetitions(ExprEvaluator repetitions) {
        this.repetitions = repetitions;
    }

    public void setTimePeriod(TimePeriodEval timePeriod) {
        this.timePeriod = timePeriod;
    }

    public TimerScheduleSpec compute(MatchedEventConvertor optionalConvertor, MatchedEventMap beginState, ExprEvaluatorContext exprEvaluatorContext, TimeZone timeZone, TimeAbacus timeAbacus) throws ScheduleParameterException {
        EventBean[] eventsPerStream = optionalConvertor == null ? null : optionalConvertor.convert(beginState);
        return compute(date, repetitions, timePeriod, eventsPerStream, exprEvaluatorContext, timeZone, timeAbacus);
    }

    protected static TimerScheduleSpec compute(ExprEvaluator date, ExprEvaluator repetitions, TimePeriodEval timePeriod,
                                               EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext,
                                               TimeZone timeZone, TimeAbacus timeAbacus) throws ScheduleParameterException {

        Calendar optionalDate = null;
        Long optionalRemainder = null;
        if (date != null) {
            Object param = PatternExpressionUtil.evaluateChecked(NAME_OBSERVER, date, eventsPerStream, exprEvaluatorContext);
            if (param instanceof String) {
                optionalDate = TimerScheduleISO8601Parser.parseDate((String) param);
            } else if (param instanceof Number) {
                long msec = ((Number) param).longValue();
                optionalDate = Calendar.getInstance(timeZone);
                optionalRemainder = timeAbacus.calendarSet(msec, optionalDate);
            } else if (param instanceof Calendar) {
                optionalDate = (Calendar) param;
            } else if (param instanceof Date) {
                optionalDate = Calendar.getInstance(timeZone);
                optionalDate.setTimeInMillis(((Date) param).getTime());
            } else if (param instanceof LocalDateTime) {
                LocalDateTime ldt = (LocalDateTime) param;
                Date d = Date.from(ldt.atZone(timeZone.toZoneId()).toInstant());
                optionalDate = Calendar.getInstance(timeZone);
                optionalDate.setTimeInMillis(d.getTime());
            } else if (param instanceof ZonedDateTime) {
                ZonedDateTime zdt = (ZonedDateTime) param;
                optionalDate = GregorianCalendar.from(zdt);
            } else if (param == null) {
                throw new EPException("Null date-time value returned from date evaluation");
            } else {
                throw new EPException("Unrecognized date-time value " + param.getClass());
            }
        }

        TimePeriod optionalTimePeriod = null;
        if (timePeriod != null) {
            try {
                optionalTimePeriod = timePeriod.timePeriodEval(eventsPerStream, true, exprEvaluatorContext);
            } catch (RuntimeException ex) {
                PatternExpressionUtil.handleRuntimeEx(ex, NAME_OBSERVER);
            }
        }

        Long optionalRepeatCount = null;
        if (repetitions != null) {
            Object param = PatternExpressionUtil.evaluateChecked(NAME_OBSERVER, repetitions, eventsPerStream, exprEvaluatorContext);
            if (param != null) {
                optionalRepeatCount = ((Number) param).longValue();
            }
        }

        if (optionalDate == null && optionalTimePeriod == null) {
            throw new EPException("Required date or time period are both null for " + NAME_OBSERVER);
        }

        return new TimerScheduleSpec(optionalDate, optionalRemainder, optionalRepeatCount, optionalTimePeriod);
    }
}
