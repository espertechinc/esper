/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.epl.datetime.calop.CalendarFieldEnum;
import com.espertech.esper.epl.datetime.calop.CalendarOpUtil;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.OpFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.expression.core.ExprValidationException;

import java.util.List;
import java.util.TimeZone;

public class ReformatOpFactory implements OpFactory {

    private final static ReformatOp FormatString = new ReformatOpStringFormat();

    public ReformatOp getOp(TimeZone timeZone, DatetimeMethodEnum method, String methodNameUsed, List<ExprNode> parameters) throws ExprValidationException {
        if (method == DatetimeMethodEnum.GET) {
            CalendarFieldEnum fieldNum = CalendarOpUtil.getEnum(methodNameUsed, parameters.get(0));
            return new ReformatOpGetField(fieldNum, timeZone);
        }
        if (method == DatetimeMethodEnum.FORMAT) {
            return FormatString;
        }
        if (method == DatetimeMethodEnum.TOCALENDAR) {
            return new ReformatOpToCalendar(timeZone);
        }
        if (method == DatetimeMethodEnum.TOMILLISEC) {
            return new ReformatOpToMillisec(timeZone);
        }
        if (method == DatetimeMethodEnum.TODATE) {
            return new ReformatOpToDate(timeZone);
        }
        if (method == DatetimeMethodEnum.GETDAYOFMONTH) {
            return new ReformatOpEval(CalendarEvalStatics.DayOfMonth, LocalDateTimeEvalStatics.DayOfMonth, ZonedDateTimeEvalStatics.DayOfMonth, timeZone);
        }
        if (method == DatetimeMethodEnum.GETMINUTEOFHOUR) {
            return new ReformatOpEval(CalendarEvalStatics.MinuteOfHour, LocalDateTimeEvalStatics.MinuteOfHour, ZonedDateTimeEvalStatics.MinuteOfHour, timeZone);
        }
        if (method == DatetimeMethodEnum.GETMONTHOFYEAR) {
            return new ReformatOpEval(CalendarEvalStatics.MonthOfYear, LocalDateTimeEvalStatics.MonthOfYear, ZonedDateTimeEvalStatics.MonthOfYear, timeZone);
        }
        if (method == DatetimeMethodEnum.GETDAYOFWEEK) {
            return new ReformatOpEval(CalendarEvalStatics.DayOfWeek, LocalDateTimeEvalStatics.DayOfWeek, ZonedDateTimeEvalStatics.DayOfWeek, timeZone);
        }
        if (method == DatetimeMethodEnum.GETDAYOFYEAR) {
            return new ReformatOpEval(CalendarEvalStatics.DayOfYear, LocalDateTimeEvalStatics.DayOfYear, ZonedDateTimeEvalStatics.DayOfYear, timeZone);
        }
        if (method == DatetimeMethodEnum.GETERA) {
            return new ReformatOpEval(CalendarEvalStatics.Era, LocalDateTimeEvalStatics.Era, ZonedDateTimeEvalStatics.Era, timeZone);
        }
        if (method == DatetimeMethodEnum.GETHOUROFDAY) {
            return new ReformatOpEval(CalendarEvalStatics.HourOfDay, LocalDateTimeEvalStatics.HourOfDay, ZonedDateTimeEvalStatics.HourOfDay, timeZone);
        }
        if (method == DatetimeMethodEnum.GETMILLISOFSECOND) {
            return new ReformatOpEval(CalendarEvalStatics.MillisOfSecond, LocalDateTimeEvalStatics.MillisOfSecond, ZonedDateTimeEvalStatics.MillisOfSecond, timeZone);
        }
        if (method == DatetimeMethodEnum.GETSECONDOFMINUTE) {
            return new ReformatOpEval(CalendarEvalStatics.SecondOfMinute, LocalDateTimeEvalStatics.SecondOfMinute, ZonedDateTimeEvalStatics.SecondOfMinute, timeZone);
        }
        if (method == DatetimeMethodEnum.GETWEEKYEAR) {
            return new ReformatOpEval(CalendarEvalStatics.Weekyear, LocalDateTimeEvalStatics.Weekyear, ZonedDateTimeEvalStatics.Weekyear, timeZone);
        }
        if (method == DatetimeMethodEnum.GETYEAR) {
            return new ReformatOpEval(CalendarEvalStatics.Year, LocalDateTimeEvalStatics.Year, ZonedDateTimeEvalStatics.Year, timeZone);
        }
        if (method == DatetimeMethodEnum.BETWEEN) {
            if (ExprNodeUtility.isAllConstants(parameters)) {
                return new ReformatOpBetweenConstantParams(parameters, timeZone);
            }
            return new ReformatOpBetweenNonConstantParams(parameters, timeZone);
        }
        throw new IllegalStateException("Unrecognized date-time method code '" + method + "'");
    }
}
