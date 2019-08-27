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
package com.espertech.esper.common.internal.epl.datetime.reformatop;

import com.espertech.esper.common.internal.epl.datetime.calop.CalendarFieldEnum;
import com.espertech.esper.common.internal.epl.datetime.calop.CalendarOpUtil;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodDesc;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodEnum;
import com.espertech.esper.common.internal.epl.datetime.eval.DatetimeMethodProviderForgeFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.rettype.EPType;

import java.util.List;

public class ReformatForgeFactory implements DatetimeMethodProviderForgeFactory {

    private final static ReformatForge FORMAT_STRING = new ReformatStringFormatForge();

    public ReformatForge getForge(EPType inputType, TimeAbacus timeAbacus, DatetimeMethodDesc desc, String methodNameUsed, List<ExprNode> parameters) throws ExprValidationException {
        DatetimeMethodEnum method = desc.getDatetimeMethod();
        if (method == DatetimeMethodEnum.GET) {
            CalendarFieldEnum fieldNum = CalendarOpUtil.getEnum(methodNameUsed, parameters.get(0));
            return new ReformatGetFieldForge(fieldNum, timeAbacus);
        }
        if (method == DatetimeMethodEnum.FORMAT) {
            if (parameters.isEmpty()) {
                return FORMAT_STRING;
            }
            ReformatFormatForgeDesc formatterType = CalendarOpUtil.validateGetFormatterType(inputType, methodNameUsed, parameters.get(0));
            return new ReformatFormatForge(formatterType, parameters.get(0).getForge(), timeAbacus);
        }
        if (method == DatetimeMethodEnum.TOCALENDAR) {
            return new ReformatToCalendarForge(timeAbacus);
        }
        if (method == DatetimeMethodEnum.TOMILLISEC) {
            return new ReformatToMillisecForge();
        }
        if (method == DatetimeMethodEnum.TODATE) {
            return new ReformatFormatToDateForge(timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFMONTH) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_MONTH, LocalDateTimeEvalStatics.DAY_OF_MONTH, ZonedDateTimeEvalStatics.DAY_OF_MONTH, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMINUTEOFHOUR) {
            return new ReformatEvalForge(CalendarEvalStatics.MINUTE_OF_HOUR, LocalDateTimeEvalStatics.MINUTE_OF_HOUR, ZonedDateTimeEvalStatics.MINUTE_OF_HOUR, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMONTHOFYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.MONTH_OF_YEAR, LocalDateTimeEvalStatics.MONTH_OF_YEAR, ZonedDateTimeEvalStatics.MONTH_OF_YEAR, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFWEEK) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_WEEK, LocalDateTimeEvalStatics.DAY_OF_WEEK, ZonedDateTimeEvalStatics.DAY_OF_WEEK, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETDAYOFYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.DAY_OF_YEAR, LocalDateTimeEvalStatics.DAY_OF_YEAR, ZonedDateTimeEvalStatics.DAY_OF_YEAR, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETERA) {
            return new ReformatEvalForge(CalendarEvalStatics.ERA, LocalDateTimeEvalStatics.ERA, ZonedDateTimeEvalStatics.ERA, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETHOUROFDAY) {
            return new ReformatEvalForge(CalendarEvalStatics.HOUR_OF_DAY, LocalDateTimeEvalStatics.HOUR_OF_DAY, ZonedDateTimeEvalStatics.HOUR_OF_DAY, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETMILLISOFSECOND) {
            return new ReformatEvalForge(CalendarEvalStatics.MILLIS_OF_SECOND, LocalDateTimeEvalStatics.MILLIS_OF_SECOND, ZonedDateTimeEvalStatics.MILLIS_OF_SECOND, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETSECONDOFMINUTE) {
            return new ReformatEvalForge(CalendarEvalStatics.SECOND_OF_MINUTE, LocalDateTimeEvalStatics.SECOND_OF_MINUTE, ZonedDateTimeEvalStatics.SECOND_OF_MINUTE, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETWEEKYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.WEEKYEAR, LocalDateTimeEvalStatics.WEEKYEAR, ZonedDateTimeEvalStatics.WEEKYEAR, timeAbacus);
        }
        if (method == DatetimeMethodEnum.GETYEAR) {
            return new ReformatEvalForge(CalendarEvalStatics.YEAR, LocalDateTimeEvalStatics.YEAR, ZonedDateTimeEvalStatics.YEAR, timeAbacus);
        }
        if (method == DatetimeMethodEnum.BETWEEN) {
            if (ExprNodeUtilityQuery.isAllConstants(parameters)) {
                return new ReformatBetweenConstantParamsForge(parameters);
            }
            return new ReformatBetweenNonConstantParamsForge(parameters);
        }
        throw new IllegalStateException("Unrecognized date-time method code '" + method + "'");
    }
}
