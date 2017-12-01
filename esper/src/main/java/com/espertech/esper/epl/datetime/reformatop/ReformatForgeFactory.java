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
package com.espertech.esper.epl.datetime.reformatop;

import com.espertech.esper.epl.datetime.calop.CalendarFieldEnum;
import com.espertech.esper.epl.datetime.calop.CalendarOpUtil;
import com.espertech.esper.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.epl.datetime.eval.ForgeFactory;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.epl.rettype.EPType;

import java.util.List;
import java.util.TimeZone;

public class ReformatForgeFactory implements ForgeFactory {

    private final static ReformatForge FORMAT_STRING = new ReformatStringFormatForge();

    public ReformatForge getForge(EPType inputType, TimeZone timeZone, TimeAbacus timeAbacus, DatetimeMethodEnum method, String methodNameUsed, List<ExprNode> parameters, ExprEvaluatorContext exprEvaluatorContext) throws ExprValidationException {
        if (method == DatetimeMethodEnum.GET) {
            CalendarFieldEnum fieldNum = CalendarOpUtil.getEnum(methodNameUsed, parameters.get(0));
            return new ReformatGetFieldForge(fieldNum, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.FORMAT) {
            if (parameters.isEmpty()) {
                return FORMAT_STRING;
            }
            Object formatter = CalendarOpUtil.getFormatter(inputType, methodNameUsed, parameters.get(0), exprEvaluatorContext);
            return new ReformatFormatForge(formatter, timeAbacus);
        }
        if (method == DatetimeMethodEnum.TOCALENDAR) {
            return new ReformatToCalendarForge(timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.TOMILLISEC) {
            return new ReformatToMillisecForge(timeZone);
        }
        if (method == DatetimeMethodEnum.TODATE) {
            return new ReformatFormatToDateForge(timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFMONTH) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_MONTH, LocalDateTimeEvalStatics.DAY_OF_MONTH, ZonedDateTimeEvalStatics.DAY_OF_MONTH, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMINUTEOFHOUR) {
            return new ReformatEvalForge(CalendarEvalStatics.MINUTE_OF_HOUR, LocalDateTimeEvalStatics.MINUTE_OF_HOUR, ZonedDateTimeEvalStatics.MINUTE_OF_HOUR, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMONTHOFYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.MONTH_OF_YEAR, LocalDateTimeEvalStatics.MONTH_OF_YEAR, ZonedDateTimeEvalStatics.MONTH_OF_YEAR, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFWEEK) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_WEEK, LocalDateTimeEvalStatics.DAY_OF_WEEK, ZonedDateTimeEvalStatics.DAY_OF_WEEK, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_YEAR, LocalDateTimeEvalStatics.DAY_OF_YEAR, ZonedDateTimeEvalStatics.DAY_OF_YEAR, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETERA) {
            return new ReformatEvalForge(CalendarEvalStatics.ERA, LocalDateTimeEvalStatics.ERA, ZonedDateTimeEvalStatics.ERA, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETHOUROFDAY) {
            return new ReformatEvalForge(CalendarEvalStatics.HOUR_OF_DAY, LocalDateTimeEvalStatics.HOUR_OF_DAY, ZonedDateTimeEvalStatics.HOUR_OF_DAY, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMILLISOFSECOND) {
            return new ReformatEvalForge(CalendarEvalStatics.MILLIS_OF_SECOND, LocalDateTimeEvalStatics.MILLIS_OF_SECOND, ZonedDateTimeEvalStatics.MILLIS_OF_SECOND, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETSECONDOFMINUTE) {
            return new ReformatEvalForge(CalendarEvalStatics.SECOND_OF_MINUTE, LocalDateTimeEvalStatics.SECOND_OF_MINUTE, ZonedDateTimeEvalStatics.SECOND_OF_MINUTE, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETWEEKYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.WEEKYEAR, LocalDateTimeEvalStatics.WEEKYEAR, ZonedDateTimeEvalStatics.WEEKYEAR, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.YEAR, LocalDateTimeEvalStatics.YEAR, ZonedDateTimeEvalStatics.YEAR, timeZone, timeAbacus);
        }
        if (method == DatetimeMethodEnum.BETWEEN) {
            if (ExprNodeUtilityRich.isAllConstants(parameters)) {
                return new ReformatBetweenConstantParamsForge(parameters, timeZone);
            }
            return new ReformatBetweenNonConstantParamsForge(parameters, timeZone);
        }
        throw new IllegalStateException("Unrecognized date-time method code '" + method + "'");
    }
}
