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

package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.epl.methodbase.DotMethodFP;

public enum DatetimeMethodEnum {

    // calendar ops
    WITHTIME("withTime", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.WITHTIME),
    WITHDATE("withDate", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.WITHDATE),
    PLUS("plus", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.PLUSMINUS),
    MINUS("minus", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.PLUSMINUS),
    WITHMAX("withMax", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD),
    WITHMIN("withMin", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD),
    SET("set", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD_PLUS_INT),
    ROUNDCEILING("roundCeiling", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD),
    ROUNDFLOOR("roundFloor", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD),
    ROUNDHALF("roundHalf", DatetimeMethodEnumStatics.calendarOpFactory, DatetimeMethodEnumParams.CALFIELD),

    // reformat ops
    GET("get", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.CALFIELD),
    FORMAT("format", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    TOCALENDAR("toCalendar", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    TODATE("toDate", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    TOMILLISEC("toMillisec", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETMINUTEOFHOUR("getMinuteOfHour", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETMONTHOFYEAR("getMonthOfYear", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFMONTH("getDayOfMonth", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFWEEK("getDayOfWeek", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFYEAR("getDayOfYear", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETERA("getEra", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETHOUROFDAY("getHourOfDay", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETMILLISOFSECOND("getMillisOfSecond", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETSECONDOFMINUTE("getSecondOfMinute", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETWEEKYEAR("getWeekyear", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    GETYEAR("getYear", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.NOPARAM),
    BETWEEN("between", DatetimeMethodEnumStatics.reformatOpFactory, DatetimeMethodEnumParams.BETWEEN),

    // interval ops
    BEFORE("before", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_BEFORE_AFTER),
    AFTER("after", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_BEFORE_AFTER),
    COINCIDES("coincides", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_COINCIDES),
    DURING("during", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_DURING_INCLUDES),
    INCLUDES("includes", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_DURING_INCLUDES),
    FINISHES("finishes", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_FINISHES_FINISHEDBY),
    FINISHEDBY("finishedBy", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_FINISHES_FINISHEDBY),
    MEETS("meets", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_MEETS_METBY),
    METBY("metBy", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_MEETS_METBY),
    OVERLAPS("overlaps", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_DURING_OVERLAPS_OVERLAPBY),
    OVERLAPPEDBY("overlappedBy", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_DURING_OVERLAPS_OVERLAPBY),
    STARTS("starts", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_STARTS_STARTEDBY),
    STARTEDBY("startedBy", DatetimeMethodEnumStatics.intervalOpFactory, DatetimeMethodEnumParams.INTERVAL_STARTS_STARTEDBY),
    ;

    private final String nameCamel;
    private final OpFactory opFactory;
    private DotMethodFP[] footprints;

    private DatetimeMethodEnum(String nameCamel, OpFactory opFactory, DotMethodFP[] footprints) {
        this.nameCamel = nameCamel;
        this.opFactory = opFactory;
        this.footprints = footprints;
    }

    public OpFactory getOpFactory() {
        return opFactory;
    }

    public String getNameCamel() {
        return nameCamel;
    }

    public static boolean isDateTimeMethod(String name) {
        for (DatetimeMethodEnum e : DatetimeMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase().equals(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static DatetimeMethodEnum fromName(String name) {
        for (DatetimeMethodEnum e : DatetimeMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase().equals(name.toLowerCase())) {
                return e;
            }
        }
        return null;
    }

    public DotMethodFP[] getFootprints() {
        return footprints;
    }
}
