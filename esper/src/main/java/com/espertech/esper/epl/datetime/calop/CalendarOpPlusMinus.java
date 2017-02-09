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
package com.espertech.esper.epl.datetime.calop;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

public class CalendarOpPlusMinus implements CalendarOp {

    private final ExprEvaluator param;
    private final int factor;

    public CalendarOpPlusMinus(ExprEvaluator param, int factor) {
        this.param = param;
        this.factor = factor;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            action(cal, factor, ((Number) value).longValue());
        } else {
            action(cal, factor, (TimePeriod) value);
        }
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            return action(ldt, factor, ((Number) value).longValue());
        } else {
            return action(ldt, factor, (TimePeriod) value);
        }
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object value = param.evaluate(eventsPerStream, isNewData, context);
        if (value instanceof Number) {
            return action(zdt, factor, ((Number) value).longValue());
        } else {
            return action(zdt, factor, (TimePeriod) value);
        }
    }

    protected static void action(Calendar cal, int factor, Long duration) {
        if (duration == null) {
            return;
        }
        if (duration < Integer.MAX_VALUE) {
            cal.add(Calendar.MILLISECOND, (int) (factor * duration));
            return;
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        cal.add(Calendar.MILLISECOND, factor * msec);
        cal.add(Calendar.DATE, factor * days);
    }

    protected static LocalDateTime action(LocalDateTime ldt, int factor, Long duration) {
        if (duration == null) {
            return ldt;
        }
        if (duration < Integer.MAX_VALUE) {
            return ldt.plus(factor * duration, ChronoUnit.MILLIS);
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        ldt = ldt.plus(factor * msec, ChronoUnit.MILLIS);
        return ldt.plus(factor * days, ChronoUnit.DAYS);
    }

    protected static ZonedDateTime action(ZonedDateTime ldt, int factor, Long duration) {
        if (duration == null) {
            return ldt;
        }
        if (duration < Integer.MAX_VALUE) {
            return ldt.plus(factor * duration, ChronoUnit.MILLIS);
        }

        int days = (int) (duration / (1000L * 60 * 60 * 24));
        int msec = (int) (duration - days * (1000L * 60 * 60 * 24));
        ldt = ldt.plus(factor * msec, ChronoUnit.MILLIS);
        return ldt.plus(factor * days, ChronoUnit.DAYS);
    }

    public static void actionSafeOverflow(Calendar cal, int factor, TimePeriod tp) {
        if (Math.abs(factor) == 1) {
            action(cal, factor, tp);
            return;
        }
        Integer max = tp.largestAbsoluteValue();
        if (max == null || max == 0) {
            return;
        }
        actionHandleOverflow(cal, factor, tp, max);
    }

    public static void action(Calendar cal, int factor, TimePeriod tp) {
        if (tp == null) {
            return;
        }
        if (tp.getYears() != null) {
            cal.add(Calendar.YEAR, factor * tp.getYears());
        }
        if (tp.getMonths() != null) {
            cal.add(Calendar.MONTH, factor * tp.getMonths());
        }
        if (tp.getWeeks() != null) {
            cal.add(Calendar.WEEK_OF_YEAR, factor * tp.getWeeks());
        }
        if (tp.getDays() != null) {
            cal.add(Calendar.DATE, factor * tp.getDays());
        }
        if (tp.getHours() != null) {
            cal.add(Calendar.HOUR_OF_DAY, factor * tp.getHours());
        }
        if (tp.getMinutes() != null) {
            cal.add(Calendar.MINUTE, factor * tp.getMinutes());
        }
        if (tp.getSeconds() != null) {
            cal.add(Calendar.SECOND, factor * tp.getSeconds());
        }
        if (tp.getMilliseconds() != null) {
            cal.add(Calendar.MILLISECOND, factor * tp.getMilliseconds());
        }
    }

    private static LocalDateTime action(LocalDateTime ldt, int factor, TimePeriod tp) {
        if (tp == null) {
            return ldt;
        }
        if (tp.getYears() != null) {
            ldt = ldt.plus(factor * tp.getYears(), ChronoUnit.YEARS);
        }
        if (tp.getMonths() != null) {
            ldt = ldt.plus(factor * tp.getMonths(), ChronoUnit.MONTHS);
        }
        if (tp.getWeeks() != null) {
            ldt = ldt.plus(factor * tp.getWeeks(), ChronoUnit.WEEKS);
        }
        if (tp.getDays() != null) {
            ldt = ldt.plus(factor * tp.getDays(), ChronoUnit.DAYS);
        }
        if (tp.getHours() != null) {
            ldt = ldt.plus(factor * tp.getHours(), ChronoUnit.HOURS);
        }
        if (tp.getMinutes() != null) {
            ldt = ldt.plus(factor * tp.getMinutes(), ChronoUnit.MINUTES);
        }
        if (tp.getSeconds() != null) {
            ldt = ldt.plus(factor * tp.getSeconds(), ChronoUnit.SECONDS);
        }
        if (tp.getMilliseconds() != null) {
            ldt = ldt.plus(factor * tp.getMilliseconds(), ChronoUnit.MILLIS);
        }
        return ldt;
    }

    private static ZonedDateTime action(ZonedDateTime zdt, int factor, TimePeriod tp) {
        if (tp == null) {
            return zdt;
        }
        if (tp.getYears() != null) {
            zdt = zdt.plus(factor * tp.getYears(), ChronoUnit.YEARS);
        }
        if (tp.getMonths() != null) {
            zdt = zdt.plus(factor * tp.getMonths(), ChronoUnit.MONTHS);
        }
        if (tp.getWeeks() != null) {
            zdt = zdt.plus(factor * tp.getWeeks(), ChronoUnit.WEEKS);
        }
        if (tp.getDays() != null) {
            zdt = zdt.plus(factor * tp.getDays(), ChronoUnit.DAYS);
        }
        if (tp.getHours() != null) {
            zdt = zdt.plus(factor * tp.getHours(), ChronoUnit.HOURS);
        }
        if (tp.getMinutes() != null) {
            zdt = zdt.plus(factor * tp.getMinutes(), ChronoUnit.MINUTES);
        }
        if (tp.getSeconds() != null) {
            zdt = zdt.plus(factor * tp.getSeconds(), ChronoUnit.SECONDS);
        }
        if (tp.getMilliseconds() != null) {
            zdt = zdt.plus(factor * tp.getMilliseconds(), ChronoUnit.MILLIS);
        }
        return zdt;
    }

    private static void actionHandleOverflow(Calendar cal, int factor, TimePeriod tp, int max) {
        if (max != 0 && factor > Integer.MAX_VALUE / max) {
            // overflow
            int first = factor / 2;
            int second = (factor - first * 2) + first;
            actionHandleOverflow(cal, first, tp, max);
            actionHandleOverflow(cal, second, tp, max);
        } else {
            // no overflow
            action(cal, factor, tp);
        }
    }
}
