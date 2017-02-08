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
import java.util.Calendar;

public class CalendarOpWithDate implements CalendarOp {

    private ExprEvaluator year;
    private ExprEvaluator month;
    private ExprEvaluator day;

    public CalendarOpWithDate(ExprEvaluator year, ExprEvaluator month, ExprEvaluator day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public void evaluate(Calendar cal, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        action(cal, yearNum, monthNum, dayNum);
    }

    public LocalDateTime evaluate(LocalDateTime ldt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return action(ldt, yearNum, monthNum, dayNum);
    }

    public ZonedDateTime evaluate(ZonedDateTime zdt, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Integer yearNum = getInt(year, eventsPerStream, isNewData, context);
        Integer monthNum = getInt(month, eventsPerStream, isNewData, context);
        Integer dayNum = getInt(day, eventsPerStream, isNewData, context);
        return action(zdt, yearNum, monthNum, dayNum);
    }

    protected static Integer getInt(ExprEvaluator expr, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object result = expr.evaluate(eventsPerStream, isNewData, context);
        if (result == null) {
            return null;
        }
        return (Integer) result;
    }

    private static void action(Calendar cal, Integer year, Integer month, Integer day) {
        if (year != null) {
            cal.set(Calendar.YEAR, year);
        }
        if (month != null) {
            cal.set(Calendar.MONTH, month);
        }
        if (day != null) {
            cal.set(Calendar.DATE, day);
        }
    }

    private static LocalDateTime action(LocalDateTime ldt, Integer year, Integer month, Integer day) {
        if (year != null) {
            ldt = ldt.withYear(year);
        }
        if (month != null) {
            ldt = ldt.withMonth(month);
        }
        if (day != null) {
            ldt = ldt.withDayOfMonth(day);
        }
        return ldt;
    }

    private static ZonedDateTime action(ZonedDateTime zdt, Integer year, Integer month, Integer day) {
        if (year != null) {
            zdt = zdt.withYear(year);
        }
        if (month != null) {
            zdt = zdt.withMonth(month);
        }
        if (day != null) {
            zdt = zdt.withDayOfMonth(day);
        }
        return zdt;
    }
}
