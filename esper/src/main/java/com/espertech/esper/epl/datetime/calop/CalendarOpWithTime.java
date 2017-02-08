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
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;

public class CalendarOpWithTime implements CalendarOp {

    private ExprEvaluator hour;
    private ExprEvaluator min;
    private ExprEvaluator sec;
    private ExprEvaluator msec;

    public CalendarOpWithTime(ExprEvaluator hour, ExprEvaluator min, ExprEvaluator sec, ExprEvaluator msec) {
        this.hour = hour;
        this.min = min;
        this.sec = sec;
        this.msec = msec;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarOpWithDate.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarOpWithDate.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarOpWithDate.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarOpWithDate.getInt(msec, eventsPerStream, isNewData, context);
        action(cal, hourNum, minNum, secNum, msecNum);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarOpWithDate.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarOpWithDate.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarOpWithDate.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarOpWithDate.getInt(msec, eventsPerStream, isNewData, context);
        return action(ldt, hourNum, minNum, secNum, msecNum);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer hourNum = CalendarOpWithDate.getInt(hour, eventsPerStream, isNewData, context);
        Integer minNum = CalendarOpWithDate.getInt(min, eventsPerStream, isNewData, context);
        Integer secNum = CalendarOpWithDate.getInt(sec, eventsPerStream, isNewData, context);
        Integer msecNum = CalendarOpWithDate.getInt(msec, eventsPerStream, isNewData, context);
        return action(zdt, hourNum, minNum, secNum, msecNum);
    }

    private static void action(Calendar cal, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            cal.set(Calendar.MINUTE, minute);
        }
        if (second != null) {
            cal.set(Calendar.SECOND, second);
        }
        if (msec != null) {
            cal.set(Calendar.MILLISECOND, msec);
        }
    }

    private static LocalDateTime action(LocalDateTime ldt, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            ldt = ldt.with(ChronoField.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            ldt = ldt.with(ChronoField.MINUTE_OF_HOUR, minute);
        }
        if (second != null) {
            ldt = ldt.with(ChronoField.SECOND_OF_MINUTE, second);
        }
        if (msec != null) {
            ldt = ldt.with(ChronoField.MILLI_OF_SECOND, msec);
        }
        return ldt;
    }

    private static ZonedDateTime action(ZonedDateTime zdt, Integer hour, Integer minute, Integer second, Integer msec) {
        if (hour != null) {
            zdt = zdt.with(ChronoField.HOUR_OF_DAY, hour);
        }
        if (minute != null) {
            zdt = zdt.with(ChronoField.MINUTE_OF_HOUR, minute);
        }
        if (second != null) {
            zdt = zdt.with(ChronoField.SECOND_OF_MINUTE, second);
        }
        if (msec != null) {
            zdt = zdt.with(ChronoField.MILLI_OF_SECOND, msec);
        }
        return zdt;
    }
}
